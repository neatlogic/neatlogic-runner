package com.neatlogic.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.codehub.git.GitWorkingCopy;
import com.neatlogic.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.neatlogic.autoexecrunner.codehub.utils.JSONUtils;
import com.neatlogic.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 查看某个commit下某文件的内容, 支持分页行数控制
 *
 * @author jh
 */
@Service
public class CommitFileContentApi extends PrivateApiComponentBase {

    private static final String CONTENT_FORMAT_ARRAY = "array";
    private static String CONTENT_FORMAT_STRING = "string";

    @Override
    public String getToken() {
        return "codehub/commit/commitfilecontent";
    }

    @Override
    public String getName() {
        return "查看某个commit下某文件的内容";
    }

    @Input({
            @Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
            @Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
            @Param(name = "branchesPath", type = ApiParamType.STRING, desc = "分支路径"),
            @Param(name = "mainBranch", type = ApiParamType.STRING, desc = "主分支"),
            @Param(name = "tagsPath", type = ApiParamType.STRING, desc = "tag路径"),
            @Param(name = "branchName", type = ApiParamType.STRING, desc = "分支名称"),
            @Param(name = "contentFormat", type = ApiParamType.STRING, desc = "文本"),
            @Param(name = "filePath", type = ApiParamType.STRING, desc = "读取的文件路径", isRequired = true),
            @Param(name = "commitId", type = ApiParamType.STRING, desc = "提交id", isRequired = true),
            @Param(name = "lineStart", type = ApiParamType.INTEGER, desc = "起始行数"),
            @Param(name = "lineEnd", type = ApiParamType.STRING, desc = "终止行数"),
            @Param(name = "url", type = ApiParamType.STRING, desc = "url", isRequired = true)
    })
    @Output({
    })
    @Description(desc = "查看commit文件内容接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String repoType = JSONUtils.optString(jsonObj, "repoType", "").trim().toLowerCase();
        String username = JSONUtils.optString(jsonObj, "username", "");
        String pwd = JSONUtils.optString(jsonObj, "password", "");
        String branchesPath = JSONUtils.optString(jsonObj, "branchesPath", "");
        String mainBranch = JSONUtils.optString(jsonObj, "mainBranch", "");
        String tagsPath = JSONUtils.optString(jsonObj, "tagsPath", "");
        String branchName = JSONUtils.optString(jsonObj, "branchName", "");
        String contentFormat = JSONUtils.optString(jsonObj, "contentFormat");

        // 用于前端diff点击更多时 指定取某个文件的diff信息, 如果有传则只去该文件的diff
        String filePath = JSONUtils.optString(jsonObj, "filePath", "");
        String commitId = JSONUtils.optString(jsonObj, "commitId", "");

        int lineStart = JSONUtils.optInt(jsonObj, "lineStart", -1);
        int lineEnd = JSONUtils.optInt(jsonObj, "lineEnd", -1);

        String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
        String url = jsonObj.getString("url");

        if (StringUtils.isEmpty(filePath)) {
            throw new ApiRuntimeException("文件路径不能为空");
        }
        if (StringUtils.isEmpty(commitId)) {
            throw new ApiRuntimeException("commitId不能为空");
        }

        boolean returnArray = CONTENT_FORMAT_STRING.equals(contentFormat) ? false : true;
        if (returnArray && (lineStart < 0 || lineEnd < 0 || lineStart > lineEnd)) {
            throw new ApiRuntimeException("行数控制参数非法");
        }

        Object ret = null;
        if (repoType.equals("svn")) {
            SVNWorkingCopy wc = new SVNWorkingCopy(wcPath, url, username, pwd, mainBranch, branchesPath, tagsPath);

            try {
                // 不能执行 update 操作，否则干扰合并
                if (returnArray) {
                    ret = wc.getFileLines(Long.parseLong(commitId), filePath, lineStart, lineEnd - lineStart + 1);
                } else {
                    ret = wc.getFileContent(Long.parseLong(commitId), filePath, Config.FILE_CONTENT_SHOW_MAX_SIZE);
                }
            } catch (Exception e) {
                throw new ApiRuntimeException(e);
            } finally {
                wc.close();
            }

        } else if (repoType.equals("gitlab")) {
            GitWorkingCopy wc = new GitWorkingCopy(wcPath, url, username, pwd);

            // 注意 git 和 svn 的区别，git取的是本地 .git 目录下的文件内容，所以要先 fetch 远程内容到本地
            // 仅需 fetch，不能 checkout/pull，否则影响 working copy 的文件
            wc.update();
            try {
                if (returnArray) {
                    ret = wc.getFileLines(commitId, filePath, lineStart, lineEnd - lineStart + 1);
                } else {
                    ret = wc.getFileContent(commitId, filePath, Config.FILE_CONTENT_SHOW_MAX_SIZE);
                }
            } catch (Exception e) {
                throw new ApiRuntimeException(e);
            } finally {
                wc.close();
            }
        }

        return ret;
    }


}
