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
import com.neatlogic.autoexecrunner.constvalue.SystemUser;
import com.neatlogic.autoexecrunner.dto.informant.InformantVo;
import com.neatlogic.autoexecrunner.startup.IStartUp;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class HeartbeatHandler implements IStartUp {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final Semaphore semaphore = new Semaphore(3);//最多3个线程心跳
    private static final LinkedBlockingQueue<InformantVo> informantQueue = new LinkedBlockingQueue<>();
    private static final Map<String, InformantVo> informantMap = new ConcurrentHashMap<>();

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
        System.out.println("创建INFORMANT-HEARTBEAT-LISTENER");
        Thread listener = new Thread(new NeatLogicThread("INFORMANT-HEARTBEAT-LISTENER") {
            @Override
            protected void execute() {
                try (DatagramSocket socket = new DatagramSocket(Config.SERVER_PORT())) {
                    while (running.get()) {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        InetAddress senderAddress = packet.getAddress();
                        int senderPort = packet.getPort();
                        String senderIp = senderAddress.getHostAddress();
                        String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                        if (StringUtils.isNotBlank(message)) {
                            System.out.println("收到来自" + senderPort + ":" + senderIp + "的udp信息: " + message + " 时间" + System.currentTimeMillis());
                            JSONObject jsonObject = JSON.parseObject(message);
                            InformantVo informantVo = JSON.toJavaObject(jsonObject, InformantVo.class);
                            informantVo.setIp(senderIp);
                            informantVo.setUdpPort(senderPort);
                            if (StringUtils.isNotBlank(informantVo.getUuid()) && StringUtils.isNotBlank(informantVo.getType())) {
                                //遇到相同的agent心跳先设为忽略，避免重复发起
                                if (informantMap.get(informantVo.getUuid()) != null) {
                                    informantMap.get(informantVo.getUuid()).setValid(false);
                                }
                                informantMap.put(informantVo.getUuid(), informantVo);
                                informantQueue.put(informantVo);
                            } else {
                                logger.warn("心跳信息异常：{}", jsonObject);
                            }
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

                        informantMap.remove(informantVo.getUuid());

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
                String url = "";
                if (Objects.equals("register", informantVo.getType())) {
                    url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), "informant/register");
                } else if (Objects.equals("heartbeat", informantVo.getType())) {
                    url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), "informant/heartbeat");
                }


                if (StringUtils.isNotBlank(url)) {
                    HttpRequestUtil util = HttpRequestUtil.post(url).setPayload(JSON.toJSONString(informantVo))
                            .setAuthType(AuthenticateType.HMAC)
                            .setTenant(informantVo.getTenant())
                            .setToken(SystemUser.AUTOEXEC.getToken())
                            .setUsername(SystemUser.AUTOEXEC.getUserId())
                            .sendRequest();
                    //System.out.println("已经发送请求到：" + url + "，参数：" + JSON.toJSONString(informantVo));
                    if (util.getError() != null) {
                        //System.out.println("请求异常：" + util.getError());
                    }
                    if (util.getResponseCode() == 200) {
                        //System.out.println("已经收到返回结果：" + util.getResult());
                        byte[] sendData = util.getResult().getBytes(StandardCharsets.UTF_8);
                        try (DatagramSocket udpSocket = new DatagramSocket()) {
                            DatagramPacket packet = new DatagramPacket(
                                    sendData,
                                    sendData.length,
                                    InetAddress.getByName(informantVo.getIp()),
                                    informantVo.getUdpPort()
                            );
                            udpSocket.send(packet);
                            //System.out.println("已通过UDP通知 agent（" + informantVo.getIp() + ":" + informantVo.getUdpPort() + ")");
                        }
                    }

                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            } finally {
                //一定要释放资源
                semaphore.release();
            }
        }
    }
}
