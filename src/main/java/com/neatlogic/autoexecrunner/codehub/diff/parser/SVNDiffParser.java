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
package com.neatlogic.autoexecrunner.codehub.diff.parser;

import com.neatlogic.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import com.neatlogic.autoexecrunner.codehub.dto.diff.Hunk;
import com.neatlogic.autoexecrunner.codehub.dto.diff.Line;
import com.neatlogic.autoexecrunner.codehub.dto.diff.Range;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A parser that parses a unified diff from text into a {@link FileDiffInfo} data
 * structure.
 * <p/>
 * An example of a unified diff this parser can handle is the following:
 * 
 * <pre>
 * Modified: trunk/test1.txt
 * ===================================================================
 * --- /trunk/test1.txt	2013-10-23 19:41:56 UTC (rev 46)
 * +++ /trunk/test1.txt	2013-10-23 19:44:39 UTC (rev 47)
 * &#64;@ -1,4 +1,3 @@
 * test1
 * -test1
 * +test234
 * -test1
 * \ No newline at end of file
 * &#64;@ -5,9 +6,10 @@
 * -test1
 * -test1
 * +test2
 * +test2
 * </pre>
 * 
 * Note that the TAB character and date after the file names are not being
 * parsed but instead cut off.
 */
public class SVNDiffParser implements DiffParser {
	public static final Pattern LINE_RANGE_PATTERN = Pattern.compile("^.*-([0-9]+)(?:,([0-9]+))? \\+([0-9]+)(?:,([0-9]+))?.*$");

	private boolean onlyChangeInfo;

	// diff中最大的增删行量, 如果设置-1则表示不过滤折叠, 将全部diff信息获取到, 否则只会取头部基本数据
	private int maxChangeCount = -1;
	
	// 指定diff的文件路径, 如果为空则表示取所有文件的diff
	private String filePath;
	
	public SVNDiffParser() {

	}

	public SVNDiffParser(boolean onlyChangeInfo,String filePath, int maxChangeCount) {
		this.onlyChangeInfo = onlyChangeInfo;
		this.filePath = filePath;
		this.maxChangeCount = maxChangeCount;
	}
	
	public int getMaxChangeCount() {
		return maxChangeCount;
	}

