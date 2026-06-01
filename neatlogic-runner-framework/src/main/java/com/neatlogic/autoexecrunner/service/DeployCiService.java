package com.neatlogic.autoexecrunner.service;

import java.io.UnsupportedEncodingException;

public interface DeployCiService {

    /**
     * 获取gitlab token
     *
     * @param repoServerAddress 仓库服务器地址
     * @param authMode          认证方式
     * @param username          gitlab用户名
     * @param password          gitlab密码
     * @return token
     */
    String getGitlabToken(String repoServerAddress, String authMode, String username, String password);

    /**
     * 获取gitlab webhook api
     *
     * @param hookId            webhook id
     * @param repoServerAddress 仓库服务器地址
     * @param repoName          仓库名
     * @param authMode          认证方式
     * @param token             token
     * @return url
     */
    String getGitlabHookApiUrl(String hookId, String repoServerAddress, String repoName, String authMode, String token) throws UnsupportedEncodingException;
}
