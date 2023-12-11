/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.neatlogic.autoexecrunner.store.mongodb;


import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.dto.MongoDbVo;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.exception.MongoDataSourceNotFoundException;
import com.neatlogic.autoexecrunner.exception.ConnectRefusedException;
import com.neatlogic.autoexecrunner.util.RestUtil;
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
            String result = StringUtils.EMPTY;
            String CALLBACK_PROCESS_UPDATE_URL = "mongodb/datasource/get";
            String url = String.format("%s/api/rest/%s", Config.NEATLOGIC_ROOT(), CALLBACK_PROCESS_UPDATE_URL);
            result = RestUtil.sendRequest(new RestVo(url, new JSONObject(), AuthenticateType.BEARER.getValue(), TenantContext.get().getTenantUuid()));
            try {
                JSONObject resultJson = JSONObject.parseObject(result);
                if (MapUtils.isNotEmpty(resultJson) && !Objects.equals(resultJson.getString("Status"), "OK")) {
                    if (resultJson.containsKey("Message")) {
                        throw new RuntimeException(resultJson.getString("Message"));
                    } else {
                        throw new RuntimeException(resultJson.toJSONString());
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
                throw new ConnectRefusedException(url + ":" + result);
            }
        } else {
            return mongoDbMap.get(TenantContext.get().getTenantUuid()).getDatabase(mongoDatabaseMap.get(TenantContext.get().getTenantUuid()));
        }
    }
}
