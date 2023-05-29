[中文](README.md) / English
<p align="left">
    <a href="https://opensource.org/licenses/Apache-2.0" alt="License">
        <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" /></a>
<a target="_blank" href="https://join.slack.com/t/neatlogichome/shared_invite/zt-1w037axf8-r_i2y4pPQ1Z8FxOkAbb64w">
<img src="https://img.shields.io/badge/Slack-Neatlogic-orange" /></a>
</p>

---

## about

neatlogic-runner (executor) is a pure back-end application. It is mainly deployed to solve cross-network segment
security policy issues. It is used to act as an agent to execute automated local or remote job scenarios (
including [neatlogic-tagent](../../../neatlogic-tagent/blob/develop3.0.0/README.md) registration, log query, and
connection agent [neatlogic-autoexec](../../../neatlogic-autoexec/blob/develop3.0.0/README.md)
and [neatlogic-autoexec-backend](../../../neatlogic-autoexec-backend/blob/master/README.MD)logic)<br>
Realize automatic operation, inspection, release, automatic discovery and collect data stored
in [neatlogic-cmdb](../../../neatlogic-cmdb/blob/develop3.0.0/README.md), cooperate
with [neatlogic-itsm ](../../../neatlogic-itsm/blob/develop3.0.0/README.md) supports process automation in processes

## Architecture Diagram

Use the springboot framework to build an independent application<br>
Automated job process: [neatlogic-autoexec](../../../neatlogic-autoexec/blob/develop3.0.0/README.md) (should have a
service cluster)->neatlogic-runner (
executor)+[ neatlogic-autoexec-backend](../../../neatlogic-autoexec-backend/blob/master/README.MD) -> Execute the
corresponding script command to the target (linux, windows, etc.) through the corresponding protocol
![img9.png](README_IMAGES/img9.png)

## Installation Tutorial

One-click build and deployment is required, please refer
to [neatlogic-itom-all](../../../neatlogic-itom-all/blob/develop3.0.0/README.md)

## configuration parameters

At present, only the docker deployment method is explained, and other methods will be added later:
Enter docker's /app/systems/autoexec-runner/config directory, edit application.properties

```
#SERVER
#application name
## about 
neatlogic-runner (executor) is a pure back-end application. It is mainly deployed to solve cross-network segment security policy issues. It is used to act as an agent to execute automated local or remote job scenarios (including [neatlogic-tagent](../../../neatlogic-tagent/blob/develop3.0.0/README.md) registration, log query, and connection agent [neatlogic-autoexec](../../../neatlogic-autoexec/blob/develop3.0.0/README.md) and [neatlogic-autoexec-backend](../../../neatlogic-autoexec-backend/blob/master/README.MD)logic)<br>
  Realize automatic operation, inspection, release, automatic discovery and collect data stored in [neatlogic-cmdb](../../../neatlogic-cmdb/blob/develop3.0.0/README.md), cooperate with [neatlogic-itsm ](../../../neatlogic-itsm/blob/develop3.0.0/README.md) supports process automation in processes

## Architecture Diagram
Use the springboot framework to build an independent application<br>
Automated job process: [neatlogic-autoexec](../../../neatlogic-autoexec/blob/develop3.0.0/README.md) (should have a service cluster)->neatlogic-runner (executor)+[ neatlogic-autoexec-backend](../../../neatlogic-autoexec-backend/blob/master/README.MD) -> Execute the corresponding script command to the target (linux, windows, etc.) through the corresponding protocol
![img9.png](README_IMAGES/img9.png)


## Installation Tutorial
One-click build and deployment is required, please refer to [neatlogic-itom-all](../../../neatlogic-itom-all/blob/develop3.0.0/README.md)


## configuration parameters
At present, only the docker deployment method is explained, and other methods will be added later:
Enter docker's /app/systems/autoexec-runner/config directory, edit application.properties
```

#SERVER
#application name
spring.application.name=autoexecrunner
server.port=8084
server.servlet.context-path=/${spring.application.name}

# SPRING AOP CONFIG

spring.aop.auto=true
spring.aop.proxy-target-class=true

# UPLOAD FILE CONFIG

# upload limit

spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-request-size=100MB
spring.servlet.multipart.max-file-size=100MB

#NEATLOGIC WEB
#neatlogic backend application address
neatlogic.root=http://127.0.0.1:8080/neatlogic
#authentication check when connecting
auth.type=basic
access.key=techsure
access.secret=x15wDEzSbBL6tV1W

#RUNNER
#runner root path
runner.home=/Users/cocokong/autoexec-runner

#LOGGER

# log level

logging.config=${runner.home}/config/logback-spring.xml
logging.home=${runner.home}/logs/autoexec-runner
logging.level.root=DEBUG

#NEATLOGIC-AUTOEXEC-BACKEND
#neatlogic--autoexec-backend automation job data path
autoexec.home=/Users/cocokong/IdeaProjects/autoexec/data/job

#DEPLOY
#neatlogic--autoexec-backend release version data path
deploy.home=/app/autoexec/data/verdata

#neatlogic--autoexec-backend data root path
data.home=${runner.home}/data

```
\ No newline at end of file
```

## Contact us

[Neatlogic in Slack](https://join.slack.com/t/neatlogichome/shared_invite/zt-1w037axf8-r_i2y4pPQ1Z8FxOkAbb64w)