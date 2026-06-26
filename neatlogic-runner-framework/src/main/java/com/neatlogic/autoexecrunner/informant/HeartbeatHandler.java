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

package com.neatlogic.autoexecrunner.informant;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.AsyncTaskManager;
import com.neatlogic.autoexecrunner.asynchronization.NeatLogicThread;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.constvalue.InformantHeartbreakReason;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class HeartbeatHandler implements IStartUp {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);
    private static final int HEARTBEAT_TIMEOUT_SCAN_INTERVAL_SECONDS = 10;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Object heartbeatSocketSendLock = new Object();
    private volatile DatagramSocket heartbeatSocket;
    private static final Map<String, InformantVo> informantMap = new ConcurrentHashMap<>();
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
        AsyncTaskManager<InformantVo> informantHeartbeatHandlerManager = AsyncTaskManager.getInstance("INFORMANT-HEARTBEAT-HANDLER", 10,
                informantVo -> {
                    try {
                        String url = "";
                        if (Objects.equals("register", informantVo.getType())) {
                            url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), "informant/register");
                        } else if (Objects.equals("heartbeat", informantVo.getType())) {
                            url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), "informant/heartbeat");
                        } else if (Objects.equals("heartbreak", informantVo.getType())) {
                            //System.out.println("收到离线请求：" + informantVo.getUuid());
                            if (StringUtils.isBlank(informantVo.getMessage())) {
                                informantVo.setMessage(InformantHeartbreakReason.ACTIVE_OFFLINE.getMessage());
                            }
                            url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), "informant/heartbreak");
                        }

                        if (StringUtils.isNotBlank(url)) {
                            //System.out.println("发送请求到：" + url + "，参数：" + JSON.toJSONString(informantVo));
                            HttpRequestUtil util = HttpRequestUtil.post(url).setPayload(JSON.toJSONString(informantVo))
                                    .setAuthType(AuthenticateType.HMAC)
                                    .setTenant(informantVo.getTenant())
                                    .setToken(SystemUser.AUTOEXEC.getToken())
                                    .setUsername(SystemUser.AUTOEXEC.getUserId())
                                    .sendRequest();
                            if (StringUtils.isNotBlank(util.getError())) {
                                logger.error("Informant request failed, type:{}, uuid:{}, hostname:{}, ip:{}, udpPort:{}, tenant:{}, url:{}, responseCode:{}, error:{}",
                                        informantVo.getType(), informantVo.getUuid(), informantVo.getHostname(), informantVo.getIp(), informantVo.getUdpPort(),
                                        informantVo.getTenant(), url, util.getResponseCode(), util.getError());
                            }
                            if (util.getResponseCode() == 200) {
                                byte[] sendData = util.getResult().getBytes(StandardCharsets.UTF_8);
                                sendUdpResponse(informantVo, sendData);
                                if (Objects.equals("register", informantVo.getType())) {
                                    logger.info("Informant register response sent, uuid:{}, hostname:{}, ip:{}, udpPort:{}, tenant:{}, url:{}, responseCode:{}, responseSize:{}",
                                            informantVo.getUuid(), informantVo.getHostname(), informantVo.getIp(), informantVo.getUdpPort(), informantVo.getTenant(),
                                            url, util.getResponseCode(), sendData.length);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    } finally {
                        informantMap.remove(getInformantTaskKey(informantVo));
                    }
                });

        Thread listener = new Thread(new NeatLogicThread("INFORMANT-HEARTBEAT-LISTENER") {
            @Override
            protected void execute() {
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket(Config.SERVER_PORT());
                    heartbeatSocket = socket;
                    while (running.get()) {
                        byte[] buffer = new byte[65535];
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
                                updateInformantState(informantVo);
                                //遇到相同的agent心跳忽略，避免重复发起
                                String taskKey = getInformantTaskKey(informantVo);
                                if (informantMap.get(taskKey) == null) {
                                    informantMap.put(taskKey, informantVo);
                                    informantHeartbeatHandlerManager.submitTask(informantVo);
                                }
                            } else {
                                logger.warn("心跳信息异常：{}", jsonObject);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    if (heartbeatSocket == socket) {
                        heartbeatSocket = null;
                    }
                    if (socket != null) {
                        socket.close();
                    }
                }
            }
        });
        listener.setDaemon(true);
        listener.start();


        AsyncTaskManager<InformantVo> informantHeartbreakHandlerManager = AsyncTaskManager.getInstance("INFORMANT-HEARTBREAK-HANDLER", 3,
                informantVo -> {
                    try {
                        // runner生成的超时下线事件进入异步队列后，可能已经收到新心跳，此时丢弃这条滞后事件。
                        InformantState currentState = informantStateMap.get(informantVo.getUuid());
                        if (informantVo.getHeartbreakReason() == InformantHeartbreakReason.HEARTBEAT_TIMEOUT
                                && currentState != null
                                && informantVo.getTimestamp() != null
                                && currentState.getLastHeartbeatTime() > informantVo.getTimestamp()) {
                            return;
                        }
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
        Thread heartbreakThread = new Thread(new NeatLogicThread("INFORMANT-HEARTBREAK-SCANNER") {
            @Override
            protected void execute() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        TimeUnit.SECONDS.sleep(HEARTBEAT_TIMEOUT_SCAN_INTERVAL_SECONDS);
                        long now = System.currentTimeMillis();
                        for (InformantState state : informantStateMap.values()) {
                            AtomicReference<InformantState> expiredStateRef = new AtomicReference<>();
                            informantStateMap.computeIfPresent(state.getUuid(), (uuid, currentState) -> {
                                if (currentState.getExpiredTime() <= now) {
                                    expiredStateRef.set(currentState);
                                    return null;
                                }
                                return currentState;
                            });
                            InformantState expiredState = expiredStateRef.get();
                            if (expiredState != null) {
                                //System.out.println("心跳超时：" + expiredState.getUuid() + ",超时时间：" + expiredState.getExpiredTime() + ",当前时间：" + now + ",扫描间隔：" + HEARTBEAT_TIMEOUT_SCAN_INTERVAL_SECONDS + "秒");
                                //发送下线更改请求
                                InformantVo informantVo = new InformantVo();
                                informantVo.setTenant(expiredState.getTenant());
                                informantVo.setUuid(expiredState.getUuid());
                                informantVo.setRunnerId(expiredState.getRunnerId());
                                informantVo.setTimestamp(now);
                                informantVo.setHeartbreakReason(InformantHeartbreakReason.HEARTBEAT_TIMEOUT);
                                informantVo.setMessage(InformantHeartbreakReason.HEARTBEAT_TIMEOUT.getMessage());
                                informantHeartbreakHandlerManager.submitTask(informantVo);
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            }
        });
        heartbreakThread.setDaemon(true);
        heartbreakThread.start();
    }

    private static String getInformantTaskKey(InformantVo informantVo) {
        return informantVo.getType() + ":" + informantVo.getUuid();
    }

    private void sendUdpResponse(InformantVo informantVo, byte[] sendData) throws Exception {
        DatagramSocket socket = heartbeatSocket;
        if (socket == null || socket.isClosed()) {
            logger.warn("Informant response skipped because heartbeat socket is not ready, uuid:{}, ip:{}, udpPort:{}",
                    informantVo.getUuid(), informantVo.getIp(), informantVo.getUdpPort());
            return;
        }
        DatagramPacket packet = new DatagramPacket(
                sendData,
                sendData.length,
                InetAddress.getByName(informantVo.getIp()),
                informantVo.getUdpPort()
        );
        // 复用监听socket回包，确保runner响应源端口与心跳监听端口一致。
        synchronized (heartbeatSocketSendLock) {
            socket.send(packet);
        }
    }

    private static void updateInformantState(InformantVo informantVo) {
        if (Objects.equals("heartbeat", informantVo.getType())) {
            informantStateMap.compute(informantVo.getUuid(), (uuid, informantState) -> {
                if (informantState == null) {
                    return new InformantState(informantVo.getUuid(), informantVo.getTenant(), informantVo.getRunnerId(), Config.INFORMANT_HEARTBEAT_INTERVAL(), TimeUnit.SECONDS);
                }
                if (informantVo.getRunnerId() != null) {
                    informantState.setRunnerId(informantVo.getRunnerId());
                }
                informantState.setExpiredTime(Config.INFORMANT_HEARTBEAT_INTERVAL(), TimeUnit.SECONDS);
                return informantState;
            });
        } else if (Objects.equals("heartbreak", informantVo.getType())) {
            informantStateMap.remove(informantVo.getUuid());
        }
    }
}
