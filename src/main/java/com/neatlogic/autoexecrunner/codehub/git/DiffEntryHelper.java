/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
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
