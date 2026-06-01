package com.neatlogic.autoexecrunner.constvalue;

/**
 * @author lvzk
 * @since 2022/6/6 15:40
 **/
public enum FileLogType {
    ERROR("ERROR", "错误"),
    INFO("INFO", "提示"),
    WARN("WARN", "警告"),
    FINE("FINE", "完结");
    private final String value;
    private final String text;

    FileLogType(String _value, String _text) {
        this.value = _value;
        this.text = _text;
    }

    public String getValue() {
        return this.value;
    }

    public String getText() {
        return this.text;
    }
}
