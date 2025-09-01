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
import com.neatlogic.autoexecrunner.asynchronization.AsyncTaskManager;
import com.neatlogic.autoexecrunner.asynchronization.NeatLogicThread;
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
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class HeartbeatHandler implements IStartUp {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final Map<String, InformantVo> informantMap = new ConcurrentHashMap<>();
    private static final DelayQueue<InformantItem> informantStateQueue = new DelayQueue<>();
    private static final Map<String, InformantState> informantStateMap = new ConcurrentHashMap<>();

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
        AsyncTaskManager<InformantVo> informantHeartbeatHandlerManager = AsyncTaskManager.getInstance("INFORMANT-HEARTBEAT-HANDLER", 3,
                informantVo -> {
                    informantMap.remove(informantVo.getUuid());
                    try {
                        String url = "";
                        if (Objects.equals("register", informantVo.getType())) {
                            url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), "informant/register");
                        } else if (Objects.equals("heartbeat", informantVo.getType())) {
                            url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), "informant/heartbeat");
                            //收到一次心跳后保存agent状态，如果5分钟之内没有收到新的心跳，判断为离线
                            InformantState informantState = informantStateMap.get(informantVo.getUuid());
                            if (informantState == null) {
                                informantState = new InformantState(informantVo.getUuid(), informantVo.getTenant(), Config.INFORMANT_HEARTBEAT_INTERVAL(), TimeUnit.SECONDS);
                                informantStateMap.put(informantVo.getUuid(), informantState);
                                informantStateQueue.put(new InformantItem(informantState));
                            } else {
                                //如果状态已存在，直接更新超时时间，处理下线代理是会自动重新放回队列
                                informantState.setExpiredTime(Config.INFORMANT_HEARTBEAT_INTERVAL(), TimeUnit.SECONDS);
                            }
                        }

                        if (StringUtils.isNotBlank(url)) {
                            HttpRequestUtil util = HttpRequestUtil.post(url).setPayload(JSON.toJSONString(informantVo))
                                    .setAuthType(AuthenticateType.HMAC)
                                    .setTenant(informantVo.getTenant())
                                    .setToken(SystemUser.AUTOEXEC.getToken())
                                    .setUsername(SystemUser.AUTOEXEC.getUserId())
                                    .sendRequest();
                            if (StringUtils.isNotBlank(util.getError())) {
                                logger.error(util.getError());
                            }
                            if (util.getResponseCode() == 200) {
                                byte[] sendData = util.getResult().getBytes(StandardCharsets.UTF_8);
                                try (DatagramSocket udpSocket = new DatagramSocket()) {
                                    DatagramPacket packet = new DatagramPacket(
                                            sendData,
                                            sendData.length,
                                            InetAddress.getByName(informantVo.getIp()),
                                            informantVo.getUdpPort()
                                    );
                                    udpSocket.send(packet);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                });

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
                            //System.out.println("收到来自" + senderPort + ":" + senderIp + "的udp信息: " + message + " 时间" + System.currentTimeMillis());
                            JSONObject jsonObject = JSON.parseObject(message);
                            InformantVo informantVo = JSON.toJavaObject(jsonObject, InformantVo.class);
                            informantVo.setIp(senderIp);
                            informantVo.setUdpPort(senderPort);
                            if (StringUtils.isNotBlank(informantVo.getUuid()) && StringUtils.isNotBlank(informantVo.getType())) {
                                //遇到相同的agent心跳忽略，避免重复发起
                                if (informantMap.get(informantVo.getUuid()) == null) {
                                    informantMap.put(informantVo.getUuid(), informantVo);
                                    informantHeartbeatHandlerManager.submitTask(informantVo);
                                }
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


        AsyncTaskManager<InformantVo> informantHeartbreakHandlerManager = AsyncTaskManager.getInstance("INFORMANT-HEARTBREAK-HANDLER", 3,
                informantVo -> {
                    try {
                        //清除状态信息
                        informantStateMap.remove(informantVo.getUuid());
                        String url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), "informant/heartbreak");
                        if (StringUtils.isNotBlank(url)) {
                            HttpRequestUtil util = HttpRequestUtil.post(url).setPayload(JSON.toJSONString(informantVo))
                                    .setAuthType(AuthenticateType.HMAC)
                                    .setTenant(informantVo.getTenant())
                                    .setToken(SystemUser.AUTOEXEC.getToken())
                                    .setUsername(SystemUser.AUTOEXEC.getUserId())
                                    .sendRequest();
                            if (StringUtils.isNotBlank(util.getError())) {
                                logger.error(util.getError());
                            }
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                });

        //处理下线的代理
        Thread heartbreakThread = new Thread(new NeatLogicThread("INFORMANT-HEARTBREAK-HANDLER") {
            @Override
            protected void execute() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        InformantItem task = informantStateQueue.take();
                        InformantState state = task.getInformantState();
                        if (state.getExpiredTime() > System.currentTimeMillis()) {
                            //收到了新心跳，超时时间已经后延，重新放回队列
                            informantStateQueue.put(new InformantItem(state));
                        } else {
                            //发送下线更改请求
                            InformantVo informantVo = new InformantVo();
                            informantVo.setTenant(state.getTenant());
                            informantVo.setUuid(state.getUuid());
                            informantHeartbreakHandlerManager.submitTask(informantVo);
                        }
                    } catch (Exception ex) {

                    }
                }
            }
        });
        heartbreakThread.setDaemon(true);
        heartbreakThread.start();
    }
}
