/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.neatlogic.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.exception.job.FifoWriteException;
import com.neatlogic.autoexecrunner.util.FifoWriter;
import com.neatlogic.autoexecrunner.util.FileSafeUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * 校验交互管道的工作目录并投递输入。
 * @author lvzk
 * @since 2021/8/17 14:31
 **/
@Component
public class JobPhaseNodeSubmitWaitInputApi extends PrivateApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(JobPhaseNodeSubmitWaitInputApi.class);

    @Override
    public String getName() {
        return "提交作业节点waitInput";
    }

    /** 保留历史请求格式，成功表示本次输入已完整写入。 */
    @Input({
            @Param(name = "option", type = ApiParamType.STRING, desc = "waitInput 选项", isRequired = true),
            @Param(name = "pipeFile", type = ApiParamType.STRING, desc = "waitInput 管道文件路径", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String option = jsonObj.getString("option");
        String pipeFile = jsonObj.getString("pipeFile");
        Long jobId = jsonObj.getLong("jobId");
        Long nodeId = jsonObj.getLong("nodeId");
        String root = Config.AUTOEXEC_HOME();
        try {
            // pipeFile 是状态文件内的原始路径，保留合法文件名中的 + 和百分号。
            File file = getPipeFile(root, pipeFile);
            if (jobId != null) {
                // 先约束到工作根目录，再按作业收紧；旧客户端只提供管道和选项。
                File jobDirectory = new File(root, JobUtil.getJobPath(jobId.toString(), new StringBuilder()));
                FileSafeUtil.validateFilePathAllowed(jobDirectory, file);
            }
            FifoWriter.write(file.toPath(), (option + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        } catch (ApiRuntimeException ex) {
            logger.error("交互管道投递失败，jobId={}，nodeId={}", jobId, nodeId, ex);
            throw ex;
        } catch (IOException | RuntimeException ex) {
            logger.error("访问交互管道失败，jobId={}，nodeId={}", jobId, nodeId, ex);
            throw new FifoWriteException("IO_ERROR", ex);
        }
        return null;
    }

    /** 将引擎上报的工作根真实路径还原为配置路径，再统一检查目录边界和禁用路径。 */
    private static File getPipeFile(String root, String pipeFile) throws IOException {
        Path configuredRoot = new File(root).toPath().toAbsolutePath().normalize();
        Path reported = new File(pipeFile).toPath().normalize();
        if (reported.isAbsolute() && !reported.startsWith(configuredRoot)) {
            Path realRoot = configuredRoot.toRealPath();
            if (reported.startsWith(realRoot)) {
                // 只替换已知工作根前缀，保留管道本身和中间目录的软链接供后续校验。
                pipeFile = realRoot.relativize(reported).toString();
            }
        }
        return FileSafeUtil.getChildPath(root, pipeFile);
    }

    @Override
    public String getToken() {
        return "/job/phase/node/submit/waitInput";
    }
}
