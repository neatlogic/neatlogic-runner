package com.neatlogic.autoexecrunner.dto;

import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.EntityField;

public class FileLineVo {
	@EntityField(name = "行号", type = ApiParamType.INTEGER)
	private Long number;
	@EntityField(name = "时间", type = ApiParamType.STRING)
	private String time;
	@EntityField(name = "类型", type = ApiParamType.STRING)
	private String type;
	@EntityField(name = "内容", type = ApiParamType.STRING)
	private String content;
	@EntityField(name = "锚点", type = ApiParamType.STRING)
	private String anchor;

	public FileLineVo(long filePointer, String time, String lineType, String content,String anchor) {
		this.number = filePointer;
		this.time = time;
		this.type = lineType;
		this.content = content;
		this.anchor = anchor;
	}

	public FileLineVo() {
	}

	public Long getNumber() {
		return number;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getAnchor() {
		return anchor;
	}

	public void setAnchor(String anchor) {
		this.anchor = anchor;
	}
}
