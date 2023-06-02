package com.techsure.autoexecrunner.codehub.utils;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import com.techsure.autoexecrunner.codehub.dto.diff.FileInfo;
import com.techsure.autoexecrunner.codehub.exception.GitOpsException;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;

import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.common.config.Config;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

public class WorkingCopyUtils {
	private static Logger logger = LoggerFactory.getLogger(WorkingCopyUtils.class);

	/** 如果分支名称中存在如下字符，替换为下划线 */
	public static String INVALID_CHARS = "\\/:*?\"<>|.";

	public static String getWcPath(JSONObject arg) {
		checkWcArgs(arg);

		return Config.WORKING_COPY_PATH + File.separator + arg.getString("repositoryServiceId") + File.separator + arg.getString("repoPath");
	}

	public static String getCachePath(JSONObject arg) {
		checkWcArgs(arg);

		// cache file path: wcpath/repositoryServiceId/.cache/repositoryId/srcBranch
		// e.g. : /app/data/workingcopy/0000b3be43802000/.cache/0000b3be43803000/zouye_src_branch
		return String.format("%s/%s/.cache/%s/%s",
				Config.WORKING_COPY_PATH,
				arg.getString("repositoryServiceId"),
				arg.getString("repositoryId"),
				StringUtils.replaceChars(arg.getString("srcBranch"), INVALID_CHARS, StringUtils.repeat("_", INVALID_CHARS.length())));
	}

	public static String getBranchRealPath(JSONObject jsonObj, String branchName) {
		if (StringUtils.isBlank(branchName)) {
			throw new RuntimeException("请指定分支名称");
		}

		if (jsonObj.getString("repoType").equalsIgnoreCase("svn")) {
			String trunk = jsonObj.getString("mainBranch");
			String branchesPath  = jsonObj.getString("branchesPath");

			if (StringUtils.equals(branchName, trunk)) {
				return getWcPath(jsonObj) + File.separator + branchName;
			}

			if (StringUtils.isNotBlank(branchesPath)) {
				return getWcPath(jsonObj) + File.separator + branchesPath + File.separator + branchName;
			}

			return getWcPath(jsonObj) + File.separator + branchName;
		}

		return getWcPath(jsonObj);
	}

	public static String getRemoteRealUrl(JSONObject jsonObj, String branchName) {
		if (StringUtils.isBlank(branchName)) {
			throw new RuntimeException("请指定分支名称");
		}

		String url = jsonObj.getString("url").trim();
		if (StringUtils.isBlank(url)) {
			throw new RuntimeException("请指定仓库地址");
		}

		if ("svn".equalsIgnoreCase(jsonObj.getString("repoType"))) {
			String trunk = jsonObj.getString("mainBranch");
			String branchesPath  = jsonObj.getString("branchesPath");

			if (StringUtils.equals(branchName, trunk)) {
				return url + "/" + branchName;
			}

			if (StringUtils.isNotBlank(branchesPath)) {
				return url + "/" + branchesPath + "/" + branchName;
			}

			return  url + "/" + branchName;
		}

		return url;
	}

	public static void checkWcArgs(JSONObject jsonObj) {
		String repoType = jsonObj.getString("repoType").trim().toLowerCase();
		String repositoryServiceId = jsonObj.getString("repositoryServiceId");
		String repoPath = jsonObj.getString("repoPath");

		if (StringUtils.isBlank(repoType) || (!"svn".equalsIgnoreCase(repoType) && !"gitlab".equalsIgnoreCase(repoType))) {
			throw new RuntimeException("请指定仓库服务类型repoType, 支持的类型为 svn、gitlab");
		}

		if (StringUtils.isBlank(repositoryServiceId)) {
			throw new RuntimeException("仓库服务repositoryServiceId不能为空");
		}

		if (StringUtils.isBlank(repoPath)) {
			throw new RuntimeException("仓库路径repoPath不能为空");
		}
	}

	/**
	 * 设置SVNWorkingCopy的trunkPath、branchesPath和tagsPath
	 * @param wc
	 * @param jsonObj
	 */
	public static void setSVNWorkingCopyPath(SVNWorkingCopy wc, JSONObject jsonObj) {
		String mainBranch  = jsonObj.getString("mainBranch");
		String branchesPath  = jsonObj.getString("branchesPath");
		String tagsPath  = jsonObj.getString("tagsPath");

		if(!StringUtils.equals(mainBranch, "trunk")){
			wc.setTrunkPath(mainBranch);
		}
		if(!StringUtils.equals(branchesPath, "branches")){
			wc.setBranchesPath(branchesPath);
		}
		if(!StringUtils.equals(tagsPath, "tags")){
			wc.setTagsPath(tagsPath);
		}
	}

