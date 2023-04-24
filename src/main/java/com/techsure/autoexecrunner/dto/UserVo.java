package com.techsure.autoexecrunner.dto;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.EntityField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserVo implements Serializable {

    private static final long serialVersionUID = 3670529362145832083L;
    @JSONField(serialize=false)
    private transient String keyword;

    private Long id;
	@EntityField(name = "用户uuid", type = ApiParamType.STRING)
	private String uuid;
	@EntityField(name = "用户id", type = ApiParamType.STRING)
	private String userId;
	@EntityField(name = "用户姓名", type = ApiParamType.STRING)
	private String userName;
	private String tenant;
	@EntityField(name = "邮箱", type = ApiParamType.STRING)
	private String email;
	private String password;
	private String roleUuid;
	@EntityField(name = "是否激活(1:激活;0:未激活)", type = ApiParamType.INTEGER)
	private Integer isActive;
	@EntityField(name = "电话", type = ApiParamType.STRING)
	private String phone;
	private String dept;
	private String company;
	private String position;
	@EntityField(name = "其他属性", type = ApiParamType.STRING)
	private String userInfo;
	@EntityField(name = "头像", type = ApiParamType.STRING)
	private String avatar;
	@EntityField(name = "VIP级别(0,1,2,3,4,5)", type = ApiParamType.ENUM)
	private Integer vipLevel;
	private String teamUuid;
	private String auth;
	private String authGroup;
	private JSONObject userInfoObj;

	@EntityField(name = "是否已删除", type = ApiParamType.ENUM)
	private Integer isDelete;

	@EntityField(name = "用户所在组的头衔", type = ApiParamType.ENUM)
	private String title;

	@EntityField(name = "用户所在组的头衔中文名", type = ApiParamType.ENUM)
	private String titleText;

	@EntityField(name = "用户所在组uuid列表", type = ApiParamType.JSONARRAY)
	private List<String> teamUuidList = new ArrayList<>();
	private List<String> teamNameList = new ArrayList<>();
	
	@EntityField(name = "用户角色uuid列表", type = ApiParamType.JSONARRAY)
	private List<String> roleUuidList = new ArrayList<>();
	//private List<String> roleNameList = new ArrayList<>();
	
	@EntityField(name = "用户角色信息列表", type = ApiParamType.JSONARRAY)
	@JSONField(serialize=false)
	private String cookieAuthorization;
	@JSONField(serialize=false)
	private String authorization;

	public UserVo() {

	}

	public UserVo(String uuid){
		this.uuid = uuid;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getAuthGroup() {
		return authGroup;
	}

	public void setAuthGroup(String authGroup) {
		this.authGroup = authGroup;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		if (StringUtils.isNotBlank(password)) {
			if (!password.startsWith("{MD5}")) {
				password = DigestUtils.md5DigestAsHex(password.getBytes());
				password = "{MD5}" + password;
			}
		}
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getTeamUuid() {
		return teamUuid;
	}

	public void setTeamUuid(String teamUuid) {
		this.teamUuid = teamUuid;
	}

	public String getRoleUuid() {
		return roleUuid;
	}

	public void setRoleUuid(String roleUuid) {
		this.roleUuid = roleUuid;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getDept() {
		return dept;
	}

	public void setDept(String dept) {
		this.dept = dept;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getUserInfo() {
		return userInfo;
	}

	public void setUserInfo(String userInfo) {
		this.userInfo = userInfo;
	}

	public JSONObject getUserInfoObj() {
		if (userInfoObj == null && StringUtils.isNotBlank(userInfo)) {
			userInfoObj = JSONObject.parseObject(userInfo);
		}
		return userInfoObj;
	}

	public Integer getIsDelete() {
		return isDelete;
	}

	public void setIsDelete(Integer isDelete) {
		this.isDelete = isDelete;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAvatar() {
		if (StringUtils.isBlank(avatar) && StringUtils.isNotBlank(userInfo)) {
			JSONObject jsonObject = JSONObject.parseObject(userInfo);
			avatar = jsonObject.getString("avatar");
		}
		return avatar;
	}

	public void setUserInfoObj(JSONObject userInfoObj) {
		this.userInfoObj = userInfoObj;
	}

	public Integer getVipLevel() {
		return vipLevel;
	}

	public void setVipLevel(Integer vipLevel) {
		this.vipLevel = vipLevel;
	}

	/*public List<String> getRoleDescriptionList() {
		if(CollectionUtils.isEmpty(roleNameList) && CollectionUtils.isNotEmpty(roleList)) {
			for(RoleVo role : roleList) {
				roleNameList.add(role.getDescription());
			}
		}
		return roleNameList;
	}*/


	public String getTitleText() {
		return titleText;
	}

	public void setTitleText(String titleText) {
		this.titleText = titleText;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserVo other = (UserVo) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

//	public List<String> getValueList() {
//		if(CollectionUtils.isNotEmpty(valueList)) {
//			for(int i =0; i<valueList.size();i++) {
//				valueList.set(i,valueList.get(i).replaceAll(GroupSearch.USER.getValuePlugin(),""));
//			}
//		}
//		return valueList;
//	}
//
//	public void setValueList(List<String> valueList) {
//		this.valueList = valueList;
//	}

	public String getCookieAuthorization() {
		return cookieAuthorization;
	}

	public void setCookieAuthorization(String cookieAuthorization) {
		this.cookieAuthorization = cookieAuthorization;
	}

	public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    
}
