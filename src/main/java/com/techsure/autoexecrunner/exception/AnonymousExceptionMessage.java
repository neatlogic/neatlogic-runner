package com.techsure.autoexecrunner.exception;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

public class AnonymousExceptionMessage extends ApiRuntimeException {

	/** 
	* @Fields serialVersionUID : TODO 
	*/

	public AnonymousExceptionMessage() {
		super("不允许匿名访问");
	}

}