	/**
	 * 主要用于执行原生命令
	 * 返回json status:success/failed result存放返回的信息
	 * @param workPath	working copy
	 * @param command
	 * @return
	 */
	public static JSONObject executeCommand(String workPath, String... command) {
		JSONObject jsonObj = new JSONObject();

		ProcessBuilder builder;
		Process proc;

		try {
			builder = new ProcessBuilder(command);
			builder.directory(new File(workPath));
			// 打印最终的命令行日志以便后续的排除错误
			logger.info(StringUtils.join(command, " ").replaceFirst("--password\\s+.+?(\\s+?|$)", ""));
			proc = builder.start();
			
			InputStream is = proc.getInputStream();
			
			byte[] buf = IOUtils.toByteArray(is);
			Charset charset = detectCharset(buf, Config.RES_POSSIBLY_CHARSETS);
			String data = new String(buf, charset);

			proc.waitFor();
			int exitStatus = proc.exitValue();
			if (exitStatus == 0) {
				jsonObj.put("status", "success");
			} else {
				buf = IOUtils.toByteArray(proc.getErrorStream());
				charset = detectCharset(buf, Config.RES_POSSIBLY_CHARSETS);
				String dataError = new String(buf, charset);
				data = StringUtils.isBlank(data) ? dataError : data + "\n" + dataError;
				jsonObj.put("status", "failed");
			}
			data = data.trim();
			jsonObj.put("result", data);
			
		} catch (Exception e) {
			jsonObj.put("status", "failed");
			jsonObj.put("result", e.getMessage());
			logger.error(e.getMessage(), e);
		}

		return jsonObj;

	}


	public static void setChangeInfo(CommitInfo commitInfo, List<FileDiffInfo> diffInfoList) {
		if (CollectionUtils.isNotEmpty(diffInfoList)) {
			int insertedCount = 0;
			int deletedCount = 0;
			int fileAddCount = 0;
			int fileDeleteCount = 0;
			int fileModifyCount = 0;

			for (FileDiffInfo fileDiffInfo : diffInfoList) {
				insertedCount += fileDiffInfo.getInsertedCount();
				deletedCount += fileDiffInfo.getDeletedCount();
				if (fileDiffInfo.getModifiedType() == 'A') {
					fileAddCount++;
				} else if (fileDiffInfo.getModifiedType() == 'D') {
					fileDeleteCount++;
				}  else if (fileDiffInfo.getModifiedType() == 'M') {
					fileModifyCount++;
				} else if (fileDiffInfo.getModifiedType() != 'U') {
					// 重名就当做是新增文件+删除文件
					fileAddCount++;
					fileDeleteCount++;
				}
			}
			commitInfo.setFileAddCount(fileAddCount);
			commitInfo.setFileDeleteCount(fileDeleteCount);
			commitInfo.setFileModifyCount(fileModifyCount);
			commitInfo.setLineAddCount(insertedCount);
			commitInfo.setLineDeleteCount(deletedCount);
		}
	}
	
	
    public static Charset detectCharset(byte[] buf, List<String> charsets) {
		if (buf == null || buf.length == 0) {
			return StandardCharsets.UTF_8;
		}
		Charset charset = null;
		for (String charsetName : charsets) {
			charset = Charset.forName(charsetName);
			CharsetDecoder decoder = charset.newDecoder();	
			decoder.reset();
			try {
				decoder.decode(ByteBuffer.wrap(buf));
				logger.info(charsetName);
				break;
			} catch (CharacterCodingException ignored) {
				
			}
		}
        if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
        return charset;
    }
	
	/**
	 * 根据 commitInfo 中的文件变更情况，计算并设置文件的添加、删除、修改数量，行的添加、删除数量
	 */
	public static void setChangeInfo(List<CommitInfo> commitList) {
		if (CollectionUtils.isEmpty(commitList)) {
			return;
		}
		
		for (CommitInfo commit: commitList) {
			setChangeInfo(commit, commit.getDiffInfoList());
		}
	}
	
	/** 先按文件类型排序，目录在前，文件在后；再按文件名称降序排序 */
	public static void fileAndPathSort(List<FileInfo> fileInfoList, boolean onlyPath) {
		if (CollectionUtils.isNotEmpty(fileInfoList)) {
			// 先按文件类型排序，目录在前，文件在后；再按文件名称降序排序
			Collections.sort(fileInfoList, new Comparator<FileInfo>() {
				@Override
				public int compare(FileInfo o1, FileInfo o2) {
					char type1 = o1.getType();
					char type2 = o2.getType();
					
					if (type1 == type2) {
						return o1.getPath().compareToIgnoreCase(o2.getPath());
					} else {
						return type1 - type2;
					}
				}
			});
			
			Iterator<FileInfo> it = fileInfoList.iterator();
			while(it.hasNext()) {
				FileInfo fileInfo = it.next();
					
				if (fileInfo.getType() == 'F') {
					if (onlyPath) {
						it.remove();
					} else {
						String contentType = Config.FILE_MIME_TYPE_MAP.getContentType(fileInfo.getPath());
					    fileInfo.setBinary(!StringUtils.startsWith(contentType, "text/"));
					}
				}
			}
		}
	}

