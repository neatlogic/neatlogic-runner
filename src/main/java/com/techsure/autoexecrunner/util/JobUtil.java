package com.techsure.autoexecrunner.util;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * @author lvzk
 * @since 2021/4/21 17:22
 **/
public class JobUtil {
    /**
     * 递归截取3位jobId作为path
     *
     * @param jobId     作业id
     * @param jobPathSb 根据作业id生产的path
     * @return 作业path
     */
    public static String getJobPath(String jobId, StringBuilder jobPathSb) {
        if (jobPathSb.length() > 0) {
            jobPathSb.append(File.separator);
        }
        if (jobId.length() > 3) {
            String tmp = jobId.substring(0, 3);
            jobId = jobId.replaceFirst(tmp, StringUtils.EMPTY);
            jobPathSb.append(tmp);
            getJobPath(jobId, jobPathSb);
        } else {
            jobPathSb.append(jobId);
        }
        return jobPathSb.toString();
    }

}
