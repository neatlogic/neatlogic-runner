package com.techsure.autoexecrunner.codehub.svn;

import java.io.IOException;
import java.util.List;

import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.exception.SVNOpsException;
import org.tmatesoft.svn.core.SVNException;

public class RepoTester {
	private static String reposRootPath = "D:/study/svn/codehub/svn";

	private static String repoLocalDir = reposRootPath;

	private static String remoteUrl = "http://1.1.1.1";
	private static String user = "23123213123";
	private static String password = "12321321";
	private static String repoPath = "单证集中管理平台/代码类";


	public static void main(String[] args) throws SVNOpsException, IOException, SVNException {
		SVNWorkingCopy wcSvn = new SVNWorkingCopy(repoLocalDir, remoteUrl, user, password,"","","");

		long startTime = 0;
        startTime = System.currentTimeMillis();
		System.out.println("Get logs start==============");
		String startCommitId = "9825";
		String endCommitId = "9489";
		//endCommitId = null;
        String branch = "fengt_src_branch";
        //branch = "branch_03";
        //branch = "devops测试";
        //branch = "fengt_src_branch2";
        int maxCount = 10;
		List<CommitInfo> commitInfoList  = wcSvn.getBranchCommitListByCommitIdRange(branch,startCommitId,endCommitId,maxCount);
        System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");
		System.out.println("commitInfoList.size():" + commitInfoList.size());
		for(CommitInfo commitInfo: commitInfoList) {
			System.out.println( commitInfo);
		}



		
		/*
		wcSvn.checkout("1.0.0");
		
		//wcSvn.createBranch("2.0.0", "trunk");
		//wcSvn.deleteBranch("2.0.0");
		System.out.println("Remote branches==============");
		List<String> branches = wcSvn.getRemoteBrancheList();
		for(String branch:branches) {
			System.out.println(branch);
		}
		
		//wcSvn.createTag("v2.0.0", "trunk");
		//wcSvn.deleteTag("v2.0.0");
		System.out.println("Remote tags==============");
		List<String> tags = wcSvn.getRemoteTagList();
		for(String tag:tags) {
			System.out.println(tag);
		}
	
		
		System.out.println("Get logs after specified date==============");
		startTime = System.currentTimeMillis();
		commitInfoList = wcSvn.getCommitLogs(new Date(System.currentTimeMillis() - (long) (86400000L*10*0.5)));
		for(CommitInfo commitInfo: commitInfoList) {
			System.out.println(commitInfo);
		}
		System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");
		*/
		
		/*
		System.out.println("Get latest 10 commit==============");
		startTime = System.currentTimeMillis();
		commitInfoList = wcSvn.getLatestCommitLogs(2);
		for(CommitInfo commitInfo: commitInfoList) {
			System.out.println(commitInfo);
		}
		System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");
		*/
/*		
		System.out.println("Get branch logs==============");
		//wcSvn.checkout("branch_1");
		startTime = System.currentTimeMillis();
		commitInfoList = wcSvn.getBranchLogs("branch_1");
		for(CommitInfo commitInfo: commitInfoList) {
			System.out.println(commitInfo);
		}
		System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");
*/
		/*System.out.println("Get tag logs==============");
		startTime = System.currentTimeMillis();
		commitInfoList = wcSvn.getCommitsForTag("tag_4", null, 0, 0, false);
		for(CommitInfo commitInfo: commitInfoList) {
			System.out.println(commitInfo);
		}
		System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");
*/
		/*
		System.out.println("===list folder========");
		wcSvn.locateToBranch("1.0.0");
		List<FileInfo> fileInfoList = wcSvn.listFolder(550, "tagent/src/main/java/com/techsure/tagent/test/");
		for(FileInfo fileInfo:fileInfoList) {
			System.out.println(fileInfo);
		}
		
		System.out.println("===Download dir========");
		wcSvn.locateToBranch("1.0.0");
		FileOutputStream fos = new FileOutputStream("/tmp/test.zip");
		wcSvn.downloadDirArchive(550, "tagent", fos);
		fos.close();
		
		
		System.out.println("===Get file content========");
		wcSvn.locateToBranch("1.0.0");
		String fileContent = wcSvn.getFileContent(550, "tagent/src/main/java/com/techsure/tagent/test/RunResultHandler.java");
		System.out.println(fileContent);
		*/
		
		/*
		System.out.println("===Diff two branch==========");
		List<FileDiffInfo> diffInfoList = wcSvn.getDiffInfo("branches/1.0.0", "trunk");
		
		for(FileDiffInfo diffInfo:diffInfoList) {
			System.out.println(diffInfo);
		}
		*/
		
		/*
		System.out.println("===Merge branch=============");
		startTime = System.currentTimeMillis();
		wcSvn.MergeBranch("1.0.0", "trunk", "merge test");
		System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");
		*/
		/*
		System.out.println("===Merge commits=============");
		startTime = System.currentTimeMillis();
		wcSvn.CherryPick("1.0.0", "trunk", "561", true, "merge test");
		System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");*/
	}
}
