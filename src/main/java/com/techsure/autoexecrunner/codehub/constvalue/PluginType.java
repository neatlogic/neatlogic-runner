package com.techsure.autoexecrunner.codehub.constvalue;


public enum PluginType {
	OBJECT("object", "对象模式"), STREAM("stream", "json流模式"), BINARY("binary", "字节流模式");

	private String name;
	private String text;

	private PluginType(String _name, String _text) {
		this.name = _name;
		this.text = _text;
	}

	public String getValue() {
		return name;
	}

	public String getText() {
		return text;
	}

	public static String getText(String name) {
		for (PluginType s : PluginType.values()) {
			if (s.getValue().equals(name)) {
				return s.getText();
			}
		}
		return "";
	}
}
