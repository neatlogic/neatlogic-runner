/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package com.techsure.autoexecrunner.store.mongodb;


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
import com.techsure.autoexecrunner.util.RestUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.Objects;

//@Configuration
public class CodedriverMongoDbFactory extends SimpleMongoClientDatabaseFactory {
    Logger logger = LoggerFactory.getLogger(CodedriverMongoDbFactory.class);

    public CodedriverMongoDbFactory(String connectionString) {
        super(connectionString);
    }

    @Override
    protected MongoDatabase doGetMongoDatabase(String dbName) {
        //TODO 先判断缓存是否存在
        if (1 == 1) {
            String result = StringUtils.EMPTY;
            String CALLBACK_PROCESS_UPDATE_URL = "mongodb/datasource/get";
            String url = String.format("%s/api/rest/%s", Config.CODEDRIVER_ROOT(), CALLBACK_PROCESS_UPDATE_URL);
            result = RestUtil.sendRequest(new RestVo(url, new JSONObject(), AuthenticateType.BEARER.getValue(), TenantContext.get().getTenantUuid()));
            JSONObject resultJson = JSONObject.parseObject(result);
            if (MapUtils.isNotEmpty(resultJson) && Objects.equals(resultJson.getString("Status"), "ERROR")) {
                throw new RuntimeException(resultJson.getString("Message"));
            }
            JSONObject returnJson = JSONObject.parseObject(result).getJSONObject("Return");
            if (MapUtils.isEmpty(returnJson)) {
                throw new MongoDataSourceNotFoundException(TenantContext.get().getTenantUuid());
            }
            MongoDbVo mongoDbVo = returnJson.toJavaObject(MongoDbVo.class);
            MongoClient client = MongoClients.create("mongodb://" + mongoDbVo.getUsername() + ":" + mongoDbVo.getPasswordPlain() + "@" + mongoDbVo.getHost() + "/" + mongoDbVo.getDatabase() + (StringUtils.isNotBlank(mongoDbVo.getOption()) ? "?" + mongoDbVo.getOption() : ""));
            return client.getDatabase(mongoDbVo.getDatabase());
        }
        return null;
    }
}
