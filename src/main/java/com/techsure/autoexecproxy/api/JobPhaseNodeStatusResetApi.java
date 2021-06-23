package com.techsure.autoexecproxy.api;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.core.ExecManager;
import com.techsure.autoexecproxy.dto.CommandVo;
import com.techsure.autoexecproxy.dto.job.NodeStatusVo;
import com.techsure.autoexecproxy.restful.annotation.Input;
import com.techsure.autoexecproxy.restful.annotation.Output;
import com.techsure.autoexecproxy.restful.annotation.Param;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.xml.stream.events.Comment;

import java.util.Map;

import static org.springframework.data.mongodb.core.query.Criteria.where;
/**
 * @author lvzk
 * @since 2021/6/2 15:31
 **/
@Component
public class JobPhaseNodeStatusResetApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "重置作业阶段节点";
    }

    @Resource
    MongoTemplate mongoTemplate;

    @Input({
            @Param(name = "jobId", type = ApiParamType.STRING, desc = "作业Id", isRequired = true),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "host", type = ApiParamType.STRING, desc = "ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Document document = new Document();
        document.put("jobId",jsonObj.getString("jobId"));
        document.put("phase",jsonObj.getString("phase"));
        document.put("host",jsonObj.getString("host"));
        document.put("port",jsonObj.getInteger("port"));
        Document result = mongoTemplate.getCollection("node_status").findOneAndDelete(document);
        if(result != null){
            return 1;
        }
        return 0;
    }

    @Override
    public String getToken() {
        return "/job/phase/node/status/reset";
    }
}
