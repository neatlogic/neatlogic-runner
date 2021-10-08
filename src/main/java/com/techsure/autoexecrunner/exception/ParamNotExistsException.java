package com.techsure.autoexecrunner.exception;


import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class ParamNotExistsException extends ApiRuntimeException {
	private static final long serialVersionUID = 9091220382590565470L;

	public ParamNotExistsException(String msg) {
		super(msg);
	}
}
