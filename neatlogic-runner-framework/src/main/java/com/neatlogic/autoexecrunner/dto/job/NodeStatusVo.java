package com.neatlogic.autoexecrunner.dto.job;

import com.alibaba.fastjson.JSONArray;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * @author lvzk
 * @since 2021/6/22 15:33
 **/
@Document
public class NodeStatusVo {
    @Id
    private ObjectId id;
    @Field
    private String host;
    @Field
    private String jobid;
    @Field
    private String phase;
    @Field
    private Integer port;
    private Date createDate;
    private JSONArray data;

    public NodeStatusVo(String jobId, String phase, String host, Integer port) {
        this.jobid = jobId;
        this.phase = phase;
        this.host = host;
        this.port = port;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getJobid() {
        return jobid;
    }

    public void setJobid(String jobid) {
        this.jobid = jobid;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public JSONArray getData() {
        return data;
    }

    public void setData(JSONArray data) {
        this.data = data;
    }
}