	/**
	 * 解析comment中的需求号, 一个commit可能包含多个需求
	 * @param comment 
	 * @return
	 */ 
	public static Set<String> parseCommentIssueNo(String comment) {
		Matcher matcher = null;
		// TODO 需求不区分大小写
//		Set<String> issueNoList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		Set<String> issueNoList = new HashSet<>();
		if (Config.MULTIPLE_ISSUE_PATTERN != null && !StringUtils.isEmpty(comment)) {
			matcher = Config.MULTIPLE_ISSUE_PATTERN.matcher(comment);
			while(matcher.find()) {
				String issueNo = matcher.group(1);
				issueNoList.add(issueNo);
				
				comment = StringUtils.substringAfter(comment, matcher.group(0));
				matcher = Config.MULTIPLE_ISSUE_PATTERN.matcher(comment);
			}
		}

		return issueNoList;
	}

	/** 
	 * 解析提交log中的需求号，将 list 格式的 commit 列表转化为  issueNo: commitList 格式的 map。<br>
	 **/
	public static Map<String, List<CommitInfo>> mapIssueCommit(List<CommitInfo> commitList) {
		Map<String, List<CommitInfo>> issueCommitMap = new HashMap<>();
		if (CollectionUtils.isEmpty(commitList)) {
			return issueCommitMap;
		}
		
		for (int i = commitList.size() - 1; i >= 0; i--) {
			CommitInfo commitInfo = commitList.get(i);

			String message = StringUtils.isBlank(commitInfo.getComment()) ? "" : commitInfo.getComment();
			Set<String> issueNoSet = WorkingCopyUtils.parseCommentIssueNo(message);

			// 未关联需求的，设置为空(分支型)
			if (CollectionUtils.isEmpty(issueNoSet)) {
				issueNoSet.add("");
			}
			
			for (String issueNo : issueNoSet) {
				/** 一个commitVo只关联一个需求，方便上层处理，如果确实有关联多个的情况，就生成多个commitVo，每个commitVo分别关联不同issueNo */
				CommitInfo issueCommitInfo = new CommitInfo();
				BeanUtils.copyProperties(commitInfo, issueCommitInfo);
				issueCommitInfo.setIssueNo(issueNo);

				if (issueCommitMap.get(issueNo) == null) {
					issueCommitMap.put(issueNo, new ArrayList<>());
				}
				
				issueCommitMap.get(issueNo).add(commitInfo);
			}

		}
		
		return issueCommitMap;
	}

	public static JSONObject getGitCommitDetail(GitWorkingCopy wc, String srcStartCommit, String targetStartCommit) throws GitOpsException {
		List<String> commitIdList = wc.gitRevListCommand(String.format("%s..%s", targetStartCommit, srcStartCommit));
		JSONArray retList = new JSONArray();
		Map<String, List<CommitInfo>> issueMap = new HashMap<>();
		List<String> issueNoList = new ArrayList<>();
		
		if (commitIdList != null) {

			// 采用的是逆序, 保证获取的需求顺序
			for (int i = commitIdList.size() - 1; i >= 0; i--) {
				String commitId = commitIdList.get(i);

				// 取到detail详情
				CommitInfo commitInfo = wc.getCommitDetail(commitId);
				List<FileDiffInfo> diffInfoList = commitInfo.getDiffInfoList();
				WorkingCopyUtils.setChangeInfo(commitInfo, diffInfoList);

				String message = StringUtils.isBlank(commitInfo.getComment()) ? "" : commitInfo.getComment();

				Set<String> issueNoSet = WorkingCopyUtils.parseCommentIssueNo(message);

				if (CollectionUtils.isEmpty(issueNoSet)) {
					issueNoSet.add("");
				}
				for (String issueNo : issueNoSet) {
					/** 一个commitVo只关联一个需求，方便上层处理，如果确实有关联多个的情况，就生成多个commitVo，每个commitVo分别关联不同issueNo */
					CommitInfo issueCommitInfo = new CommitInfo();
					BeanUtils.copyProperties(commitInfo, issueCommitInfo);
					issueCommitInfo.setIssueNo(issueNo);

					if (issueMap.get(issueNo) == null) {
						issueMap.put(issueNo, new ArrayList<>());
						if (StringUtils.isNotBlank(issueNo)) {
							issueNoList.add(issueNo);
						}
					}
					issueMap.get(issueNo).add(commitInfo);
				}

			}
		}
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("issueNoList", issueNoList);

		if (!issueMap.isEmpty()) {
			for (String issueNo : issueMap.keySet()) {
				JSONObject issueObj = new JSONObject();
				issueObj.put("issueNo", issueNo);
				issueObj.put("commitList", issueMap.get(issueNo));
				retList.add(issueObj);
			}
		}
		jsonObject.put("issueCommitList", retList);
		return jsonObject;
	}
}
