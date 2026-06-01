package com.neatlogic.autoexecrunner.exception;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class AnonymousExceptionMessage extends ApiRuntimeException {

	/** 
	* @Fields serialVersionUID : TODO 
	*/

	public AnonymousExceptionMessage() {
		super("不允许匿名访问");
	}

}
