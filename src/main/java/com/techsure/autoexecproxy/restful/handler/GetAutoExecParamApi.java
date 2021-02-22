package com.techsure.autoexecproxy.restful.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Component;

/**
 * @Title: AutoExecApi
 * @Package: com.techsure.autoexecproxy.restful.handler
 * @Description: 执行接口
 * @author: chenqiwei
 * @date: 2021/2/2210:44 上午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
@Component
public class GetAutoExecParamApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取执行配置接口";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String param = "{\n" +
                "        \"taskId\": \"156\",\n" +
                "        \"stepId\": \"353\",\n" +
                "        \"preTaskId\": \"156\",\n" +
                "        \"preStepId\": \"353\",\n" +
                "        \"parallel\": 10,\n" +
                "        \"arg\": {\n" +
                "                \"fetchfile\": \"testfile\"\n" +
                "        },\n" +
                "        \"pre\": [{\n" +
                "                \"opId\": \"localdemo\",\n" +
                "                \"opName\": \"local测试\",\n" +
                "                \"opType\": \"local\",\n" +
                "                \"failIgnore\": false,\n" +
                "                \"arg\": {\n" +
                "                        \"tinput\": \"xxxx\",\n" +
                "                        \"tselect\": \"yyyy\",\n" +
                "                        \"tpassword\": \"{RC4}xxxxxxx\",\n" +
                "                        \"tfile\": \"testfile\"\n" +
                "                },\n" +
                "\n" +
                "                \"desc\": {\n" +
                "                        \"tinput\": \"input\",\n" +
                "                        \"tselect\": \"input\",\n" +
                "                        \"tpassword\": \"password\",\n" +
                "                        \"tfile\": \"file\"\n" +
                "                },\n" +
                "                \"output\": {\n" +
                "                        \"outtext\": \"text\",\n" +
                "                        \"outfile\": \"file\"\n" +
                "                }\n" +
                "        }],\n" +
                "        \"run\": [{\n" +
                "                        \"opId\": \"localdemo\",\n" +
                "                        \"opName\": \"local测试\",\n" +
                "                        \"opType\": \"local\",\n" +
                "                        \"failIgnore\": false,\n" +
                "                        \"arg\": {\n" +
                "                                \"tinput\": \"xxxx\",\n" +
                "                                \"tselect\": \"yyyy\",\n" +
                "                                \"tpassword\": \"{RC4}xxxxxxx\",\n" +
                "                                \"tfile\": \"testfile\"\n" +
                "                        },\n" +
                "\n" +
                "                        \"desc\": {\n" +
                "                                \"tinput\": \"input\",\n" +
                "                                \"tselect\": \"input\",\n" +
                "                                \"tpassword\": \"password\",\n" +
                "                                \"tfile\": \"file\"\n" +
                "                        },\n" +
                "                        \"output\": {\n" +
                "                                \"outtext\": \"text\",\n" +
                "                                \"outfile\": \"file\"\n" +
                "                        }\n" +
                "                },\n" +
                "                {\n" +
                "                        \"opId\": \"localremotedemo\",\n" +
                "                        \"opName\": \"localremote测试\",\n" +
                "                        \"opType\": \"localremote\",\n" +
                "                        \"failIgnore\": false,\n" +
                "                        \"arg\": {\n" +
                "                                \"tinput\": \"xxxx\",\n" +
                "                                \"tselect\": \"yyyy\",\n" +
                "                                \"tpassword\": \"{RC4}xxxxxxx\",\n" +
                "                                \"tfile\": \"testfile\"\n" +
                "                        },\n" +
                "\n" +
                "                        \"desc\": {\n" +
                "                                \"tinput\": \"input\",\n" +
                "                                \"tselect\": \"input\",\n" +
                "                                \"tpassword\": \"password\",\n" +
                "                                \"tfile\": \"file\"\n" +
                "                        },\n" +
                "                        \"output\": {\n" +
                "                                \"outtext\": \"text\",\n" +
                "                                \"outfile\": \"file\"\n" +
                "                        }\n" +
                "                }\n" +
                "        ],\n" +
                "        \"post\": [{\n" +
                "                        \"opId\": \"localdemo\",\n" +
                "                        \"opName\": \"local测试\",\n" +
                "                        \"opType\": \"local\",\n" +
                "                        \"failIgnore\": false,\n" +
                "                        \"arg\": {\n" +
                "                                \"tinput\": \"xxxx\",\n" +
                "                                \"tselect\": \"yyyy\",\n" +
                "                                \"tpassword\": \"{RC4}xxxxxxx\",\n" +
                "                                \"tfile\": \"testfile\"\n" +
                "                        },\n" +
                "\n" +
                "                        \"desc\": {\n" +
                "                                \"tinput\": \"input\",\n" +
                "                                \"tselect\": \"input\",\n" +
                "                                \"tpassword\": \"password\",\n" +
                "                                \"tfile\": \"file\"\n" +
                "                        },\n" +
                "                        \"output\": {\n" +
                "                                \"outtext\": \"text\",\n" +
                "                                \"outfile\": \"file\"\n" +
                "                        }\n" +
                "                },\n" +
                "                {\n" +
                "                        \"opId\": \"localdemo\",\n" +
                "                        \"opName\": \"local测试\",\n" +
                "                        \"opType\": \"local\",\n" +
                "                        \"failIgnore\": false,\n" +
                "                        \"arg\": {\n" +
                "                                \"tinput\": \"xxxx\",\n" +
                "                                \"tselect\": \"yyyy\",\n" +
                "                                \"tpassword\": \"{RC4}xxxxxxx\",\n" +
                "                                \"tfile\": \"testfile\"\n" +
                "                        },\n" +
                "\n" +
                "                        \"desc\": {\n" +
                "                                \"tinput\": \"input\",\n" +
                "                                \"tselect\": \"input\",\n" +
                "                                \"tpassword\": \"password\",\n" +
                "                                \"tfile\": \"file\"\n" +
                "                        },\n" +
                "                        \"output\": {\n" +
                "                                \"outtext\": \"text\",\n" +
                "                                \"outfile\": \"file\"\n" +
                "                        }\n" +
                "                }\n" +
                "        ]\n" +
                "}";

        return JSONObject.parse(param);
    }

    @Override
    public boolean isRaw() {
        return true;
    }

    @Override
    public String getToken() {
        return "getparam";
    }
}
