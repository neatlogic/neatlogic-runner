package com.techsure.autoexecrunner.exception.tagent;

public class TagentRunnerConnectRefusedException extends RuntimeException {
    public TagentRunnerConnectRefusedException(String s) {
        super("Runner url： '" + s + "' connect failed");
    }
}
