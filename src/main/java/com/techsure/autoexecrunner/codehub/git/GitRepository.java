/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
 * 本地GIT bare仓库的功能库
 */
package com.techsure.autoexecrunner.codehub.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import com.techsure.autoexecrunner.codehub.dto.diff.FileInfo;
import com.techsure.autoexecrunner.codehub.exception.GitOpsException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.ArchiveCommand;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.archive.ZipFormat;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.References;

public class GitRepository {
    private String repoLocalDir = null;

    private Git localGit = null;

    /**
     * 本地仓库构造函数（对应于.git目录或者bare的仓库目录），使用完后需要主动调用close关闭仓库实例，否则会导致资源泄漏
     *
     * @param repoLocalDir Git的working copy的目录
     * @throws GitOpsException 当操作异常会抛出此异常
     */
    public GitRepository(String repoLocalDir) throws GitOpsException {
        this.repoLocalDir = repoLocalDir;

        try {
            File localPath = new File(repoLocalDir);
            if (localPath.exists()) {
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                Repository repo = builder.readEnvironment() // scan environment GIT_* variables
                        .findGitDir(new File(repoLocalDir)) // scan up the file system tree
                        .build();
                localGit = new Git(repo);
            }
        } catch (IOException e) {
            throw new GitOpsException("Open local git directory failed, " + e.getMessage(), e);
        }
    }

    /**
     * 主动关闭打开的仓库实例
     */
    public void close() {
        if (localGit != null) {
            localGit.close();
            localGit = null;
        }
    }

    /**
     * 检查仓库实例是否已经关闭
     *
     * @return true：仓库已经关闭，false：仓库未关闭
     */
    public boolean isClose() {
        if (localGit == null) {
            return true;
        }
        return false;
    }

    /**
     * 获取commit的明细信息，包括commit修改的文件，插入删除的行数，修改的上下文信息
     *
     * @param commitId commit id
     * @param logOnly   true:只获取日志不分析commit修改的文件
     * @return
     * @throws GitOpsException
     */
    public CommitInfo getCommitDetail(String commitId, boolean logOnly) throws GitOpsException {
        RevWalk revWalk = null;
        try {
            Repository repo = localGit.getRepository();
            revWalk = new RevWalk(repo);

            ObjectId commitObjId = repo.resolve(commitId);
            RevCommit commit = revWalk.parseCommit(commitObjId);

            CommitInfo commitInfo = null;
            if (commit.getParentCount() > 0) {
                RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0));
                commitInfo = getChangeInfo(commit, parentCommit, logOnly);
            } else {
                commitInfo = getChangeInfo(commit, commit, logOnly);
            }

