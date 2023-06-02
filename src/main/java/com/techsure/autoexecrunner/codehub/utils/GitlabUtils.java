package com.techsure.autoexecrunner.codehub.utils;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yujh
 * 
 *         Gitlab工具类
 *
 */
public class GitlabUtils {
    private final static Logger logger = LoggerFactory.getLogger(GitlabUtils.class);

    public static JSONObject createProject(JSONObject jsonObj) {

        GitlabApi gitlabApi = new GitlabApi(jsonObj);

        String name = jsonObj.getString("repositoryName");
        String path = StringUtils.stripEnd(jsonObj.getString("repoPath"), "/");
        String visibility = jsonObj.getString("visibility");
        String groups = "";
        Integer namespaceId = null;

        // 没有所属组则会在当前用户的命名空间下创建, 但是会导致仓库地址不正确
        if (!path.contains("/")) {
            throw new ApiRuntimeException("仓库地址的所属组不能为空");
        }

        if (gitlabApi.existsProject(path)) {
            logger.info("项目已经在GitLab上存在");
            return null;
        }

        groups = StringUtils.substringBeforeLast(path, "/");
        path = StringUtils.substringAfterLast(path, "/");
        JSONArray allGroups = gitlabApi.listAllGroups();

        boolean isMatch = false;
        for (int i = 0; i < allGroups.size(); i++) {
            JSONObject jsonObject = allGroups.getJSONObject(i);
            if (jsonObject.getString("full_path").equalsIgnoreCase(groups)) {
                isMatch = true;
                namespaceId = jsonObject.getIntValue("id");
            }
        }
        if (!isMatch) {
            throw new ApiRuntimeException("无法找到仓库地址上的项目组");
        }
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("path", path);
        obj.put("namespace_id", namespaceId);

        // 字段保留 可能以后扩展
        if (StringUtils.isNotEmpty(visibility)) {
            obj.put("visibility", visibility);
        }

        return gitlabApi.createProject(obj);
    }

}
