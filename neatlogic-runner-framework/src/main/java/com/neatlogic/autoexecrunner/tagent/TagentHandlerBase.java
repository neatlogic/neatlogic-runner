package com.neatlogic.autoexecrunner.tagent;

import com.alibaba.fastjson.JSONObject;

public abstract class TagentHandlerBase {

    public abstract String getName();

    public abstract JSONObject execute(JSONObject param);
}