	public void setMaxChangeCount(int maxChangeCount) {
		this.maxChangeCount = maxChangeCount;
	}
	
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	@Override
	public List<FileDiffInfo> parse(InputStream in) {
		List<FileDiffInfo> diffInfoList = new LinkedList<FileDiffInfo>();
		ResizingParseWindow window = new ResizingParseWindow(in);

		int lineSize = 0;
		boolean onlyChangeInfoInit = onlyChangeInfo;
		boolean isPropertyChangesInfo = false;
		
		try {
			FileDiffInfo currentDiff = null;
			String currentLine = null;
			String prevLine = null;

			while ((currentLine = window.slideForward()) != null) {
				lineSize++;

				if (!onlyChangeInfo && maxChangeCount > 0 && lineSize > maxChangeCount) {
					onlyChangeInfo = true;
					if (currentDiff != null) {
						currentDiff.setCollapsed(true);
						if (currentDiff.getHunks() != null) {
							currentDiff.getHunks().clear();
						}
					}
				}
				
				if(currentLine.startsWith("Index: ")) {
					//一个新文件开始时将lineSize初始化为0,将onlyChangeInfo初始化为初始值
					lineSize = 1;
					onlyChangeInfo = onlyChangeInfoInit;
					String line1 = currentLine;
					String line2 = window.getFutureLine(1);
					if("===================================================================".equals(line2)) {
						String line3 = window.getFutureLine(2);
						String line4 = window.getFutureLine(3);
		
						if(currentDiff != null ) {
                            setModifiedType(currentDiff);
							diffInfoList.add(currentDiff);
						}
						
						currentDiff = new FileDiffInfo();

						if (line3.startsWith("--- ")) {
							if (line4.startsWith("+++ ")) {

								/*
								 * 这里解析出commit id, 跟git的位置不一样
								 * 
								 * Index: 2.txt
								 * ===================================================================
								 * ...
								 *        --- 2.txt(revision 55)
								 *        +++ 2.txt(revision 1)
								 *        
								 *        
								 * 第一个是from revision, 第二个是to revision, 
								 * 保存起来 以备后续文件内容读取使用
								 *
								  */

								parseFromFile(currentDiff, line3);
								parseToFile(currentDiff, line4);
								
								parseHeader(currentDiff, line1);
								parseHeader(currentDiff, line2);

								window.slideForward();
								window.slideForward();
								window.slideForward();
								
								if (StringUtils.isNotEmpty(filePath)) {
									// 指定diff的文件路径, 匹配不了就直接返回
									if (!filePath.equals(currentDiff.getToFileName()) && !filePath.equals(currentDiff.getFromFileName())) {
										currentDiff = null;
										continue;
									}
								}
							}
						} else if (line3.startsWith("Cannot display: file marked as a binary type.")) {
							parseHeader(currentDiff, line1);
							parseHeader(currentDiff, line2);

							currentDiff.setBinary(true);
							String fileName = line1.substring(7);
							currentDiff.setFromFileName(fileName);
							currentDiff.setToFileName(fileName);
							window.slideForward();
							window.slideForward();
							window.slideForward();
						}
					} else if(currentDiff != null) {
						parseNeutralLine(currentDiff, currentLine);
					}
				}
				else if (currentDiff != null && currentLine.startsWith("+")) {
					parseToLine(currentDiff, currentLine);
				} else if (currentDiff != null && currentLine.startsWith("-")) {
					parseFromLine(currentDiff, currentLine);
				} else if (currentDiff != null && LINE_RANGE_PATTERN.matcher(currentLine).matches()) {
					parseHunkStart(currentDiff, currentLine);
				} else if(currentDiff != null && !"\\ No newline at end of file".equals(currentLine)){

					// 对于merge commit来说, 末尾会多了一堆property信息, svn diff 解析没有处理这块逻辑
					if (!isPropertyChangesInfo && currentLine.startsWith("Property changes on:")) {
						isPropertyChangesInfo = true;
					} else if (isPropertyChangesInfo && StringUtils.isBlank(currentLine) && StringUtils.isBlank(prevLine)) {
						isPropertyChangesInfo = false;
					} else if (!isPropertyChangesInfo) {
						parseNeutralLine(currentDiff, currentLine);
					}
					prevLine = currentLine;
					/* 属性修改
					Property changes on: trunk/运维使用/qq10.txt
					___________________________________________________________________
					Added: svn:mime-type
					   + text/plain
					
					
					Property changes on: trunk
					___________________________________________________________________
					Modified: svn:mergeinfo
					   Merged /branches/branch-1:r179
					
					* */
				}
			}
			
            if (currentDiff != null) {
            	// ResizingParseWindow读取 DIFF 的内容末尾会多一个空行
				if (!onlyChangeInfo) {
					Hunk lastHunk = currentDiff.getLatestHunk();
					if(lastHunk != null) {
						List<Line> lines = lastHunk.getLines();
						if (CollectionUtils.isNotEmpty(lines)) {
							Line lastLine = lines.get(lines.size() - 1);
							if (lastLine.getLineType().equals(Line.LineType.NEUTRAL) && StringUtils.isEmpty(lastLine.getContent())) {
								lines.remove(lines.size() - 1);
							}
						}
					}
				}
                setModifiedType(currentDiff);
				diffInfoList.add(currentDiff);
			}
		} finally {
		}

		int diffInfoCount = diffInfoList.size();
		if(diffInfoCount > 0) {
			/*
			
			Property changes on: 
			___________________________________________________________________
			Modified: svn:mergeinfo
			   Reverse-merged /branches/1.0.0:r505-550
			   Reverse-merged /trunk:r499-540
			 */
			FileDiffInfo diffInfo = diffInfoList.get(diffInfoCount-1);
			
			// replaced 类型 , 修复 hunk不存在时候的异常错误
			Hunk lastHunk = diffInfo.getLatestHunk();
			if (lastHunk != null) {
				List<Line> lines = lastHunk.getLines();
				int linesCount = lines.size();
				if(linesCount > 8) {
					if(lines.get(linesCount - 3).getContent().startsWith("   Reverse-merged ")
						&& lines.get(linesCount - 4).getContent().startsWith("   Reverse-merged ")
						&& lines.get(linesCount - 5).getContent().startsWith("Modified: svn:mergeinfo")
						&& lines.get(linesCount - 6).getContent().equals("___________________________________________________________________")
						&& lines.get(linesCount - 7).getContent().equals("Property changes on: ")
					){
						lines.remove(linesCount-1);
						lines.remove(linesCount-2);
						lines.remove(linesCount-3);
						lines.remove(linesCount-4);
						lines.remove(linesCount-5);
						lines.remove(linesCount-6);
						lines.remove(linesCount-7);
						lines.remove(linesCount-8);
					}
				}
			}
			
		}
		
		return diffInfoList;
	}

    /**
     * 设置文件修改类型 [新增,删除,修改]
     * 
     * @param currentDiff
     */
    private void setModifiedType(FileDiffInfo currentDiff) {

    	// 新增和删除 是通过diff中的标识得到
		if (currentDiff.getModifiedType() == 'U') {
			if (currentDiff.getInsertedCount() > 0 || currentDiff.getDeletedCount() > 0) {
				currentDiff.setModifiedType('M');
			}
		}
    }

	@Override
	public FileDiffInfo parseSingle(InputStream in) {
		FileDiffInfo currentDiff = new FileDiffInfo();

		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(in));
			String line1 = br.readLine();
			String line2 = br.readLine();
			String line3 = br.readLine();
			String line4 = br.readLine();

