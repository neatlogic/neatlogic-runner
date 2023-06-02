package com.techsure.autoexecrunner.codehub.api;

import java.util.HashSet;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

/**
 * 给外部系统调用的 Gitlab API 接口
 * 
 * 注意: 
 *  查询搜索方法命名以search前缀开头, 支持分页, 返回一个JSONObject列表, 具体的列表数据放于list字段, 并且包含总页数total等信息
 *  查看列表方法命名以list前缀开头, 复数单词结尾, 是不分页的全量查询, 只返回一个JSONArray列表
 *  search和list的方法只需要有read权限即可
 *  去除某种权限的方法命名以remove前缀开头, 无返回值, 如遇错误则会throw
 *  
 * @author yujh
 */
@Service
public class GitlabApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "外部系统调用的GitlabAPI接口";
    }

    @Override
    public String getToken() {
        return "codehub/gitlab";
    }

    @Input({
            @Param(name = "method", type = ApiParamType.STRING, desc = "gitlab具体要调用的方法")
    })
    @Description(desc = "外部系统调用的GitlabAPI接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String method = jsonObj.getString("method");
        com.techsure.autoexecrunner.codehub.utils.GitlabApi gitlabApi = new com.techsure.autoexecrunner.codehub.utils.GitlabApi(jsonObj);

        if ("searchBranches".equals(method)) {
            // 搜索仓库的分支
            return gitlabApi.searchBranches();
        } if ("listBranches".equals(method)) {
            // 搜索仓库的分支
            return gitlabApi.listBranches();
        } if ("listProtectBranches".equals(method)) {
            // 列出已经保护的分支
            return gitlabApi.listProtectBranches();
        } else if ("protectBranch".equals(method)) {
            // 添加分支保护
            return gitlabApi.protectBranch(jsonObj);
        } else if ("removeProtectBranch".equals(method)) {
            // 删除分支保护
            gitlabApi.removeProtectBranch(jsonObj.getString("name"));
        } else if ("addProjectMember".equals(method)) {
            // 添加用户类型的权限
            // gitlab已经存在的会报错, 这里需要采用覆盖更新
            JSONArray member = gitlabApi.listProjectMembers();
            Set<Integer> userIds = new HashSet<>();
            for (int i = 0; i < member.size(); i++) {
                userIds.add(member.getJSONObject(i).getIntValue("id"));
            }
            JSONObject retObj = new JSONObject();
            String[] addUserIds = jsonObj.getString("user_id").split(",");
            for (String addUserId : addUserIds) {
                if (userIds.contains(Integer.parseInt(addUserId))) {
                    retObj = gitlabApi.updateProjectMember(Integer.parseInt(addUserId), jsonObj.getIntValue("access_level"), jsonObj.getString("expires_at"));
                } else {
                    retObj = gitlabApi.addProjectMember(Integer.parseInt(addUserId), jsonObj.getIntValue("access_level"), jsonObj.getString("expires_at"));
                }
            }
            return retObj;
        } else if ("updateProjectMember".equals(method)) {
            // 更新用户类型的权限
            return gitlabApi.updateProjectMember(jsonObj.getIntValue("user_id"), jsonObj.getIntValue("access_level"), jsonObj.getString("expires_at"));
        } else if ("removeProjectMember".equals(method)) {
            // 删除用户类型的权限
            gitlabApi.removeProjectMember(jsonObj.getIntValue("user_id"));
        } else if ("listProjectMembers".equals(method)) {
            // 列出项目下所有成员的权限信息
            return gitlabApi.listProjectMembers(jsonObj.getBooleanValue("inheritedMembers"));
        }  else if ("searchProjectMembers".equals(method)) {
            // 搜索项目下所有成员的权限信息, 带分页功能的
            return gitlabApi.searchProjectMembers();
        } else if ("addShareProjectWithGroup".equals(method)) {
            // 添加项目组类型的权限
            // gitlab原本的接口如果添加已经存在的会报错, 这里需要采用覆盖更新
            JSONArray group = gitlabApi.listProjectSharedWithGroups(false);
            Set<Integer> groupIds = new HashSet<>();
            for (int i = 0; i < group.size(); i++) {
                groupIds.add(group.getJSONObject(i).getIntValue("group_id"));
            }
            JSONObject retObj = new JSONObject();
            String[] addGroupIds = jsonObj.getString("group_id").split(",");
            for (String addGroupId : addGroupIds) {
                if (groupIds.contains(Integer.parseInt(addGroupId))) {
                    // TODO !! 暂时没有找到更新的api接口 只能先删除后添加
                    gitlabApi.removeShareProjectWithGroup(Integer.parseInt(addGroupId));
                }
                retObj = gitlabApi.addShareProjectWithGroup(Integer.parseInt(addGroupId), jsonObj.getIntValue("group_access"), jsonObj.getString("expires_at"));
            }
            return retObj;
        } else if ("removeShareProjectWithGroup".equals(method)) {
            // 删除项目组类型的权限
            gitlabApi.removeShareProjectWithGroup(jsonObj.getIntValue("group_id"));
        } else if ("listProjectSharedWithGroups".equals(method)) {
            // 列出项目下所有项目组的权限信息
            return gitlabApi.listProjectSharedWithGroups(jsonObj.getBooleanValue("withGroupMembers"));
        } else if ("listGroupMembers".equals(method)) {
            // 列出项目组成员
            return gitlabApi.listGroupMembers(jsonObj.getString("group"));
        }  else if ("searchGroupMembers".equals(method)) {
            // 列出项目组成员
            return gitlabApi.searchGroupMembers(jsonObj.getString("group"));
        } else if ("searchUsers".equals(method)) {
            // 搜索用户信息
            return gitlabApi.searchUsers(jsonObj.getString("search"));
        } else if ("searchGroups".equals(method)) {
            // 搜索项目组
            return gitlabApi.searchGroups(jsonObj.getString("search"));
        }  else if ("getUser".equals(method)) {
            // 获取用户信息
            return gitlabApi.getUser(jsonObj.getIntValue("id"));
        }   else if ("listAllUsers".equals(method)) {
            // 列出所有用户信息
            return gitlabApi.listAllUsers();
        } else {
            throw new ApiRuntimeException("method error: " + method);
        }

        return null;

    }

    /*    @Override
    public JSONArray help() {
        JSONArray jsonArray = new JSONArray();

        ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", "method");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "gitlab具体要调用的方法");
        jsonArray.add(jsonObj);
        ApiHelpUtils.addSVNWorkingCopyPathJsonObj(jsonArray);
        return jsonArray;
    }*/


}
