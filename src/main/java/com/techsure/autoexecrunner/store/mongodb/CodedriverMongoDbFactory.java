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
import com.techsure.autoexecrunner.util.RestUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

//@Configuration
public class CodedriverMongoDbFactory extends SimpleMongoClientDatabaseFactory {
    Logger logger = LoggerFactory.getLogger(CodedriverMongoDbFactory.class);
    //@Autowired
    //MongoDataSources mongoDataSources;

    /*public CodedriverMongoDbFactory(@Qualifier("getMongoClient") MongoClient mongoClient, String databaseName) {
        super(mongoClient, databaseName);
    }*/

    public CodedriverMongoDbFactory(MongoClient mongoClient, String databaseName) {
        super(mongoClient, databaseName);
    }

    @Override
    protected MongoDatabase doGetMongoDatabase(String dbName) {
        //TODO 先判断缓存是否存在
        if (1 == 1) {
            String result = StringUtils.EMPTY;
            String CALLBACK_PROCESS_UPDATE_URL = "mongodb/datasource/get";
            String url = Config.CALLBACK_URL() + CALLBACK_PROCESS_UPDATE_URL;
            try {
                result = RestUtil.sendRequest(new RestVo(url, new JSONObject(), AuthenticateType.BEARER.getValue(), TenantContext.get().getTenantUuid()));
                MongoDbVo mongoDbVo = JSONObject.parseObject(result).getJSONObject("Return").toJavaObject(MongoDbVo.class);
                MongoClient client = MongoClients.create("mongodb://" + mongoDbVo.getUsername() + ":" + mongoDbVo.getPasswordPlain() + "@" + mongoDbVo.getHost() + ":" + mongoDbVo.getPort() + "/" + mongoDbVo.getDatabase()+"?authSource=admin");
                return client.getDatabase(mongoDbVo.getDatabase());
            } catch (Exception e) {
                logger.error("do RESTFul api failed,url: #{},result: #{}", url, result);
                throw new RuntimeException(String.format("do RESTFul api failed,url: %s,result: %s", url, result));
            }
        }
        return null;
    }
}