			parseHeader(currentDiff, line1);
			parseHeader(currentDiff, line2);

			if (line3.startsWith("--- ")) {
				parseFromFile(currentDiff, line3);
				if (line4.startsWith("+++ ")) {
					parseToFile(currentDiff, line4);
				}
			} else if (line3.startsWith("Cannot display: file marked as a binary type.")) {
				currentDiff.setBinary(true);
				String fileName = line1.substring(7);
				currentDiff.setFromFileName(fileName);
				currentDiff.setToFileName(fileName);
			}

			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("+")) {
					parseToLine(currentDiff, line);
				} else if (line.startsWith("-")) {
					parseFromLine(currentDiff, line);
				} else if (LINE_RANGE_PATTERN.matcher(line).matches()) {
					parseHunkStart(currentDiff, line);
				} else if(!"\\ No newline at end of file".equals(line)){
					parseNeutralLine(currentDiff, line);
				}
			}
		} catch (IOException e) {

		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
				br = null;
			}
		}

		return currentDiff;
	}

	private void parseNeutralLine(FileDiffInfo currentDiff, String currentLine) {
		if (!onlyChangeInfo) {
			Line line = new Line(Line.LineType.NEUTRAL, currentLine);
			Hunk lastHunk = currentDiff.getLatestHunk();
			if(lastHunk != null) {
				lastHunk.getLines().add(line);
			}
		}
	}

	private void parseToLine(FileDiffInfo currentDiff, String currentLine) {
		if (!onlyChangeInfo) {
			Line toLine = new Line(Line.LineType.TO, currentLine.substring(1));
			currentDiff.getLatestHunk().getLines().add(toLine);
		}
		currentDiff.increaseInstedCount();
	}

	private void parseFromLine(FileDiffInfo currentDiff, String currentLine) {
		if (!onlyChangeInfo) {
			Line fromLine = new Line(Line.LineType.FROM, currentLine.substring(1));
			currentDiff.getLatestHunk().getLines().add(fromLine);
		}

		currentDiff.increaseDeletedCount();
	}

	private void parseHunkStart(FileDiffInfo currentDiff, String currentLine) {
		if (!onlyChangeInfo) {
			Matcher matcher = LINE_RANGE_PATTERN.matcher(currentLine);
			if (matcher.matches()) {
				String range1Start = matcher.group(1);
				String range1Count = (matcher.group(2) != null) ? matcher.group(2) : "1";
				Range fromRange = new Range(Integer.valueOf(range1Start), Integer.valueOf(range1Count));

				String range2Start = matcher.group(3);
				String range2Count = (matcher.group(4) != null) ? matcher.group(4) : "1";
				Range toRange = new Range(Integer.valueOf(range2Start), Integer.valueOf(range2Count));

				Hunk hunk = new Hunk();
				hunk.setFromFileRange(fromRange);
				hunk.setToFileRange(toRange);
				currentDiff.getHunks().add(hunk);
			} else {
				throw new IllegalStateException(String.format("No line ranges found in the following hunk start line: '%s'. Expected something " + "like '-1,5 +3,5'.", currentLine));
			}
		}

	}

//	Index: 1.txt
//	===================================================================
//	--- 1.txt       (revision 25)
//	+++ 1.txt       (nonexistent)
	// SVN 利用nonexistent是否存在来设置修改类型



	private void parseToFile(FileDiffInfo currentDiff, String currentLine) {
		currentDiff.setToIndexId(getRevision(currentLine));
		currentDiff.setToFileName(cutAfterTab(currentLine.substring(4)));

		if (currentLine.endsWith("(nonexistent)")) {
			currentDiff.setModifiedType('D');
		}
	}

	private void parseFromFile(FileDiffInfo currentDiff, String currentLine) {
		currentDiff.setFromIndexId(getRevision(currentLine));
		currentDiff.setFromFileName(cutAfterTab(currentLine.substring(4)));

		if (currentLine.endsWith("(nonexistent)")) {
			currentDiff.setModifiedType('A');
		}
	}

	/**
	 * Cuts a TAB and all following characters from a String.
	 */
	private String cutAfterTab(String line) {
		Pattern p = Pattern.compile("^(.*)\\t.*$");
		Matcher matcher = p.matcher(line);
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			return line;
		}
	}
	
	
	/**
	 * 解析出diff文件右侧的revision号码, 用于读取diff文件内容
	 * 
	 * @param line
	 * @return
	 */
	private String getRevision(String line) {
		Pattern p = Pattern.compile("\t\\(revision (\\d+)\\)");
		Matcher matcher = p.matcher(line);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}

	private void parseHeader(FileDiffInfo currentDiff, String currentLine) {
		currentDiff.getHeaderLines().add(currentLine);
	}

	public boolean isOnlyChangeInfo() {
		return onlyChangeInfo;
	}

	public void setOnlyChangeInfo(boolean onlyChangeInfo) {
		this.onlyChangeInfo = onlyChangeInfo;
	}
}
