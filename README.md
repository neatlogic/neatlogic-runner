中文 / [English](README.en.md)


---

## 关于

neatlogic-runner(执行器)
是一个纯后端应用,部署上主要是为了解决跨网段安全策略问题,功能上用于代理执行自动化本地或远程作业任务场景(
包括[neatlogic-tagent](../../../neatlogic-tagent/blob/develop3.0.0/README.md)
注册、日志查询,以及衔接代理[neatlogic-autoexec](../../../neatlogic-autoexec/blob/develop3.0.0/README.md)
和[neatlogic-autoexec-backend](../../../neatlogic-autoexec-backend/blob/master/README.MD)逻辑)<br>
实现自动化作业、巡检、发布、自动发现采集数据存放在[neatlogic-cmdb](../../../neatlogic-cmdb/blob/develop3.0.0/README.md)
，配合[neatlogic-itsm](../../../neatlogic-itsm/blob/develop3.0.0/README.md)支持在流程中实现流程自动化

## 架构图

采用springboot框架搭建独立应用<br>
自动化作业过程:[neatlogic-autoexec](../../../neatlogic-autoexec/blob/develop3.0.0/README.md)(应有服务集群)->
neatlogic-runner(执行器)+[neatlogic-autoexec-backend](../../../neatlogic-autoexec-backend/blob/master/README.MD)->
通过对应协议到目标(linux、windows等)执行相应脚本命令
![img9.png](README_IMAGES/img9.png)

## 安装教程

需整体一键构建和部署，请参考[neatlogic-itom-all](../../../neatlogic-itom-all/blob/develop3.0.0/README.md)
### ❗❗❗ 注意
```js
 ❗❗❗"仅支持在mac或linux下开发部署"   
```

## 配置参数

目前仅讲解docker部署方式,其他方式后续补充:
进入docker的/app/systems/neatlogic-runner/config 目录,编辑application.properties

```
#SERVER
#应用名
spring.application.name=autoexecrunner
server.port=8084
server.servlet.context-path=/${spring.application.name}

# SPRING AOP CONFIG
spring.aop.auto=true
spring.aop.proxy-target-class=true

# UPLOAD FILE CONFIG
#上传限制
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-request-size=100MB
spring.servlet.multipart.max-file-size=100MB

#NEATLOGIC WEB
#neatlogic后端应用地址
neatlogic.root=http://127.0.0.1:8080/neatlogic
#认证 连接时校验
auth.type=basic
access.key=neatlogic
access.secret=x15wDEzSbBL6tV1W


#RUNNER
#runner根路径
runner.home=/Users/cocokong/autoexec-runner

#LOGGER
#日志级别
logging.config=${runner.home}/config/logback-spring.xml
logging.home=${runner.home}/logs/autoexec-runner
logging.level.root=DEBUG

#NEATLOGIC-AUTOEXEC-BACKEND
#neatlogic--autoexec-backend自动化作业数据路径
autoexec.home=/Users/cocokong/IdeaProjects/autoexec/data/job

#DEPLOY
#neatlogic--autoexec-backend发布版本数据路径
deploy.home=/app/autoexec/data/verdata
 
#neatlogic--autoexec-backend数据根路径
data.home=${runner.home}/data

#tagent 安装包下载目录，将文件放在这个目录，就可以通过“http://ip:8084/autoexecrunner/tagent/download/” +文件名下载文件
tagent.download.path=/app/autoexec/data/tagent/
```

## 技术交流

[点击交流 Neatlogic in Slack](https://join.slack.com/t/neatlogichome/shared_invite/zt-1w037axf8-r_i2y4pPQ1Z8FxOkAbb64w)