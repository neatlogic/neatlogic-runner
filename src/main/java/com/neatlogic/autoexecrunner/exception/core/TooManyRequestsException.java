package com.neatlogic.autoexecrunner.exception.core;

public class TooManyRequestsException extends RuntimeException {
	private static final long serialVersionUID = 9206337410118158624L;
	private String errorCode;

	public TooManyRequestsException() {
		super();
	}

	public TooManyRequestsException(Throwable ex) {
		super(ex);
	}

	public TooManyRequestsException(String message, Throwable ex) {
		super(message, ex);
	}

	public TooManyRequestsException(String message) {
		super(message);
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

}
