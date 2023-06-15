package com.neatlogic.autoexecrunner.codehub.git;

import com.neatlogic.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.neatlogic.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import com.neatlogic.autoexecrunner.codehub.exception.GitOpsException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;

import java.io.IOException;
import java.util.List;

public class RepoTester {
	private static String reposRootPath = "D:\\study\\codehub";
	//private static String reposRootPath = "/Users/wenhb/codehub-repos/git";
	
	private static String repoName = "test-codehub";
	private static String repoLocalDir = reposRootPath + repoName;
	
	private static String remoteUrl = "http://192.168.0.82:7070/zouye/test-codehub.git";
	private static String user = "deploytest";
	private static String password = "y9O7Y6p8jsswNGrD";
	
	
	
	public static void main(String[] args) throws GitOpsException, RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException  {
		GitWorkingCopy wcGit = new GitWorkingCopy(repoLocalDir, remoteUrl, user, password);
		//wcGit.resetAndUpdate();
		//wcGit.checkout("develope");
		
		GitRepository gitRepo = new GitRepository(repoLocalDir);

		//wcGit.createBranch("test-branch", "master");
		//wcGit.deleteBranch("test-branch");
		List<CommitInfo> commits = gitRepo.getCommitsForBranch("release", "develop2.0.0", null, 0, 0, true);
		System.out.println(commits);

		System.out.println("Remote branches==============");
		List<String> branches = wcGit.getRemoteBranchList();
		for(String branch:branches) {
			System.out.println(branch);
		}

		//wcGit.createTag("test-tag", "master");
		//wcGit.deleteTag("test-tag");
		System.out.println("Remote tages==============");
		List<String> tags = wcGit.getRemoteTagList();
		for(String tag:tags) {
			System.out.println(tag);
		}
		
		long startTime = 0;
		List<CommitInfo> commitInfoList = null;
		
		
		System.out.println("Commit logs==============");
		startTime = System.currentTimeMillis();
		commitInfoList = wcGit.getCommits(null, 3, 0, false);
		for(CommitInfo commitInfo: commitInfoList) {
			System.out.println(commitInfo);
		}
		System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");
		
		
		/*
		System.out.println("Branch mainline commit logs============");
		startTime = System.currentTimeMillis();
		commitInfoList = wcGit.getMainLogsForBranch("2.0.0", System.currentTimeMillis()/1000 - (long) (86400L*365*0.5));
		for(CommitInfo commitInfo: commitInfoList) {
			System.out.println(commitInfo);
		}
		System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");
		*/
		
		/*
		System.out.println("Branch mainline commit logs until join integration branch===========");
		startTime = System.currentTimeMillis();
		commitInfoList = wcGit.getMainLogsForBranch("test2", "devtest");
		for(CommitInfo commitInfo: commitInfoList) {
			System.out.println(commitInfo);
		}
		System.out.println("\n====Consume time:" + (System.currentTimeMillis()-startTime) + "\n");
		*/
		
		/*
		System.out.println("List folder ==============");
		List<FileInfo> fileInfoList = wcGit.listFolder("refs/remotes/origin/develope", "src/main/webapp");
		for(FileInfo fileInfo:fileInfoList) {
			System.out.println(fileInfo);
		}
		*/
		
		System.out.println("Get diff info ==============");
		List<FileDiffInfo> fileDiffInfoList = wcGit.getDiffInfo("refs/remotes/origin/2.0.0", "refs/remotes/origin/1.0.0");
		for(FileDiffInfo diffInfo:fileDiffInfoList) {
			System.out.println(diffInfo);
		}
		
		System.out.println("Get default branch =======");
		System.out.println(gitRepo.getDefaultBranch());
	}
}
