/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neatlogic.autoexecrunner.codehub.dto.diff;

import com.neatlogic.autoexecrunner.codehub.diff.parser.GITDiffParser;
import com.neatlogic.autoexecrunner.codehub.diff.parser.SVNDiffParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Diff between two files.
 *
 * @author Tom Hombergs <tom.hombergs@gmail.com>
 */
public class FileDiffInfo {
	private String fromFileName;

	private String toFileName;
	
	private String fromIndexId;
	private String toIndexId;
	
	// D dir, F file
	private char fileType = 'U'; //'D', 'F'
	
	/**GIT: <br>A Add, C COPY, D DELETE, M MODIFY, R RENAME <br>SVN<br>A ADD, D DELETE, M MODIFY, R Replaced */
	// U unknown
	private char modifiedType = 'U';
	
	private boolean isBinary = false;

	private List<String> headerLines = new ArrayList<>();

	private List<Hunk> hunks = new ArrayList<>();

	private int insertedCount = 0;

	private int deletedCount = 0;
	
	private boolean collapsed = false;
	
	public static FileDiffInfo parseGitSingleDiffLog(ByteArrayOutputStream diffOut, boolean onlyChangeInfo, int maxChangeCount) {
		GITDiffParser parser = new GITDiffParser(onlyChangeInfo, maxChangeCount);
		return parser.parseSingle(new ByteArrayInputStream(diffOut.toByteArray()));
	}

	public static List<FileDiffInfo> parseSvnDiffLog(ByteArrayOutputStream diffOut, boolean onlyChangeInfo) {
		return parseSvnDiffLog(diffOut, onlyChangeInfo, "", -1);
	}
	
	public static List<FileDiffInfo> parseSvnDiffLog(ByteArrayOutputStream diffOut, boolean onlyChangeInfo, String filePath, int maxChangeCount) {
		SVNDiffParser parser = new SVNDiffParser(onlyChangeInfo,filePath, maxChangeCount);
		return parser.parse(new ByteArrayInputStream(diffOut.toByteArray()));
	}
	
	public static FileDiffInfo parseSvnSingleDiffLog(ByteArrayOutputStream diffOut, boolean onlyChangeInfo) {
		SVNDiffParser parser = new SVNDiffParser(onlyChangeInfo, "", -1);
		return parser.parseSingle(new ByteArrayInputStream(diffOut.toByteArray()));
	}
	/**
	 * The header lines of the diff. These lines are purely informational and are
	 * not parsed.
	 *
	 * @return the list of header lines.
	 */
	public List<String> getHeaderLines() {
		return headerLines;
	}

	public void setHeaderLines(List<String> headerLines) {
		this.headerLines = headerLines;
	}

	/**
	 * Gets the name of the first file that was compared with this Diff (the file
	 * "from" which the changes were made, i.e. the "left" file of the diff).
	 *
	 * @return the name of the "from"-file.
	 */
	public String getFromFileName() {
		return fromFileName;
	}

	/**
	 * Gets the name of the second file that was compared with this Diff (the file
	 * "to" which the changes were made, i.e. the "right" file of the diff).
	 *
	 * @return the name of the "to"-file.
	 */
	public String getToFileName() {
		return toFileName;
	}

	/**
	 * The list if all {@link Hunk}s which contain all changes that are part of this
	 * Diff.
	 *
	 * @return list of all Hunks that are part of this Diff.
	 */
	public List<Hunk> getHunks() {
		return hunks;
	}

	public void setFromFileName(String fromFileName) {
		this.fromFileName = fromFileName;
	}

	public void setToFileName(String toFileName) {
		this.toFileName = toFileName;
	}

	public void setHunks(List<Hunk> hunks) {
		this.hunks = hunks;
	}

	/**
	 * Gets the last {@link Hunk} of changes that is part of this Diff.
	 *
	 * @return the last {@link Hunk} that has been added to this Diff.
	 */
	public Hunk getLatestHunk() {
		Hunk hunk = null;
		if ( hunks.size() > 0) {
			hunk = hunks.get(hunks.size() - 1);
		} 
		
		return hunk;
	}

	
	public boolean isBinary() {
		return isBinary;
	}

	public void setBinary(boolean isBinary) {
		this.isBinary = isBinary;
	}

	public char getFileType() {
		return fileType;
	}

	public void setFileType(char fileType) {
		this.fileType = fileType;
	}

	public char getModifiedType() {
		return modifiedType;
	}

	public void setModifiedType(char modifiedType) {
		this.modifiedType = modifiedType;
	}

	public int getInsertedCount() {
		return insertedCount;
	}

	public void setInsertedCount(int insertedCount) {
		this.insertedCount = insertedCount;
	}

	public void increaseInstedCount() {
		this.insertedCount++;
	}

	public int getDeletedCount() {
		return deletedCount;
	}

	public void setDeletedCount(int deletedCount) {
		this.deletedCount = deletedCount;
	}

	public void increaseDeletedCount() {
		this.deletedCount++;
	}

	public boolean isCollapsed() {
		return collapsed;
	}

	public void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed;
	}

	public String getFromIndexId() {
		return fromIndexId;
	}

	public void setFromIndexId(String fromIndexId) {
		this.fromIndexId = fromIndexId;
	}

	public String getToIndexId() {
		return toIndexId;
	}

	public void setToIndexId(String toIndexId) {
		this.toIndexId = toIndexId;
	}

	@Override
	public String toString() {
		String content = "********************************************************************\n";
		
		for (String line : headerLines) {
			content = content + "" + line + "\n";
		}
		content = content + "fileType:" + fileType + " ";
		content = content + "modifiedType:" + modifiedType + " ";
		content = content + "isBinary:" + isBinary + "\n";
 		content = content + "inserted " + insertedCount + "  deleted " + deletedCount + "\n";
 		content = content + "-------------------------------------------------------------------\n";
		
		content = content + "---" + fromFileName + "\n";
		content = content + "+++" + toFileName + "\n";

		
		for (Hunk hunk : hunks) {
			content = content + hunk.toString();
		}
		
		content = content + "********************************************************************\n";
		
		return content;
	}
}
