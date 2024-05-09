/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
