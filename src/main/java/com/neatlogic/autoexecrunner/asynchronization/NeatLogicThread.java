/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package com.neatlogic.autoexecrunner.asynchronization;


import com.neatlogic.autoexecrunner.asynchronization.threadlocal.RequestContext;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public abstract class NeatLogicThread implements Runnable, Comparable<NeatLogicThread> {
    private static final Logger logger = LoggerFactory.getLogger(NeatLogicThread.class);
    protected UserContext userContext;
    protected RequestContext requestContext;
    private final String tenantUuid;
    private String threadName;
    private boolean isUnique = false;
    /* For generating thread ID */
    private long id;
    private int priority = 3;//默认优先级是3，数字越低优先级越高
    private Semaphore lock;//用于hold住其他异步线程，控制两个异步线程的先后顺序
    private CountDownLatch countDownLatch;//用于hold住主线程，这里只需要countdown，不需要等待
    private boolean needAwaitAdvance = true;// 是否需要等待所有模块加载完成后再任务

    @Override
    public int compareTo(NeatLogicThread other) {
        return Integer.compare(this.priority, other.priority); // 优先级高的先出队,priority越小代表优先级越高
    }

    public Semaphore getLock() {
        return lock;
    }

    public void setLock(Semaphore lock) {
        this.lock = lock;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPriority() {
        if (priority <= 1) {
            priority = 1;
        }
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }



    /*public NeatLogicThread() {
        userContext = UserContext.get();
        tenantContext = TenantContext.get();
        inputFromContext = InputFromContext.get();
    }*/

    /*public NeatLogicThread(UserContext _userContext, TenantContext _tenantContext) {
        if (_userContext != null) {
            userContext = _userContext.copy();
        }
        tenantUuid = _tenantContext.getTenantUuid();
        activeModuleList = _tenantContext.getActiveModuleList();
        inputFromContext = InputFromContext.get();
    }*/


    public NeatLogicThread(String _threadName) {
        UserContext tmp = UserContext.get();
        if (tmp != null) {
            userContext = tmp.copy();
        }
        tenantUuid = TenantContext.get().getTenantUuid();
        requestContext = RequestContext.get();
        this.threadName = _threadName;
    }

    public NeatLogicThread(String _threadName, int priority) {
        UserContext tmp = UserContext.get();
        if (tmp != null) {
            userContext = tmp.copy();
        }
        tenantUuid = TenantContext.get().getTenantUuid();
        requestContext = RequestContext.get();
        this.threadName = _threadName;
        this.priority = priority;
    }

    public NeatLogicThread(String _threadName, boolean _isUnique) {
        userContext = UserContext.get();
        tenantUuid = TenantContext.get().getTenantUuid();
        requestContext = RequestContext.get();
        this.threadName = _threadName;
        this.isUnique = _isUnique;
    }

    @Override
    public final void run() {
        TenantContext tenantContext = TenantContext.init(tenantUuid);
        UserContext.init(userContext);
        try {
            String oldThreadName = Thread.currentThread().getName();
            if (StringUtils.isNotBlank(threadName)) {
                Thread.currentThread().setName(threadName);
            }

            if (this.lock != null) {
                //System.out.println(this.getThreadName() + "尝试获取锁" + this.lock);
                lock.acquire();
                //System.out.println(this.getThreadName() + "成功获取锁" + this.lock);
            }
            execute();
            Thread.currentThread().setName(oldThreadName);
        } catch (ApiRuntimeException ex) {
            logger.warn(ex.getMessage(), ex);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (this.lock != null) {
                lock.release();
                //System.out.println(this.getThreadName() + "成功释放锁" + this.lock);
            }
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
            // 清除所有threadlocal
            if (TenantContext.get() != null) {
                TenantContext.get().release();
            }
            if (UserContext.get() != null) {
                UserContext.get().release();
            }
        }
    }

    protected abstract void execute();

    public void setUnique(boolean unique) {
        isUnique = unique;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getThreadName() {
        return threadName;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    public boolean isNeedAwaitAdvance() {
        return needAwaitAdvance;
    }

    public void setNeedAwaitAdvance(boolean needAwaitAdvance) {
        this.needAwaitAdvance = needAwaitAdvance;
    }
}
