package com.techsure.autoexecproxy.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.core.ExecManager;
import com.techsure.autoexecproxy.dto.FileVo;
import com.techsure.autoexecproxy.restful.annotation.Input;
import com.techsure.autoexecproxy.restful.annotation.Output;
import com.techsure.autoexecproxy.restful.annotation.Param;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecproxy.util.FileUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2021/5/13 14:31
 **/
@Component
public class JobPhaseNodeSqlListApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取作业节点sql列表";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "nodeId", type = ApiParamType.LONG, desc = "作业nodeId", isRequired = true),
            @Param(name = "resourceId", type = ApiParamType.LONG, desc = "资源id"),
            @Param(name = "phase", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phase = jsonObj.getString("phase");
        String ip = jsonObj.getString("ip");
        String port = jsonObj.getString("port") == null ? StringUtils.EMPTY : jsonObj.getString("port");
        String logPath = Config.LOG_PATH() + File.separator + ExecManager.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phase + File.separator;
        logPath += ip + "-" + port + "-" + jsonObj.getString("resourceId") ;
        List<FileVo> fileVoList = FileUtil.readFileList(logPath);
        JSONArray resultArray = new JSONArray();
        if(CollectionUtils.isNotEmpty(fileVoList)){
            for(FileVo fileVo : fileVoList) {
                if(fileVo.getIsDirectory() == 0) {
                    String sqlStatusContent = FileUtil.getReadFileContent(fileVo.getFilePath());
                    JSONObject sqlStatus = JSONObject.parseObject(sqlStatusContent);
                    resultArray.add(sqlStatus);
                }
            }
        }
        return resultArray;
    }

    @Override
    public String getToken() {
        return "/job/phase/node/sql/list";
    }
}
