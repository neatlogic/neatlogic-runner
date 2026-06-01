/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.neatlogic.autoexecrunner.codehub.git;

import com.neatlogic.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.neatlogic.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import com.neatlogic.autoexecrunner.codehub.exception.GitOpsException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;



public class DiffEntryHelper {
	public static CommitInfo handleDiffEntrys(Repository repo, RevCommit newCommit, List<DiffEntry> diffs, boolean onlyChangeInfo, int maxChangeCount) throws GitOpsException {
		ByteArrayOutputStream out = null;
		DiffFormatter formatter = null;
		RenameDetector renameDetector = null;

		try {
			CommitInfo commitInfo = new CommitInfo();

			out = new ByteArrayOutputStream();
			formatter = new DiffFormatter(out);
			formatter.setRepository(repo);
			
			// 检测重命名
			formatter.setDetectRenames(true);
			renameDetector = formatter.getRenameDetector();
			renameDetector.addAll(diffs);
			List<DiffEntry> renameEntryList = renameDetector.compute();

			for (DiffEntry entry : renameEntryList) {
				formatter.format(entry);
				FileDiffInfo diffInfo = FileDiffInfo.parseGitSingleDiffLog(out, onlyChangeInfo, maxChangeCount);
				if (entry.getScore() >= renameDetector.getRenameScore()) {
					diffInfo.setModifiedType(DiffEntry.ChangeType.RENAME.toString().charAt(0));
				} else {
					diffInfo.setModifiedType(entry.getChangeType().toString().charAt(0));
				}
				diffInfo.setFromFileName(entry.getOldPath());
				diffInfo.setToFileName(entry.getNewPath());
				commitInfo.addDiffInfo(diffInfo);
				out.reset();
			}

			return commitInfo;
		} catch (IOException e) {
			throw new GitOpsException("Get diff failed, " + e.getMessage(), e);
		} finally {
			if (formatter != null) {
				formatter.close();
				formatter = null;
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
				out = null;
			}
		}
	}
}
