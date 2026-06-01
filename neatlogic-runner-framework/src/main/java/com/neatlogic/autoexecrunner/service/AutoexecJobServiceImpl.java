package com.neatlogic.autoexecrunner.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

@Service
public class AutoexecJobServiceImpl implements AutoexecJobService {
    @Override
    public JSONObject getJobOperationInputParam(JSONObject jsonObj) {
        Long jobId = jsonObj.getLong("jobId");
        String ip = jsonObj.getString("ip");
        String port = jsonObj.getString("port") == null ? StringUtils.EMPTY : jsonObj.getString("port");
        String execMode = jsonObj.getString("execMode");
        String phaseName = jsonObj.getString("phase");
        String jobPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder());
        String paramPath = jobPath + File.separator + "input" + File.separator;
        JSONObject result = new JSONObject();
        File desFile = new File(paramPath);
        boolean isInputExit = false;
        if (desFile.exists()) {
            if (Arrays.asList("target", "runner_target").contains(execMode)) {
                paramPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") + ".json";
            } else {
                paramPath += "local-0-0.json";
            }
            JSONObject param = JSONObject.parseObject(FileUtil.getReadFileContent(paramPath));
            if (MapUtils.isNotEmpty(param)) {
                isInputExit = true;
                result = param;
            }
        }

        if (!isInputExit) {
            JSONObject options = new JSONObject();
            JSONArray arguments = new JSONArray();
            result.put("options",options);
            result.put("arguments",arguments);
            String content = FileUtil.getReadFileContent(jobPath + File.separator + "params.json");
            if (StringUtils.isNotBlank(content)) {
                JSONObject paramsJson = JSONObject.parseObject(content);
                JSONArray groupList = paramsJson.getJSONArray("runFlow");
                OUT:
                for (int i = 0; i < groupList.size(); i++) {
                    JSONObject group = groupList.getJSONObject(i);
                    JSONArray phaseList = group.getJSONArray("phases");
                    for (int j = 0; j < phaseList.size(); j++) {
                        JSONObject phase = phaseList.getJSONObject(j);
                        if (Objects.equals(phase.getString("phaseName"), phaseName)) {
                            JSONArray operationList = phase.getJSONArray("operations");
                            getOperationList(operationList, result);
                            break OUT;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * 递归获取工具列表
     *
     * @param operationList        工具列表
     * @param result 返回入参
     */
    private void getOperationList(JSONArray operationList, JSONObject result) {
        for (int k = 0; k < operationList.size(); k++) {
            JSONObject operation = operationList.getJSONObject(k);
            JSONObject resultOperation = new JSONObject();
            resultOperation.put("options",operation.getJSONObject("opt"));
            resultOperation.put("arguments",operation.getJSONArray("arg"));
            result.put(operation.getString("opId"), resultOperation);
            if (operation.containsKey("if")) {
                JSONArray ifOperationArray = operation.getJSONArray("if");
                getOperationList(ifOperationArray, result);
            }
            if (operation.containsKey("else")) {
                JSONArray elseOperationArray = operation.getJSONArray("else");
                getOperationList(elseOperationArray, result);
            }
        }
    }
}
