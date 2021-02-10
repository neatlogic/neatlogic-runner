package com.techsure.autoexecproxy.exception;


import com.techsure.autoexecproxy.exception.core.ApiRuntimeException;

public class ParamNotExistsException extends ApiRuntimeException {
	private static final long serialVersionUID = 9091220382590565470L;

	public ParamNotExistsException(String msg) {
		super(msg);
	}
}
