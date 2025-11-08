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

package com.neatlogic.autoexecrunner.store.mongodb;


import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.constvalue.SystemUser;
import com.neatlogic.autoexecrunner.dto.MongoDbVo;
import com.neatlogic.autoexecrunner.exception.ConnectRefusedException;
import com.neatlogic.autoexecrunner.exception.MongoDataSourceNotFoundException;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//@Configuration
public class NeatlogicMongoDbFactory extends SimpleMongoClientDatabaseFactory {
    Logger logger = LoggerFactory.getLogger(NeatlogicMongoDbFactory.class);
    private static final Map<String, MongoClient> mongoDbMap = new HashMap<>();
    private static final Map<String, String> mongoDatabaseMap = new HashMap<>();

    public NeatlogicMongoDbFactory(String connectionString) {
        super(connectionString);
    }

    @Override
    protected MongoDatabase doGetMongoDatabase(String dbName) {
        if (!mongoDbMap.containsKey(TenantContext.get().getTenantUuid())) {
            String CALLBACK_PROCESS_UPDATE_URL = "mongodb/datasource/get";
            String url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), CALLBACK_PROCESS_UPDATE_URL);
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url).setPayload(new JSONObject().toJSONString())
                    .setAuthType(AuthenticateType.HMAC)
                    .setTenant(TenantContext.get().getTenantUuid())
                    .setToken(SystemUser.AUTOEXEC.getToken())
                    .setUsername(SystemUser.AUTOEXEC.getUserId())
                    .sendRequest();
            if (httpRequestUtil.getResponseCode() != 200 || StringUtils.isNotBlank(httpRequestUtil.getError())) {
                throw new ApiRuntimeException(String.format("Request to %s failed, result: %s, ResponseCode: %s, ErrorMsg: %s, Exception %s",
                        url, httpRequestUtil.getResult(), httpRequestUtil.getResponseCode(), httpRequestUtil.getErrorMsg(), httpRequestUtil.getError()));
            }
            JSONObject resultJson = httpRequestUtil.getResultJson();
            try {
                if (MapUtils.isNotEmpty(resultJson) && !Objects.equals(resultJson.getString("Status"), "OK")) {
                    if (resultJson.containsKey("Message")) {
                        throw new ApiRuntimeException(resultJson.getString("Message"));
                    } else {
                        throw new ApiRuntimeException(httpRequestUtil.getResult());
                    }
                }
                JSONObject returnJson = resultJson.getJSONObject("Return");
                if (MapUtils.isEmpty(returnJson)) {
                    throw new MongoDataSourceNotFoundException(TenantContext.get().getTenantUuid());
                }
                MongoDbVo mongoDbVo = returnJson.toJavaObject(MongoDbVo.class);
                MongoClient client = MongoClients.create("mongodb://" + mongoDbVo.getUsername() + ":" + mongoDbVo.getPasswordPlain() + "@" + mongoDbVo.getHost() + "/" + mongoDbVo.getDatabase() + (StringUtils.isNotBlank(mongoDbVo.getOption()) ? "?" + mongoDbVo.getOption() : ""));
                mongoDbMap.put(TenantContext.get().getTenantUuid(), client);
                mongoDatabaseMap.put(TenantContext.get().getTenantUuid(), mongoDbVo.getDatabase());
                return client.getDatabase(mongoDbVo.getDatabase());
            } catch (JSONException ex) {
                logger.error(ex.getMessage(), ex);
                throw new ConnectRefusedException(url + ":" + httpRequestUtil.getResult());
            }
        } else {
            return mongoDbMap.get(TenantContext.get().getTenantUuid()).getDatabase(mongoDatabaseMap.get(TenantContext.get().getTenantUuid()));
        }
    }
}
