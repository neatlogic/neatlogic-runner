/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.neatlogic.autoexecrunner.asynchronization.queue;

import com.alibaba.fastjson.JSON;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NeatLogicUniqueBlockingQueue<T> {
    private static final Logger logger = LoggerFactory.getLogger(NeatLogicUniqueBlockingQueue.class);
    private final BlockingQueue<Task<T>> blockingQueue;
    private final ConcurrentHashMap<String, Boolean> taskMap; // 用于去重

    public NeatLogicUniqueBlockingQueue(int capacity) {
        this.blockingQueue = new LinkedBlockingQueue<>(capacity);
        this.taskMap = new ConcurrentHashMap<>();
    }

    /**
     * 添加队列成员
     * 返回-1：队列已满，1：添加成功，0:重复添加
     */
    public int offer(T t) {
        Task<T> task = new Task<>(t);
        // 保证任务唯一性
        if (taskMap.putIfAbsent(task.getUniqueKey(), Boolean.TRUE) == null) {
            logger.debug("====addQueue:" + JSON.toJSONString(task));
            // 如果任务是新任务，放入队列
            boolean added = blockingQueue.offer(task);
            if (!added) {
                // 如果队列已满，移除任务标记
                taskMap.remove(task.getUniqueKey());
                logger.error("Queue is full!");
                return -1;
            }
            return 1;
        } else {
            if (t != null) {
                logger.debug("NeatLogicUniqueBlockingQueue repeat： {}", JSON.toJSONString(t));
            }
            return 0;
        }
    }

    public boolean remove(Predicate<Task<T>> condition) {
        for (Task<T> task : blockingQueue) {
            if (condition.test(task)) {
                boolean removed = blockingQueue.remove(task);
                if (removed) {
                    taskMap.remove(task.getUniqueKey());
                    logger.debug("Removed task: {}", JSON.toJSONString(task));
                    return true;
                }
            }
        }
        return false;
    }

    public T take() throws InterruptedException {
        Task<T> task = blockingQueue.take(); // 阻塞式获取任务
        taskMap.remove(task.getUniqueKey()); // 移除已处理任务的唯一标记
        TenantContext tenantContext = TenantContext.get();
        if (tenantContext != null) {
            tenantContext.switchTenant(task.getTenantUuid());
        } else {
            TenantContext.init(task.getTenantUuid());
        }
        return task.getT();
    }

    public static class Task<T> {
        private final T t;
        private final String tenantUuid;

        public Task(T t) {
            this.t = t;
            this.tenantUuid = TenantContext.get().getTenantUuid();
        }

        public T getT() {
            return t;
        }

        public String getTenantUuid() {
            return tenantUuid;
        }

        public String getUniqueKey() {
            // 唯一标识任务的 key，可根据需求定义，例如 `tenantUuid-t.hashCode`
            //System.out.println(tenantUuid + "-" + t.hashCode());
            return tenantUuid + "-" + t.hashCode();
        }
    }

    public int size() {
        return blockingQueue.size();
    }

    public List<T> getQueue() {
        List<T> list = new ArrayList<>();
        List<Task<T>> taskList = new ArrayList<>(blockingQueue);
        if (CollectionUtils.isNotEmpty(taskList)) {
            list = taskList.stream().map(Task::getT).collect(Collectors.toList());
        }
        return list;
    }

//    public static void main(String[] args) throws InterruptedException {
//        NeatLogicUniqueBlockingQueue<CommandVo> queue = new NeatLogicUniqueBlockingQueue<>(1);
//
//        // 模拟任务插入
//        CommandVo a = new CommandVo();
//        a.setCommandList(Arrays.asList("--nodes"));
//        System.out.println(queue.offer(a)); // 返回 true，任务插入成功
//        System.out.println(queue.size());
//        Thread.sleep(2000);
//        CommandVo b = new CommandVo();
//        b.setCommandList(Arrays.asList("--nodes1"));
//        System.out.println(queue.offer(b)); // 返回 true，任务插入成功
//        System.out.println(queue.size());
//
//
//        // 模拟任务消费
//        queue.take(); // 消费 "task1"
//        System.out.println(queue.size());
//        Thread.sleep(2000);
//        System.out.println(queue.size());
//        queue.take(); // 消费 "task1"
//    }
}

