package com.techsure.autoexecrunner.codehub.utils;

import com.techsure.autoexecrunner.codehub.constvalue.RepoType;
import com.techsure.autoexecrunner.common.config.Config;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * 
 * @ClassName   CacheUtils   
 * @Description TODO
 * @author      zouye
 * @date        2021-01-07   
 *
 */
public class CacheUtils {

	public static String INVALID_CHARS = "\\/:*?\"<>|.";
	
	public static String readCache(boolean forceFlush, String repoServiceUuid, String repoUuid, String srcBranch, String targetBranch) {
		if (forceFlush) {
			return null;
		} else {
			String cachePath = getCachePath(repoServiceUuid, repoUuid, srcBranch, targetBranch);			
			File cacheFilePath = new File(cachePath);
			if (cacheFilePath.exists()) {
				File headFile = new File(cachePath + "/HEAD");
				if (!headFile.exists()) {
					return null;
				}
				
				
			}
			
			return null;
		}
	}
	
	
	public static String getCommitsFromCache(String repoServiceUuid, String repoUuid, String repoType, 
			String srcBranch, String targetBranch, String srcCommitId, String mergeBase) {
		
		String cachePath = getCachePath(repoServiceUuid, repoUuid, srcBranch, targetBranch);
		
		// SVN 需求型
		if (RepoType.SVN.myEquals(repoType)) {
			File commitPath = new File(cachePath + "/commit");
			if (!commitPath.exists()) {
				return null;
			}
			
			String[] fileNames = commitPath.list();
			if (fileNames == null || fileNames.length == 0) {
				return null;
			}
			
			List<Long> commitIdList = new ArrayList<>();
			for (String commitId: fileNames) {
				commitIdList.add(Long.parseLong(commitId));
			}
			
			Collections.sort(commitIdList);
			
		} else {
			String commitPath = String.format("%s/commit/%s/%s.%s", cachePath, srcCommitId.substring(0, 1), srcCommitId, mergeBase);
		}
		
		return null;
	}
	
	public static String getCachePath(String repoServiceUuid, String repoUuid, String srcBranch, String targetBranch) {
		return String.format("%s/%s/.cache/%s/%s.%s",
			Config.WORKING_COPY_PATH,
			repoServiceUuid,
			repoUuid,
			StringUtils.replaceChars(srcBranch, INVALID_CHARS, StringUtils.repeat("_", INVALID_CHARS.length())),
			StringUtils.replaceChars(targetBranch, INVALID_CHARS, StringUtils.repeat("_", INVALID_CHARS.length())));
	}
	
	public static String getCommitCachePath(String repoServiceUuid, String repoUuid, String repoType,
			String srcBranch, String targetBranch, String srcCommitId, String mergeBase) {
		
		if (RepoType.SVN.myEquals(repoType)) {
			
		}
		return null;
	}
}
