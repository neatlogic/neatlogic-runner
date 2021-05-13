package com.techsure.autoexecproxy.dto;

public class FileTailerVo {
	private Long startPos;
	private Long endPos;
	private Long logPos;
	private String lastLine;
	private String tailContent = "";

	public FileTailerVo() {
	}

	public FileTailerVo(Long _logPos) {
		logPos = _logPos;
		startPos = _logPos;
		endPos = _logPos;
	}

	public Long getStartPos() {
		return startPos;
	}
	public void setStartPos(Long startPos) {
		this.startPos = startPos;
	}
	public Long getEndPos() {
		return endPos;
	}
	public void setEndPos(Long endPos) {
		this.endPos = endPos;
	}
	public Long getLogPos() {
		return logPos;
	}
	public void setLogPos(Long logPos) {
		this.logPos = logPos;
	}
	public String getLastLine() {
		return lastLine;
	}
	public void setLastLine(String lastLine) {
		this.lastLine = lastLine;
	}
	public String getTailContent() {
		return tailContent;
	}
	public void setTailContent(String tailContent) {
		this.tailContent = tailContent;
	}
	
}
