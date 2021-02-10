package com.techsure.autoexecproxy.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.constvalue.AuthenticateType;
import com.techsure.autoexecproxy.restful.annotation.EntityField;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ApiVo implements Serializable {

    private static final long serialVersionUID = 3689437871016436622L;

    public enum Type {
        OBJECT("object", "对象模式", "rest/"), STREAM("stream", "json流模式", "stream/"), BINARY("binary", "字节流模式", "binary/");

        private final String name;
        private final String text;
        private final String urlPre;

        private Type(String _name, String _text, String _urlPre) {
            this.name = _name;
            this.text = _text;
            this.urlPre = _urlPre;
        }

        public String getValue() {
            return name;
        }

        public String getText() {
            return text;
        }

        public String getUrlPre() {
            return urlPre;
        }

        public static String getText(String name) {
            for (Type s : Type.values()) {
                if (s.getValue().equals(name)) {
                    return s.getText();
                }
            }
            return "";
        }

        public static String getUrlPre(String name) {
            for (Type s : Type.values()) {
                if (s.getValue().equals(name)) {
                    return s.getUrlPre();
                }
            }
            return "";
        }
    }


    @EntityField(name = "名称", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "处理器", type = ApiParamType.STRING)
    private String handler;
    @EntityField(name = "处理器名", type = ApiParamType.STRING)
    private String handlerName;
    @EntityField(name = "配置信息，json格式", type = ApiParamType.JSONOBJECT)
    private String config;
    @EntityField(name = "token", type = ApiParamType.STRING)
    private String token;
    @EntityField(name = "地址", type = ApiParamType.STRING)
    private String url;
    @EntityField(name = "帮助地址", type = ApiParamType.STRING)
    private String helpUrl;
    @EntityField(name = "描述", type = ApiParamType.STRING)
    private String description;
    @EntityField(name = "用户名", type = ApiParamType.STRING)
    private String username;
    @EntityField(name = "密码", type = ApiParamType.STRING)
    private String password;

    @EntityField(name = "接口类型", type = ApiParamType.STRING)
    private String type;
    @EntityField(name = "接口类型名称", type = ApiParamType.STRING)
    private String typeText;
    @EntityField(name = "接口数据类型，stream,rest,binary", type = ApiParamType.STRING)
    private String dataType;
    @EntityField(name = "访问频率", type = ApiParamType.INTEGER)
    private Integer qps = 0;
    @EntityField(name = "功能ID(从token中截取第一个单词而来)", type = ApiParamType.STRING)
    private String funcId;
    @JSONField(serialize = false)
    private transient JSONObject pathVariableObj;
    @JSONField(serialize = false)
    private transient List<String> pathVariableList;
    @JSONField(serialize = false)
    private transient String keyword;
    @JSONField(serialize = false)
    private transient List<String> tokenList;
    //	private Long totalDataSize = 0l;
//	private String totalDataSizeText;
    @JSONField(serialize = false)
    private String authorization;


    public void addPathVariable(String para) {
        if (pathVariableList == null) {
            pathVariableList = new ArrayList<>();
        }
        pathVariableList.add(para);
    }


    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<String> getTokenList() {
        return tokenList;
    }

    public void setTokenList(List<String> tokenList) {
        this.tokenList = tokenList;
    }

    public String getTypeText() {
        if (getType() != null) {
            typeText = Type.getText(type);
        }
        return typeText;
    }

    public void setTypeText(String typeText) {
        this.typeText = typeText;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }


    public Integer getQps() {
        return qps;
    }

    public void setQps(Integer qps) {
        this.qps = qps;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((handler == null) ? 0 : handler.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((qps == null) ? 0 : qps.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ApiVo other = (ApiVo) obj;
        if (config == null) {
            if (other.config != null) {
                return false;
            }
        } else if (!config.equals(other.config)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (handler == null) {
            if (other.handler != null) {
                return false;
            }
        } else if (!handler.equals(other.handler)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        if (qps == null) {
            if (other.qps != null) {
                return false;
            }
        } else if (!qps.equals(other.qps)) {
            return false;
        }
        if (token == null) {
            if (other.token != null) {
                return false;
            }
        } else if (!token.equals(other.token)) {
            return false;
        }
        if (username == null) {
            if (other.username != null) {
                return false;
            }
        } else if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }

    public JSONObject getPathVariableObj() {
        return pathVariableObj;
    }

    public void setPathVariableObj(JSONObject pathVariableObj) {
        this.pathVariableObj = pathVariableObj;
    }

    public List<String> getPathVariableList() {
        return pathVariableList;
    }

    public void setPathVariableList(List<String> pathVariableList) {
        this.pathVariableList = pathVariableList;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }


    public String getUrl() {
        if (StringUtils.isBlank(url) && StringUtils.isNotBlank(type) && StringUtils.isNotBlank(token)) {
            url = "api/" + Type.getUrlPre(type) + token;
        }
        return url;
    }

    public String getHelpUrl() {
        if (StringUtils.isBlank(helpUrl) && StringUtils.isNotBlank(type) && StringUtils.isNotBlank(token)) {
            helpUrl = "api/help/" + Type.getUrlPre(type) + token;

        }
        return helpUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