            return commitInfo;
        } catch (RevisionSyntaxException | IOException e) {
            throw new GitOpsException("Get commit detail failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
        }
    }

    /**
     * 按照时间倒序获取从startCommitId开始的，最近不超过maxCount，不早于startTime的提交日志
     *
     * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
     * @param maxCount  最多获取的commit数，0:不限制，否则最多获取maxCount条记录
     * @param startTime 最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于startTime
     * @param logOnly   true:只获取日志不分析commit修改的文件
     * @throws GitOpsException 操作异常
     */
    public List<CommitInfo> getCommits(String startCommitId, int maxCount, long startTime, boolean logOnly) throws GitOpsException {
        List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();

        RevWalk revWalk = null;
        try {
            Repository repo = localGit.getRepository();
            revWalk = new RevWalk(repo);

            ObjectId lastCommitId = null;
            boolean needSkip = false;
            if (startCommitId == null) {
                // get a list of all known heads, tags, remotes, ...
                Collection<Ref> allRefs = repo.getRefDatabase().getRefs();
                // a RevWalk allows to walk over commits based on some filtering that is defined
                for (Ref ref : allRefs) {
                    revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
                }

                // System.out.println("Walking all commits starting with " + allRefs.size() + "
                // refs: " + allRefs);
            } else {
                needSkip = true;
                lastCommitId = repo.resolve(startCommitId);
            }

            int count = 0;
            for (RevCommit commit : revWalk) {
                if (needSkip) {
                    continue;
                }

                if (commit.getId().equals(lastCommitId)) {
                    needSkip = false;
                }

                count++;

                commit = revWalk.parseCommit(commit);

                CommitInfo commitInfo = null;
                if (commit.getParentCount() > 0) {
                    RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0));
                    commitInfo = getChangeInfo(commit, parentCommit, logOnly);
                } else {
                    commitInfo = getChangeInfo(commit, commit, logOnly);
                }
                commitInfoList.add(commitInfo);

                if (maxCount > 0 && count >= maxCount) {
                    break;
                }

                if (startTime > 0 && commit.getCommitTime() <= startTime) {
                    break;
                }
            }

            return commitInfoList;
        } catch (IOException e) {
            throw new GitOpsException("Get commit log failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
                revWalk = null;
            }
        }
    }

    /**
     * 用于获取srcBranch相对于dstBranch的change，获取srcBranch从dstBranch分支出来的RevCommit，或者srcBranch并入dstBranch的commit点
     *
     * @param srcBranch 源分支名，准备merge进入dstBranch的分支名
     * @param dstBranch 目标分支名，准备接收srcBranch并入的分支名
     * @return
     * @throws GitOpsException
     */
    private RevCommit getBranchCreateFrom(String srcBranch, String dstBranch) throws GitOpsException {
        Repository repo = localGit.getRepository();
        RevWalk revWalk = null;
        
        try {
            revWalk = new RevWalk(repo);
            ObjectId srcObjectId = repo.resolve("refs/remotes/origin/" + srcBranch);
            ObjectId dstObjectId = repo.resolve("refs/remotes/origin/" + dstBranch);
            if (srcObjectId == null) {
            	throw new RuntimeException(String.format("branch '%s' is not exist.", srcBranch));
            }
            if (dstObjectId == null) {
            	throw new RuntimeException(String.format("branch '%s' is not exist.", dstBranch));
            }
            
            RevCommit srcCommit = revWalk.parseCommit(srcObjectId);
            RevCommit dstCommit = revWalk.parseCommit(dstObjectId);

            RevCommit mergeBase = getMergeBase(srcCommit, dstCommit);
            RevCommit bestMergeBase = null;

            while (mergeBase != null) {
            	bestMergeBase = mergeBase;
            	
                mergeBase = revWalk.parseCommit(mergeBase);
                // 找出mergeBase在源分支的儿子节点，如果这个节点的主父亲节点并不是mergeBase，
                // 那么代表这个mergeBase是并入srcBranch的并入基点，不是我们所要找的基点，继续往下找
                RevCommit mergeBaseChild = findFirstChild(srcCommit, mergeBase);
                if (mergeBaseChild != null) {
                    mergeBaseChild = revWalk.parseCommit(mergeBaseChild);
                    RevCommit childParent = revWalk.parseCommit(mergeBaseChild.getParent(0));
                    if (References.isSameObject(childParent, mergeBase)) {
                        break;
                    } else {
                        srcCommit = childParent;
                        dstCommit = mergeBase;
                    }
                } else {
                    // 如果找到的mergeBase在srcBranch上的儿子节点是空，
                    // 代表srcBranch的第一个节点就是第一个找到的mergeBase，代表srcBranch已经并入了dstBranch
                    break;
                }
                
                mergeBase = getMergeBase(srcCommit, dstCommit);
            }

            return bestMergeBase;
        } catch (Exception e) {
            throw new GitOpsException("Get create commit for branch:" + srcBranch + " from " + dstBranch + " failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
                revWalk = null;
            }
        }
    }
    
    public List<RevCommit> getMergeBaseList(String srcCommit, String dstCommit) throws GitOpsException {
    	if (srcCommit == null || dstCommit == null) {
    		return null;
    	}
    	
    	RevWalk revWalk = null;
        try {
        	Repository repo = localGit.getRepository();
        	revWalk = new RevWalk(repo);
        	RevCommit srcRevCommit = revWalk.parseCommit(repo.resolve(srcCommit));
        	RevCommit dstRevCommit = revWalk.parseCommit(repo.resolve(dstCommit));
        	
            revWalk.setRevFilter(RevFilter.MERGE_BASE);
            // zouye: RevCommits need to be produced by the same RevWalk instance otherwise you can't compare them
            revWalk.markStart(srcRevCommit);
            revWalk.markStart(dstRevCommit);
            
            List<RevCommit> retList = new ArrayList<>();
            RevCommit mergeBase = revWalk.next();
            
            // 可能 merge-base 和目标分支相同
            while (mergeBase != null) {
            	retList.add(mergeBase);
            	
                mergeBase = revWalk.parseCommit(mergeBase);
                // 找出mergeBase在源分支的儿子节点，如果这个节点的主父亲节点并不是mergeBase，
                // 那么代表这个mergeBase是并入srcBranch的并入基点，不是我们所要找的基点，继续往下找
                RevCommit mergeBaseChild = findFirstChild(srcRevCommit, mergeBase);
                if (mergeBaseChild != null) {
                    mergeBaseChild = revWalk.parseCommit(mergeBaseChild);
                    RevCommit childParent = revWalk.parseCommit(mergeBaseChild.getParent(0));
                    if (References.isSameObject(childParent, mergeBase)) {
                        break;
                    } else {
                    	srcRevCommit = childParent;
                    	dstRevCommit = mergeBase;
                    }
                } else {
                    // 如果找到的mergeBase在srcBranch上的儿子节点是空，
                    // 代表srcBranch的第一个节点就是第一个找到的mergeBase，代表srcBranch已经并入了dstBranch
                    break;
                }
                
                mergeBase = getMergeBase(srcRevCommit, dstRevCommit);
            }

            return retList;
        } catch (IOException e) {
            throw new GitOpsException(String.format("Get merge base for: %s, %s failed, %s", srcCommit, dstCommit, e.getMessage()), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
        }
    }
    
    /**
     * 获取两个 commit 的 merge base
     *
     * @param srcCommit 源分 commit 的 commitId/分支名
     * @param dstCommit 目标 commit 的 commitId/分支名
     * @return
     * @throws GitOpsException
     */
    public RevCommit getMergeBase(String srcCommit, String dstCommit) throws GitOpsException {
    	if (srcCommit == null || dstCommit == null) {
    		return null;
    	}
    	
    	RevWalk revWalk = null;
        try {
        	Repository repo = localGit.getRepository();
        	revWalk = new RevWalk(repo);
        	RevCommit srcRevCommit = revWalk.parseCommit(repo.resolve(srcCommit));
        	RevCommit dstRevCommit = revWalk.parseCommit(repo.resolve(dstCommit));
        	
            revWalk.setRevFilter(RevFilter.MERGE_BASE);
            // zouye: RevCommits need to be produced by the same RevWalk instance otherwise you can't compare them
            revWalk.markStart(srcRevCommit);
            revWalk.markStart(dstRevCommit);
            
            RevCommit mergeBase = revWalk.next();
            RevCommit bestMergeBase = mergeBase;
            
            // 可能 merge-base 和目标分支相同
            while (mergeBase != null/* && !References.isSameObject(mergeBase, dstRevCommit)*/) {
            	bestMergeBase = mergeBase;
            	
                mergeBase = revWalk.parseCommit(mergeBase);
                // 找出mergeBase在源分支的儿子节点，如果这个节点的主父亲节点并不是mergeBase，
                // 那么代表这个mergeBase是并入srcBranch的并入基点，不是我们所要找的基点，继续往下找
                RevCommit mergeBaseChild = findFirstChild(srcRevCommit, mergeBase);
                if (mergeBaseChild != null) {
                    mergeBaseChild = revWalk.parseCommit(mergeBaseChild);
                    RevCommit childParent = revWalk.parseCommit(mergeBaseChild.getParent(0));
                    if (References.isSameObject(childParent, mergeBase)) {
                        break;
                    } else {
                    	srcRevCommit = childParent;
                    	dstRevCommit = mergeBase;
                    }
                } else {
                    // 如果找到的mergeBase在srcBranch上的儿子节点是空，
                    // 代表srcBranch的第一个节点就是第一个找到的mergeBase，代表srcBranch已经并入了dstBranch
                    break;
                }
                
                mergeBase = getMergeBase(srcRevCommit, dstRevCommit);
            }

            return bestMergeBase;
        } catch (IOException e) {
            throw new GitOpsException(String.format("Get merge base for: %s, %s failed, %s", srcCommit, dstCommit, e.getMessage()), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
        }
    }

    /**
     * 获取两个分支的Commit的第一个交汇点
     *
     * @param srcCommit 起始commit节点
     * @param dstCommit 另外的起始commit节点
     * @return
     * @throws GitOpsException
     */
    public RevCommit getMergeBase(RevCommit srcCommit, RevCommit dstCommit) throws GitOpsException {
        RevWalk revWalk = null;
        try {
            Repository repo = localGit.getRepository();
            revWalk = new RevWalk(repo);
            revWalk.setRevFilter(RevFilter.MERGE_BASE);
            // zouye: RevCommits need to be produced by the same RevWalk instance otherwise you can't compare them
            revWalk.markStart(revWalk.parseCommit(srcCommit));
            revWalk.markStart(revWalk.parseCommit(dstCommit));
            RevCommit mergeBase = revWalk.next();

            return mergeBase;
        } catch (IOException e) {
            throw new GitOpsException("Get merge base for:" + srcCommit.getName() + " and " + dstCommit.getName() + " failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
        }
    }

    /**
     * 从某个commit开始查找parentCommit的子commit节点，因为Git的commit关系结构是有向无环图，
     * 节点只有父亲节点属性，没有其对应的子节点属性，所以，需要顺序遍历查找。
     *
     * @param startCommit  查找的起始commit节点
     * @param parentCommit 查找目标：parentCommit的子节点
     * @return parentCommit的字节点RevCommit对象，找不到则为null
     * @throws GitOpsException
     */
    public RevCommit findFirstChild(RevCommit startCommit, RevCommit parentCommit) throws GitOpsException {
        RevWalk revWalk = null;
        try {
            Repository repo = localGit.getRepository();
            revWalk = new RevWalk(repo);

            revWalk.setRevFilter(RevFilter.ALL);
            startCommit = revWalk.parseCommit(startCommit);
            parentCommit = revWalk.parseCommit(parentCommit);
            revWalk.markStart(startCommit);

            boolean found = false;
            RevCommit preCommit = startCommit;
            RevCommit commit = null;
            while ((commit = revWalk.next()) != null) {
                if (References.isSameObject(commit, parentCommit)) {
                    found = true;
                    break;
                }

                preCommit = commit;
            }

            if (!found || References.isSameObject(commit, startCommit)) {
                return null;
            } else {
                return preCommit;
            }
        } catch (IOException e) {
            throw new GitOpsException("find child for:" + parentCommit.getName() + " start at: " + startCommit.getName() + " failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
        }
    }

    /**
     * 获取从某个分支从startCommitId开始，最近不超过maxCount，不早于startTime的commit log，注意：对于那些先提交后push的commit会获取不到
     *
     * @param branch    分支名称
     * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
     * @param maxCount  最多获取的commit数，0:不限制，否则最多获取maxCount条记录
     * @param startTime 最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于startTime
     * @param logOnly   true:只获取日志不分析commit修改的文件
     * @throws GitOpsException
     */
    public List<CommitInfo> getCommitsForBranch(String branch, String startCommitId, int maxCount, long startTime, boolean logOnly) throws GitOpsException {
        List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();

        RevWalk revWalk = null;
        try {
            Repository repo = localGit.getRepository();
            revWalk = new RevWalk(repo);
            revWalk.setFirstParent(true);

            if (startCommitId == null) {
                ObjectId commitId = repo.resolve("refs/remotes/origin/" + branch);
                if (commitId == null) {
                    throw new RuntimeException("branch " + branch + " is not exist.");
                }
                revWalk.markStart(revWalk.parseCommit(commitId));
            } else {
                ObjectId lastCommitId = repo.resolve(startCommitId);
                revWalk.markStart(revWalk.parseCommit(lastCommitId));
                revWalk.next();
            }

            int count = 0;
            RevCommit commit = null;
            while ((commit = revWalk.next()) != null) {
                count++;
                commit = revWalk.parseCommit(commit);
                CommitInfo commitInfo = null;
                if (commit.getParentCount() > 0) {
                    RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0));
                    commitInfo = getChangeInfo(commit, parentCommit, logOnly);
                } else {
                    commitInfo = getChangeInfo(commit, commit, logOnly);
                }
                commitInfo.setBranch(branch);
                commitInfoList.add(commitInfo);

                if (maxCount > 0 && count >= maxCount) {
                    break;
                }

                if (startTime > 0 && commit.getCommitTime() <= startTime) {
                    break;
                }
            }

            return commitInfoList;
        } catch (IOException e) {
            throw new GitOpsException("Get branch " + branch + " mainline commit log failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
                revWalk = null;
            }
        }
    }
    
    public List<CommitInfo> getCommitsForMergeByCommitRange(ObjectId srcStartCommit, ObjectId dstStartCommit, int maxCount) throws GitOpsException {
    	List<CommitInfo> retList = new LinkedList<>();
    	if (srcStartCommit == null || dstStartCommit == null) {
    		return retList;
    	}
    	
    	try {
	    	Iterable<RevCommit> log = localGit.log()
	    			.addRange(dstStartCommit, srcStartCommit)
	    			.setMaxCount(maxCount)
	    			.call();
	    	//RevCommit commit = null;
	    	log.forEach(commit ->{
	    		CommitInfo commitInfo = new CommitInfo();
	            commitInfo.setId(commit.getName());
	            commitInfo.setComment(commit.getFullMessage());
	
	            PersonIdent authorIdent = commit.getAuthorIdent();
	            PersonIdent committerIdent = commit.getCommitterIdent();
	
	            commitInfo.setAuthor(authorIdent.getName());
	            commitInfo.setAuthorEmail(authorIdent.getEmailAddress());
	            commitInfo.setAuthorDate(authorIdent.getWhen());
	
	            commitInfo.setCommitter(committerIdent.getName());
	            commitInfo.setCommitterEmail(committerIdent.getEmailAddress());
	            commitInfo.setCommitterDate(committerIdent.getWhen());
	            
	            retList.add(commitInfo);
	    	});
    	}
    	catch (Exception e) {
    		throw new GitOpsException(String.format("Get log since '%s' until '%s' failed, %s", srcStartCommit, srcStartCommit, e.getMessage()), e);
    	}
    	
    	return retList;
    }
    
    public List<CommitInfo> getCommitsForMerge(String srcBranch, String dstBranch, int maxCount, boolean isBranch) throws GitOpsException {
    	List<CommitInfo> retList = new LinkedList<>();
    	if (srcBranch == null || dstBranch == null) {
    		return retList;
    	}
    	
    	try {
	    	Repository repo = localGit.getRepository();
	    	if (isBranch) {
	    		srcBranch = "refs/remotes/origin/" + srcBranch;
	    		dstBranch = "refs/remotes/origin/" + dstBranch;
	    	}
	    	
	    	ObjectId srcStartCommit = repo.resolve(srcBranch);
	    	ObjectId dstStartCommit = repo.resolve(dstBranch);
	    	retList = getCommitsForMergeByCommitRange(srcStartCommit, dstStartCommit, maxCount);
    	}
    	catch (Exception e) {
    		throw new GitOpsException(String.format("Get log since '%s' until '%s' failed, %s", srcBranch, dstBranch, e.getMessage()), e);
    	}
    	
    	return retList;
    }
    
    // TODO: 复杂情况下，数据与 gitlab 对比不一致
    public void getCommitsForMerge(List<CommitInfo> commitInfoList, String startCommitId, List<RevCommit> mergeBaseList, int maxCount, boolean logOnly) throws GitOpsException {
        if (mergeBaseList == null || mergeBaseList.isEmpty() || commitInfoList == null
        		|| commitInfoList.size() >= maxCount) {
        	return;
        }
        CommitInfo commitInfo = new CommitInfo();
        commitInfo.setId(startCommitId);
        if (commitInfoList.contains(commitInfo)) {
        	return;
        }
        
        RevWalk revWalk = null;
        try {
            Repository repo = localGit.getRepository();
            revWalk = new RevWalk(repo);
            revWalk.setFirstParent(true);
            revWalk.markStart(revWalk.parseCommit(repo.resolve(startCommitId)));

            RevCommit commit = null;
            while ((commit = revWalk.next()) != null && !mergeBaseList.contains(commit)) {
                commit = revWalk.parseCommit(commit);
                RevCommit oldCommit = null;
                
                if (commit.getParentCount() >= 1) {
                	oldCommit = commit.getParent(0);
                } else {
                	// init commit
                	oldCommit = commit;
                }
                
                commitInfo = getChangeInfo(commit, oldCommit, logOnly);
                if (commitInfoList.contains(commitInfo)) {
                	break;
                }
                
                commitInfoList.add(commitInfo);
                if (commitInfoList.size() >= maxCount) {
                	break;
                }

                if (commit.getParentCount() > 1) {
                	for (int i = 1; i < commit.getParentCount(); i++) {
                		getCommitsForMerge(commitInfoList, commit.getParent(i).getName(), mergeBaseList, maxCount, true);
                	}
                }
            }
        } catch (Exception e) {
            throw new GitOpsException("Get mainline commit log failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
                revWalk = null;
            }
        }
    }

    /**
     * 获取srcBranch从dstBranch分支出来的commit，或者srcBranch并入dstBranch的commit点，从startCommitId开始，最近不超过maxCount，不早于startTime的commit log
     * @param srcBranch 源分支名，准备merge进入branch的分支名
     * @param dstBranch     目标分支名，准备接收fromBranch并入的分支名
     * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
     * @param maxCount   最多获取的commit数，0:不限制，否则最多获取maxCount条记录
     * @param startTime  最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于startTime
     * @param logOnly    true:只获取日志不分析commit修改的文件
     * @return
     * @throws GitOpsException
     */
    public List<CommitInfo> getCommitsForBranch(String srcBranch, String dstBranch, String startCommitId, int maxCount, long startTime, boolean logOnly) throws GitOpsException {
        List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();

        RevCommit endCommit = getBranchCreateFrom(srcBranch, dstBranch);

        RevWalk revWalk = null;
        try {
            Repository repo = localGit.getRepository();
            revWalk = new RevWalk(repo);
            revWalk.setFirstParent(true);

            if (startCommitId == null) {
                ObjectId commitId = repo.resolve("refs/remotes/origin/" + srcBranch);
                revWalk.markStart(revWalk.parseCommit(commitId));
            } else {
                ObjectId lastCommitId = repo.resolve(startCommitId);
                revWalk.markStart(revWalk.parseCommit(lastCommitId));
                revWalk.next();
            }

            if (endCommit != null) {
                endCommit = revWalk.parseCommit(endCommit);
            }

            int count = 0;
            RevCommit commit = null;
            while ((commit = revWalk.next()) != null && !References.isSameObject(commit, endCommit)) {
                count++;
                commit = revWalk.parseCommit(commit);

                CommitInfo commitInfo = null;
                if (commit.getParentCount() > 0) {
                	RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0));
                    commitInfo = getChangeInfo(commit, parentCommit, logOnly);
                } else {
                    commitInfo = getChangeInfo(commit, commit, logOnly);
                }

                commitInfo.setBranch(srcBranch);
                commitInfoList.add(commitInfo);

                if (maxCount > 0 && count >= maxCount) {
                    break;
                }

                if (startTime > 0 && commit.getCommitTime() <= startTime) {
                    break;
                }
            }

            return commitInfoList;
        } catch (Exception e) {
            throw new GitOpsException("Get branch " + srcBranch + " mainline commit log failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
                revWalk = null;
            }
        }
    }

    /**
     * 获取某个tag从startCommitId开始，最近不超过maxCount，不早于startTime的commit log
     *
     * @param tag       标签名称
     * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
     * @param maxCount  最多获取的commit数，0:不限制，否则最多获取maxCount条记录
     * @param startTime 最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于startTime
     * @param logOnly   true:只获取日志不分析commit修改的文件
     * @throws GitOpsException
     */
    public List<CommitInfo> getCommitsForTag(String tag, String startCommitId, int maxCount, long startTime, boolean logOnly) throws GitOpsException {
        List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();

        RevWalk revWalk = null;
        try {
            Repository repo = localGit.getRepository();
            revWalk = new RevWalk(repo);
            revWalk.setFirstParent(true);

            if (startCommitId == null) {
                ObjectId commitId = repo.resolve(tag);
                revWalk.markStart(revWalk.parseCommit(commitId));
            } else {
                ObjectId lastCommitId = repo.resolve(startCommitId);
                revWalk.markStart(revWalk.parseCommit(lastCommitId));
                revWalk.next();
            }

            int count = 0;
            RevCommit commit = null;
            while ((commit = revWalk.next()) != null) {
                count++;
                commit = revWalk.parseCommit(commit);
                CommitInfo commitInfo = null;
                if (commit.getParentCount() > 0) {
                    RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0));
                    commitInfo = getChangeInfo(commit, parentCommit, logOnly);
                } else {
                    commitInfo = getChangeInfo(commit, commit, logOnly);
                }
                commitInfoList.add(commitInfo);

                if (maxCount > 0 && count >= maxCount) {
                    break;
                }

                if (startTime > 0 && commit.getCommitTime() <= startTime) {
                    break;
                }
            }

            return commitInfoList;
        } catch (IOException e) {
            throw new GitOpsException("Get tag " + tag + " mainline commit log failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
                revWalk = null;
            }
        }
    }

    /**
     * 初始化一个空的bare的git目录
     *
     * @throws GitOpsException
     */
    public void init() throws GitOpsException {
        File localPath = new File(repoLocalDir);
        if (localPath.exists()) {
            throw new GitOpsException("Directory:" + repoLocalDir + " already exists!");
        }

        try {
            this.localGit = Git.init().setDirectory(localPath).setBare(true).call();
        } catch (GitAPIException e) {
            throw new GitOpsException("Init git repository " + repoLocalDir + "failed, " + e.getMessage(), e);
        }
    }

    /**
     * 获取所有的分支列表
     *
     * @return 分支名列表
     * @throws GitOpsException
     */
    public List<String> getBranchList() throws GitOpsException {
        try {
            List<String> branchNameList = new ArrayList<String>();

            List<Ref> branchList = localGit.branchList().setListMode(ListMode.ALL).call();
            for (Ref ref : branchList) {
                if (ref.getName().startsWith("refs/heads/")) {
                    branchNameList.add(ref.getName().substring(11));
                }
            }

            return branchNameList;
        } catch (GitAPIException e) {
            throw new GitOpsException("Get branch list failed, " + e.getMessage(), e);
        }
    }

    public String getDefaultBranch() throws GitOpsException {
        try {
            String defaultBranch = null;

            Ref head = localGit.lsRemote().callAsMap().get(Constants.HEAD);

            if (head != null) {
                if (head.isSymbolic()) {
                    defaultBranch = head.getTarget().getName();
                } else {
                    ObjectId objectId = head.getObjectId();
                    if (objectId != null) {
                        List<Ref> refs = localGit.getRepository().getRefDatabase().getRefsByPrefix(Constants.R_REMOTES);
                        for (Ref ref : refs) {
                            if (ref.getObjectId().equals(objectId)) {
                                defaultBranch = ref.getName();
                                break;
                            }
                        }
                    }
                }
            }

            if (defaultBranch != null && defaultBranch.startsWith("refs/remotes/origin/")) {
                defaultBranch = defaultBranch.substring(20);
            }
            return defaultBranch;
        } catch (GitAPIException | IOException e) {
            throw new GitOpsException("Get default branch failed, " + e.getMessage(), e);
        }
    }

    /**
     * 从某个分支创建新的分支
     *
     * @param branchName      需要创建的分支名称
     * @param startBranchName 源头分支名称
     * @throws GitOpsException
     */
    public void createBranch(String branchName, String startBranchName) throws GitOpsException {
        try {
            localGit.branchCreate().setName(branchName).setStartPoint("refs/heads/" + startBranchName).call();
        } catch (GitAPIException e) {
            throw new GitOpsException("Create branch " + branchName + " from " + startBranchName + " failed, " + e.getMessage(), e);
        }
    }

    /**
     * 删除分支
     *
     * @param branchName 分支名称
     * @throws GitOpsException
     */
    public void deleteBranch(String branchName) throws GitOpsException {
        try {
            localGit.branchDelete().setBranchNames("refs/heads/" + branchName).call();
        } catch (GitAPIException e) {
            throw new GitOpsException("Delete branch " + branchName + " failed, " + e.getMessage(), e);
        }
    }

    /**
     * 分析commit，获取commit的详细信息，包括作者，描述，具体修改的文件清单内容等 同时，包括：commit的作者，修改时间，提交人，提交时间
     *
     * @param newCommit
     * @return
     * @throws GitOpsException
     */
    public CommitInfo getChangeInfo(RevCommit newCommit, boolean logOnly) throws GitOpsException {
        return getChangeInfo(newCommit, null, logOnly);
    }

    /**
     * * 分析commit，用于git的commit log分析得到commit关联的需求，修改的文件，每个文件删除，插入的行数的统计信息
     * 同时，包括：commit的作者，修改时间，提交人，提交时间
     *
     * @param oldCommit 比较的源commit
     * @param newCommit 比价的目标commit
     * @return
     * @throws GitOpsException
     */
    public CommitInfo getChangeInfo(RevCommit newCommit, RevCommit oldCommit, boolean logOnly) throws GitOpsException {
        try {
            Repository repo = localGit.getRepository();

            if (oldCommit == null) {
                RevWalk revWalk = new RevWalk(repo);
                try {
                    oldCommit = revWalk.parseCommit(newCommit.getParent(0));
                } finally {
                    revWalk.close();
                }
            }

            ObjectId oldHead = oldCommit.getTree();

            ObjectId head = newCommit.getTree();

            // prepare the two iterators to compute the diff between
            ObjectReader reader = repo.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldHead);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, head);

            // finally get the list of changed files
            CommitInfo commitInfo = null;
            if (logOnly) {
                commitInfo = new CommitInfo();
            } else {
                List<DiffEntry> diffs = localGit.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
                commitInfo = DiffEntryHelper.handleDiffEntrys(repo, newCommit, diffs, true, -1);
            }

            String comment = newCommit.getFullMessage();

            commitInfo.setId(newCommit.getName());
            commitInfo.setComment(comment);

            PersonIdent authorIdent = newCommit.getAuthorIdent();
            PersonIdent committerIdent = newCommit.getCommitterIdent();

            commitInfo.setAuthor(authorIdent.getName());
            commitInfo.setAuthorEmail(authorIdent.getEmailAddress());
            commitInfo.setAuthorDate(authorIdent.getWhen());

            commitInfo.setCommitter(committerIdent.getName());
            commitInfo.setCommitterEmail(committerIdent.getEmailAddress());
            commitInfo.setCommitterDate(committerIdent.getWhen());

            // TODO: 根据commit comment进行需求关联的处理
            /*
             * if (Config.reqNoPattern != null) { Matcher matcher =
             * Config.reqNoPattern.matcher(comment);
             *
             * while (matcher.find() && matcher.groupCount() >= 1) {
             * commitInfo.addRequirment(matcher.group(1)); } }
             *
             * if (Config.bugfixPattern != null) { Matcher matcher =
             * Config.bugfixPattern.matcher(comment);
             *
             * while (matcher.find() && matcher.groupCount() >= 1) {
             * commitInfo.addBugfix(matcher.group(1)); } }
             */

            return commitInfo;
        } catch (IOException | GitAPIException e) {
            throw new GitOpsException("Get diff failed, " + e.getMessage(), e);
        } finally {

        }
    }

    /**
     * 分析commit，用于commit修改代码的详细的diff信息，用于页面展示文件修改比对的展示 同时，包括：commit的作者，修改时间，提交人，提交时间
     *
     * @param newRef commit引用，例如："refs/remotes/origin/2.0.0"
     * @param oldRef commit引用，例如："refs/remotes/origin/1.0.0", 如果为null则当newRef是最早的一个commit
     * @param filePath 指定diff的文件路径
     * @param maxChangeCount 最大获取的changge数量限制, -1表示没有限制, 0表示只取diff基本数据
     * @return
     * @throws GitOpsException
     */
    public List<FileDiffInfo> getDiffInfo(String newRef, String oldRef, String filePath, int maxChangeCount) throws GitOpsException {
        RevWalk revWalk = null;

        try {
            Repository repo = localGit.getRepository();

            revWalk = new RevWalk(repo);
            ObjectId newObjId = null;
            if (StringUtils.isNotEmpty(newRef)) {
                newObjId = repo.resolve(newRef);
            }
            ObjectId oldObjId = null;
            if (StringUtils.isNotEmpty(oldRef)) {
                oldObjId = repo.resolve(oldRef);
            }
            if (newObjId == null) {
                throw new GitOpsException("Object reference " + newRef + " not found.");
            }
            
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            AbstractTreeIterator oldTreeIter = new CanonicalTreeParser();
            RevCommit newCommit = revWalk.parseCommit(newObjId);
            ObjectId head = newCommit.getTree();
            // prepare the two iterators to compute the diff between
            ObjectReader reader = repo.newObjectReader();
            newTreeIter.reset(reader, head);
            
            if (oldObjId == null) {
                // 这里为null, 可能是因为newRef是最早提交的第一个commit, 所以它没有parent, 这里可以用一个空树进行对比
                oldTreeIter = new EmptyTreeIterator();
            } else {
                RevCommit oldCommit = revWalk.parseCommit(oldObjId);
                // zouye：修复问题：git diff 得到的文件差异列表比 gitlab 得到的多
                // 源 commit 不是直接和目标 commit 比，而是和他们的最后一个 merge base 比。否则会出现diff时显示有差异，却怎么也合不进去的情况
                RevCommit mergeBase = getMergeBase(newCommit, oldCommit);
                if (mergeBase != null) {
                    oldCommit = mergeBase;
                }
                ObjectId oldHead = oldCommit.getTree();
                ((CanonicalTreeParser)oldTreeIter).reset(reader, oldHead);
            }
            
            
            DiffCommand diffCommand = localGit.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter);
            if (StringUtils.isNotEmpty(filePath)) {
            	diffCommand.setPathFilter(PathFilter.create(filePath));
            	maxChangeCount = -1; // 不做diff数量限制
			}
            
            // finally get the list of changed files
            List<DiffEntry> diffs = diffCommand.call();
            CommitInfo commitInfo = DiffEntryHelper.handleDiffEntrys(repo, newCommit, diffs, false, maxChangeCount);

            return commitInfo.getDiffInfoList();
        } catch (IOException | GitAPIException e) {
            throw new GitOpsException("Get diff failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
        }
    }

    /**
     * 下载整个仓库目录
     *
     * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
     * @param out      文件数据的输出到流
     * @throws GitOpsException
     */
    public void downloadRepo(String commitId, OutputStream out) throws GitOpsException {
        Repository repo = localGit.getRepository();
        ArchiveCommand.registerFormat("zip", new ZipFormat());

        try {
            localGit.archive().setTree(repo.resolve(commitId)).setFormat("zip").setOutputStream(out).call();
        } catch (RevisionSyntaxException | GitAPIException | IOException e) {
            throw new GitOpsException("Dwonload repo at " + commitId + " failed," + e.getMessage(), e);
        } finally {
            ArchiveCommand.unregisterFormat("zip");
        }
    }

    /**
     * 以流的方式从仓库存储中获取某个目录的数据
     *
     * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
     * @param filePath
     * @param out      文件数据的输出到流
     * @throws GitOpsException
     */
    public void downloadDir(String commitId, String filePath, OutputStream out) throws GitOpsException {
        Repository repo = localGit.getRepository();
        ArchiveCommand.registerFormat("zip", new ZipFormat());
        try {
            localGit.archive().setPaths(filePath).setTree(repo.resolve(commitId)).setFormat("zip").setOutputStream(out).call();
        } catch (RevisionSyntaxException | GitAPIException | IOException e) {
            throw new GitOpsException("Dwonload repo at " + commitId + " failed," + e.getMessage(), e);
        } finally {
            ArchiveCommand.unregisterFormat("zip");
        }
    }

    /**
     * 以流的方式从仓库存储中获取文件数据
     *
     * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
     * @param filePath
     * @param out      文件数据的输出到流
     * @throws GitOpsException
     */
    public void downloadFile(String commitId, String filePath, OutputStream out) throws GitOpsException {
        Repository repo = localGit.getRepository();

        RevWalk revWalk = null;
        TreeWalk treeWalk = null;

        try {
            ObjectId lastCommitId = repo.resolve(commitId);
            revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(lastCommitId);

            RevTree tree = commit.getTree();
            treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));

            if (!treeWalk.next()) {
                throw new GitOpsException("File " + filePath + " not found");
            }

            ObjectLoader loader = repo.open(treeWalk.getObjectId(0));

            InputStream in = loader.openStream();

            byte[] buffer = new byte[8 * 1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new GitOpsException("Download file " + filePath + " failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
            if (treeWalk != null) {
                treeWalk.close();
            }
        }
    }

    /**
     * 从仓库存储中获取文件数据
     *
     * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
     * @param filePath
     * @return 以字符串的方式返回文件内容，为了防止过度使用内存，最多返回2M内容，大文件应该用getSourceFileStream
     * @throws GitOpsException
     */
    public String getFileContent(String commitId, String filePath, int maxSize) throws GitOpsException {
        Repository repo = localGit.getRepository();

        RevWalk revWalk = null;
        TreeWalk treeWalk = null;

        try {
            ObjectId lastCommitId = repo.resolve(commitId);
            revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(lastCommitId);

            RevTree tree = commit.getTree();
            treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));

            if (!treeWalk.next()) {
                throw new GitOpsException("File " + filePath + " not found");
            }

            ObjectLoader loader = repo.open(treeWalk.getObjectId(0));
            byte[] bytes = loader.getBytes(1024);
            boolean isBinary = RawText.isBinary(bytes);

            String content = null;
            if (isBinary) {
                throw new GitOpsException("File " + filePath + " is binary");
            } else {
            	if (maxSize <= 0) {
            		maxSize = 2 * 1024 * 1024;
            	}
                content = new String(loader.getBytes(maxSize));
            }

            return content;
        } catch (IOException e) {
            throw new GitOpsException("Download file " + filePath + " failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
            if (treeWalk != null) {
                treeWalk.close();
            }
        }
    }

    /**
     * 从仓库存储中获取文件部分行数据
     *
     * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
     * @param filePath
     * @param startLine 开始行号
     * @param lineCount 行数，如果是正向获取则为正数，如果是负向获取则为负数
     * @return 以字符串列表的方式返回文件内容
     * @throws GitOpsException
     */
    public List<String> getFileLines(String commitId, String filePath, int startLine, int lineCount) throws GitOpsException {
    	List<String> content = new ArrayList<String>();
    	BufferedReader reader = null;
    	
        Repository repo = localGit.getRepository();

        RevWalk revWalk = null;
        TreeWalk treeWalk = null;

        try {
            ObjectId lastCommitId = repo.resolve(commitId);
            revWalk = new RevWalk(repo);
            ObjectLoader loader;
            if (revWalk.parseAny(lastCommitId) instanceof RevBlob){
                // 如果传入的是Blob文件的id, 就没必要解析commit了
                loader = repo.open(lastCommitId);
            }else{
                RevCommit commit = revWalk.parseCommit(lastCommitId);
                if (commit == null) {
                    throw new GitOpsException("Commit " + commitId + " not exist");
                }

                RevTree tree = commit.getTree();
                treeWalk = new TreeWalk(repo);
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filePath));

                if (!treeWalk.next()) {
                    throw new GitOpsException("File " + filePath + " not found");
                }
                loader = repo.open(treeWalk.getObjectId(0));
            }
            byte[] bytes = loader.getBytes(1024);
            boolean isBinary = RawText.isBinary(bytes);

            if (isBinary) {
                throw new GitOpsException("File " + filePath + " is binary");
            } else {
            	int start = 0;
            	int end = 0;
            	
            	if(lineCount > 0) {
            		start = startLine;
            		end = startLine + lineCount;
            	}else {
            		start = startLine + lineCount;
            		end = startLine;
            	}
            	
            	if(start < 1 ) {
        			start = 1;
        		}
            	
                ObjectStream stream = loader.openStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                String line = reader.readLine();
				if ( start == 1 && line != null) {
					content.add(line);
					start = start+1;
				}
				
				//略过不需要的行
				for (int i = 2; i < start && line != null; i++) {
					line = reader.readLine();
				}
				
				for (int i = start; i < end && (line = reader.readLine())!= null; i++) {
					content.add(line);
				}
            }
        } catch (IOException e) {
            throw new GitOpsException("Download file " + filePath + " failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
            if (treeWalk != null) {
                treeWalk.close();
            }
            if(reader != null) {
            	try {
					reader.close();
				} catch (IOException e) {
				}
            }
        }
        
        return content;
    }
    
    /**
     * 获取某个目录下的所有文件和子目录
     *
     * @param commitId commit的hash字串
     * @param filePath 相对于工程根的目录路径
     * @return
     * @throws GitOpsException
     */
    public List<FileInfo> listFolder(String commitId, String filePath) throws GitOpsException {
        List<FileInfo> fileInfoList = new LinkedList<FileInfo>();

        Repository repo = localGit.getRepository();

        RevWalk revWalk = null;
        TreeWalk treeWalk = null;

        try {
            ObjectId lastCommitId = repo.resolve(commitId);
            revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(lastCommitId);

            treeWalk = new TreeWalk(repo);
            treeWalk.setRecursive(true);

            String replaceFilePath = "";
            if (filePath != null && !filePath.equals("") && !filePath.equals("/")) {
                treeWalk.setFilter(PathFilter.create(filePath));
                replaceFilePath = filePath;
                if(!StringUtils.endsWith(filePath, "/")) {
                    replaceFilePath = filePath + "/";
                }
            }

            RevTree tree = commit.getTree();
            treeWalk.addTree(tree);

            if (!treeWalk.next()) {
                throw new GitOpsException("Path " + filePath + " not found");
            }
            treeWalk.setRecursive(false);

            if (treeWalk.isSubtree()) {
                treeWalk.enterSubtree();
            }

            while (treeWalk.next()) {
                FileInfo fileInfo = null;
                String fileFullPath = treeWalk.getPathString();
                String fileRealPath = fileFullPath;
                if(StringUtils.isNotEmpty(replaceFilePath)) {
                    fileRealPath = fileFullPath.replace(replaceFilePath, "");
                }
                if (treeWalk.isSubtree()) {
                    fileInfo = new FileInfo(fileRealPath, 'D');
                } else {
                    fileInfo = new FileInfo(fileRealPath, 'F');
                }
                // add(lastCommitId) 没加的话, 没有区分分支, 文件都是取最后一次的commit
                Iterable<RevCommit> log = localGit.log().add(lastCommitId).addPath(treeWalk.getPathString()).call();
                RevCommit latestCommit = log.iterator().next();
                if(latestCommit != null){
                    fileInfo.setLastAuthor(latestCommit.getAuthorIdent().getName());
                    fileInfo.setLastChangeDate(latestCommit.getAuthorIdent().getWhen());
                    fileInfo.setLastCommitMessage(latestCommit.getShortMessage());
                    fileInfo.setLastCommitId(latestCommit.getName());
                }

                fileInfoList.add(fileInfo);
            }

            return fileInfoList;
        } catch (IOException | GitAPIException e) {
            throw new GitOpsException("Download file " + filePath + " failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
            if (treeWalk != null) {
                treeWalk.close();
            }
        }
    }

    /**
     * 获取某个目录下的所有文件和子目录
     *
     * @param commitId commit的hash字串
     * @param filePath 相对于工程根的目录路径
     * @param skipCount 略过的数量，当skipCount=0时，代表不略过
     * @param limitCount 查询的最大数量，当limitCount=0是，代表查询全部
     * @author fengt
     * @return
     * @throws GitOpsException
     */
    public List<FileInfo> listFolder(String commitId, String filePath, int skipCount, int limitCount) throws GitOpsException {
        List<FileInfo> fileInfoList = new LinkedList<FileInfo>();

        Repository repo = localGit.getRepository();

        RevWalk revWalk = null;
        TreeWalk treeWalk = null;

        try {
            ObjectId lastCommitId = repo.resolve(commitId);
            if(lastCommitId == null){
                return null;
            }
            revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(lastCommitId);

            treeWalk = new TreeWalk(repo);
            treeWalk.setRecursive(true);

            String replaceFilePath = "";
            if (filePath != null && !filePath.equals("") && !filePath.equals("/")) {
                treeWalk.setFilter(PathFilter.create(filePath));
                replaceFilePath = filePath;
                if(!StringUtils.endsWith(filePath, "/")) {
                    replaceFilePath = filePath + "/";
                }
            }

            RevTree tree = commit.getTree();
            treeWalk.addTree(tree);

            boolean isWalked = treeWalk.next();
            if (!isWalked) {
                throw new GitOpsException("Path " + filePath + " not found");
            }
            treeWalk.setRecursive(false);

            int count = 0;
            if(skipCount < 0){
                skipCount = 0;
            }

        	while (isWalked) {
                count++;
                if(limitCount > 0){
                    int maxCount = skipCount + limitCount;
                    if(count <= skipCount){
                        continue;
                    }
                    if(count > maxCount){
                        break;
                    }
                }
                FileInfo fileInfo = null;
                String fileFullPath = treeWalk.getPathString();
                String fileRealPath = fileFullPath;
                if(StringUtils.isNotEmpty(replaceFilePath)) {
                    fileRealPath = fileFullPath.replace(replaceFilePath, "");
                }
                if (treeWalk.isSubtree()) {
                    fileInfo = new FileInfo(fileRealPath, 'D');
                } else {
                    fileInfo = new FileInfo(fileRealPath, 'F');
                }
                Iterable<RevCommit> log = localGit.log().add(lastCommitId).addPath(treeWalk.getPathString()).call();
                RevCommit latestCommit = log.iterator().next();
                if(latestCommit != null){
                    fileInfo.setLastAuthor(latestCommit.getAuthorIdent().getName());
                    fileInfo.setLastChangeDate(latestCommit.getAuthorIdent().getWhen());
                    fileInfo.setLastCommitMessage(latestCommit.getShortMessage());
                    fileInfo.setLastCommitId(latestCommit.getName());
                }

                fileInfoList.add(fileInfo);
                
                isWalked = treeWalk.next();
            }

            return fileInfoList;
        } catch (IOException | GitAPIException e) {
            throw new GitOpsException("Download file " + filePath + " failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
            if (treeWalk != null) {
                treeWalk.close();
            }
        }
    }


    /**
     * 获取某个目录下的LastCommit信息
     *
     * @param commitId commit的hash字串, 可以由分支得到
     * @param filePath 相对于工程根的目录路径
     * @return
     * @throws GitOpsException
     */
    public CommitInfo getFilePathLastCommit(String commitId, String filePath) throws GitOpsException {
        Repository repo = localGit.getRepository();
        try {
            if (StringUtils.isEmpty(filePath)) {
                return getCommitDetail(commitId, true);
            }

            ObjectId lastCommitId = repo.resolve(commitId);
            Iterable<RevCommit> log = localGit.log().add(lastCommitId).addPath(filePath).call();
            RevCommit latestCommit = log.iterator().next();
            if(latestCommit != null){
                return getCommitDetail(latestCommit.getName(), true);
            }
            return null;
        } catch (IOException | GitAPIException e) {
            throw new GitOpsException("getFilePathLastCommit " + filePath + " failed, " + e.getMessage(), e);
        }
    }
}
