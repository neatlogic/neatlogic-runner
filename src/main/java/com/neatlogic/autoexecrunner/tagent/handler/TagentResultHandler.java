package com.neatlogic.autoexecrunner.tagent.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.tagent.handler.TagentCmdOutHandler;

import java.io.ByteArrayOutputStream;

public class TagentResultHandler implements TagentCmdOutHandler {
    private Integer status;
    private String context;
    private JSONArray stdDataArray = new JSONArray();
    private JSONArray errDataArray = new JSONArray();
    private ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    @Override
    public void handleStdOutLine(String line) {
        stdDataArray.add(line);
    }

    @Override
    public void handleErrOutLine(String line) {
        errDataArray.add(line);

    }

    @Override
    public String getContent() {
        JSONObject result = new JSONObject();
        result.put("std", stdDataArray);
        result.put("err", errDataArray);
        context = result.toJSONString();
        return context;
    }

    public void setContent(String context) {
        this.context = context;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;

    }

    @Override
    public int getStatus() {
        if (status != null) {
            return status;
        } else if (errDataArray.size() > 0) {
            return 1;
        } else {
            return 0;
        }

    }

    @Override
    public void handleFile(byte[] fileStream) {
        outStream.write(fileStream, 0, fileStream.length);
    }

    public byte[] getFileByteArray() {
        return outStream.toByteArray();
    }
}