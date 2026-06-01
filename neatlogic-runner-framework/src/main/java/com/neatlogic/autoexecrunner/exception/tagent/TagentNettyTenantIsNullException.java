package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

/**
 * @author longrf
 * @date 2022/5/26 12:05 下午
 */
public class TagentNettyTenantIsNullException extends ApiRuntimeException {
    public TagentNettyTenantIsNullException() {
        super("Tenant is blank in the file 'tagent.conf' ");
    }
}
