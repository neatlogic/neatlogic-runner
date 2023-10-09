package com.neatlogic.autoexecrunner.startup;

public interface IStartUp {

    /**
     * 唯一标识
     */
    String getName();

    /**
     * 描述
     */
    String getDescription();

    /*
     * 需要执行的逻辑
     */
    void doService();
}
