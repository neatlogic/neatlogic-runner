package com.techsure.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.exception.tagent.*;
import com.techsure.autoexecrunner.restful.core.publicapi.PublicBinaryStreamApiComponentBase;
import com.techsure.autoexecrunner.tagent.handler.TagentResultHandler;
import com.techsure.autoexecrunner.util.RC4Util;
import com.techsure.tagent.client.TagentClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

@Service
public class TagentUpgradeApi extends PublicBinaryStreamApiComponentBase {

    private static final Logger logger = LoggerFactory.getLogger(TagentUpgradeApi.class);

    @Override
    public String getName() {
        return "tagent升级接口";
    }

    @Override
    public String getToken() {
        return "tagent/upgrade";
    }

    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {

        String fileName = paramObj.getString("fileName");
        MultipartFile multipartFile = null;
        TagentResultHandler handler = new TagentResultHandler();

        try {
            //获得升级文件
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            multipartFile = multipartRequest.getFile(fileName);
            InputStream input = multipartFile.getInputStream();

            //获得os类型
            TagentClient tagentClient = new TagentClient(paramObj.getString("ip"), Integer.parseInt(paramObj.getString("port")), RC4Util.decrypt(paramObj.getString("credential").substring(4)));
            String osType = tagentClient.getAgentOsType();
            //创建tmp，无论是否存在都创建，则不抓异常
            try {
                tagentClient.execCmd("cd \"$TAGENT_BASE\" && mkdir tmp", paramObj.getString("user"), 10000, handler);
            } catch (Exception e) {
            }
            int uploadStatus = tagentClient.upload(input, fileName, "$TAGENT_BASE/tmp", null);

            //兼容老版本的tagent的upload不支持路径中的环境变量的情况
            if (uploadStatus != 0) {
                //获得tagent安装路径
                int getTagentHomeStatus = tagentClient.execCmd("echo %TAGENT_BASE% ", null, 10000, handler);
                if (getTagentHomeStatus != 0) {
                    throw new TagentInstallPathFailedException();
                }
                String path = JSONObject.parseObject(handler.getContent()).getJSONArray("std").getString(0).trim().replaceAll("\n", "");

                //上传tagent安装包
                uploadStatus = tagentClient.upload(input, fileName, path + "/tmp", null);
                if (uploadStatus != 0) {
                    throw new TagentUploadPkgFailedException();
                }
            }

            // 解压  替换文件 升级
            int cmdExecStatus = -1;
            String ignoreFile = paramObj.getString("ignoreFile");
            if ("windows".equals(osType)) {
                StringBuilder cmd = new StringBuilder();
                cmd.append("7z x -ttar -y \"%TAGENT_BASE%\\tmp\\").append(fileName).append("\"");
                if (StringUtils.isNotBlank(ignoreFile)) {
                    ignoreFile = ignoreFile.trim();
                    cmd.append(" -x!").append(ignoreFile.replaceAll("\\s+", " -x!"));
                }

                cmd.append(" -o\"%TAGENT_BASE%\" bin lib tools mod");
                cmdExecStatus = tagentClient.execCmd(cmd.toString(), null, 10000, handler);

            } else {
                // 切换至agent目录解压升级文件
                cmdExecStatus = tagentClient.execCmd("cd \"$TAGENT_BASE\" && tar xvf \"$TAGENT_BASE/tmp/" + fileName + "\" bin lib tools mod ", paramObj.getString("user"), 10000, handler);
                if (cmdExecStatus != 0) {
                    throw new TagentDecompressionFailedException();
                }
            }

            //重启
            int reloadStatus = tagentClient.reload();

            if (reloadStatus != 0) {
                throw new TagentRestartFailedException();
            }
        } catch (Exception e) {
            logger.error("upgrade tagent failed, exception: " + ExceptionUtils.getStackTrace(e));
            throw new TagentUpgradeFailedException(e.getMessage());
        }
        return null;
    }

}
