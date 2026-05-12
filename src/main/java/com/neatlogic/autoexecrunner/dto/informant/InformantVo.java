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

package com.neatlogic.autoexecrunner.dto.informant;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class InformantVo {
    private String uuid;
    private String type;
    @JSONField(serialize = false)
    private boolean isValid = true;
    @JSONField(serialize = false)
    private String tenant;
    private String hostname;
    private String sign;
    private Long timestamp;
    private String ip;
    private Long runnerId;
    private int udpPort;
    private int httpPort;
    private int httpsPort;
    private String protocol;
    private String version;
    private String osType;
    private String cpuArch;
    private String healthState;
    private String preHealthState;
    private List<String> ipv4Address;
    private List<String> ipv6Address;
    private Double cpuUsage;
    private Double memSize;
    private Double diskUsed;

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getCpuArch() {
        return cpuArch;
    }

    public void setCpuArch(String cpuArch) {
        this.cpuArch = cpuArch;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public Long getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(Long runnerId) {
        this.runnerId = runnerId;
    }

    public String getHealthState() {
        return healthState;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

    public String getPreHealthState() {
        return preHealthState;
    }

    public void setPreHealthState(String preHealthState) {
        this.preHealthState = preHealthState;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @JSONField(name = "ipv4_address")
    public List<String> getIpv4Address() {
        return ipv4Address;
    }

    @JSONField(name = "ipv4_address")
    public void setIpv4Address(List<String> ipv4Address) {
        this.ipv4Address = ipv4Address;
    }

    @JSONField(name = "ipv6_address")
    public List<String> getIpv6Address() {
        return ipv6Address;
    }

    @JSONField(name = "ipv6_address")
    public void setIpv6Address(List<String> ipv6Address) {
        this.ipv6Address = ipv6Address;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemSize() {
        return memSize;
    }

    public void setMemSize(Double memSize) {
        this.memSize = memSize;
    }

    public Double getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(Double diskUsed) {
        this.diskUsed = diskUsed;
    }

}
