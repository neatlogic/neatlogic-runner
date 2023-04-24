package com.techsure.autoexecrunner.constvalue;

public enum FileType {
    FILE("f", "文件"),
    DIRECTORY("d", "目录"),
    LINK("l", "链接");
    private final String value;
    private final String text;

    FileType(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

}
