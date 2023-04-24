/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package com.techsure.autoexecrunner.store.mongodb;


import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.techsure.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.AuthenticateType;
import com.techsure.autoexecrunner.dto.MongoDbVo;
import com.techsure.autoexecrunner.dto.RestVo;
import com.techsure.autoexecrunner.exception.MongoDataSourceNotFoundException;
import com.techsure.autoexecrunner.exception.ConnectRefusedException;
import com.techsure.autoexecrunner.util.RestUtil;
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
                if (MapUtils.isNotEmpty(resultJson) && Objects.equals(resultJson.getString("Status"), "ERROR")) {
                    throw new RuntimeException(resultJson.getString("Message"));
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
            }catch (JSONException ex){
                throw new ConnectRefusedException(url+":"+result);
            }
        }else{
            return mongoDbMap.get(TenantContext.get().getTenantUuid()).getDatabase(mongoDatabaseMap.get(TenantContext.get().getTenantUuid()));
        }
    }
}
