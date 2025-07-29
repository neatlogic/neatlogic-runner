/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
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

package com.neatlogic.autoexecrunner.informant;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.NeatLogicThread;
import com.neatlogic.autoexecrunner.asynchronization.threadpool.CachedThreadPool;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.dto.informant.InformantVo;
import com.neatlogic.autoexecrunner.startup.IStartUp;
import com.neatlogic.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class HeartbeatHandler implements IStartUp {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final Semaphore semaphore = new Semaphore(3);//最多3个线程心跳
    private static final LinkedBlockingQueue<InformantVo> informantQueue = new LinkedBlockingQueue<>();
    private static final Map<String, InformantVo> informantMap = new HashMap<>();

    @Override
    public String getName() {
        return "启动informant心跳监听服务";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public void doService() {
        Thread listener = new Thread(new NeatLogicThread("INFORMANT-HEARTBEAT-LISTENER") {
            @Override
            protected void execute() {
                try (DatagramSocket socket = new DatagramSocket(Config.SERVER_PORT())) {
                    while (running.get()) {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                        if (StringUtils.isNotBlank(message)) {
                            JSONObject jsonObject = JSON.parseObject(message);
                            InformantVo informantVo = JSON.toJavaObject(jsonObject, InformantVo.class);
                            informantMap.put(informantVo.getUuid(), informantVo);
                            informantQueue.put(informantVo);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        listener.setDaemon(true);
        listener.start();

        Thread handler = new Thread(new NeatLogicThread("INFORMANT-HEARTBEAT-HANDLER") {
            @Override
            protected void execute() {
                InformantVo informantVo;
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        informantVo = informantQueue.take();
                        if (!informantVo.isValid()) {
                            continue;
                        }
                        semaphore.acquire();
                        CachedThreadPool.execute(new InformantHandler(informantVo));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        handler.setDaemon(true);
        handler.start();
    }

    // 处理逻辑线程
    static class InformantHandler extends NeatLogicThread {
        private final InformantVo informantVo;

        public InformantHandler(InformantVo informantVo) {
            super("INFORMANT-HEARTBEAT-HANDLER-" + informantVo.getUuid());
            this.informantVo = informantVo;
        }

        @Override
        public void execute() {
            try {
                System.out.println(informantVo.getUuid() + "-" + informantVo.isNeedKey());
                String url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), "informant/heartbeat");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uuid", informantVo.getUuid());
                jsonObject.put("needKey", informantVo.isNeedKey());
                RestVo restVo = new RestVo(url, jsonObject, AuthenticateType.HMAC.getValue(), informantVo.getTenant());
                //异步请求，不需要等待
                RestUtil.sendRequest(restVo);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            } finally {
                //一定要释放资源
                semaphore.release();
            }
        }
    }
}
