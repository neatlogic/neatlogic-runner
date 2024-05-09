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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A parser that parses a unified diff from text into a {@link FileDiffInfo}
 * data structure.
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
public class GITDiffParser implements DiffParser {
	public static final Pattern LINE_RANGE_PATTERN = Pattern.compile("^.*-([0-9]+)(?:,([0-9]+))? \\+([0-9]+)(?:,([0-9]+))?.*$");

	private boolean onlyChangeInfo;

	// diff中最大的增删行量, 如果设置-1则表示不过滤折叠, 将全部diff信息获取到, 否则只会取头部基本数据
	private int maxChangeCount = -1;


	public GITDiffParser() {

	}

	public GITDiffParser(boolean onlyChangeInfo, int maxChangeCount) {
		this.onlyChangeInfo = onlyChangeInfo;
		this.maxChangeCount = maxChangeCount;
	}


	public int getMaxChangeCount() {
		return maxChangeCount;
	}

	public void setMaxChangeCount(int maxChangeCount) {
		this.maxChangeCount = maxChangeCount;
	}

	@Override
	/**
	 * not supported
	 */
	@Deprecated
	public List<FileDiffInfo> parse(InputStream in) {
		return null;
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
			String line4 = null;

			if (line1 != null && line1.startsWith("diff --git ")) {
				if (line3.startsWith("new mode ")) {
					// 修改了文件权限
					line2 = br.readLine();
					line3 = br.readLine();
					line4 = br.readLine();
				} else if (line3.startsWith("index ")) {
					// 此文件为新增文件
					line2 = line3;
					line3 = br.readLine();
					line4 = br.readLine();
				} else if (line3.startsWith("--- ")) {
					// 此文件仅修改了内容
					line4 = br.readLine();
				} else if (line3.startsWith("rename ")) {
					// 此文件为重命名文件, 且可能修改了内容 add 2021-2-2
					/*
					diff --git a/pom4.xml b/pom5.xml
					similarity index 99%
					rename from pom4.xml
					rename to pom5.xml
					index 6cea2a8..aeef70c 100644
					--- a/pom4.xml
					+++ b/pom5.xml
					@@ -7,7 +7,7 @@
					* */

					// line3 == rename from
					br.readLine();			// rename to
					line2 = br.readLine();	// index
					if (line2 == null) {
						// 这里是检测到重命名, 但是没有内容修改的情况下, line2以下则为空
						return currentDiff;
					}
					line3 = br.readLine();	// --- a
					line4 = br.readLine();	// +++ b
				}

				parseHeader(currentDiff, line1);
				parseHeader(currentDiff, line2);

				// 此处的第二行就是 index 0000000..aae5e41, 也有可能是index 123456..aae5e41 100644
				if (line2.startsWith("index ")){
					/* 查看mr左右两侧对应文件的内容时候会用到, 根据commit查某文件内容, git的id在diff头 headerLines中有保存,可以从这里提取, svn的在别处 */
					String[] data = line2.split("\\s+")[1].split("\\.\\.");
					if (data.length == 2) {
						currentDiff.setFromIndexId(data[0]);
						currentDiff.setToIndexId(data[1]);
					}
				}

				if (line4 == null) {
					String diffLine = line1.substring(10);
					int halfLen = diffLine.length() / 2;
					currentDiff.setFromFileName(diffLine.substring(3, halfLen));
					currentDiff.setToFileName(diffLine.substring(halfLen + 3));
				} else {
					if (line3.startsWith("--- ")) {
						parseFromFile(currentDiff, line3);
						if (line4.startsWith("+++ ")) {
							parseToFile(currentDiff, line4);
						}
					} else if (line3.startsWith("Binary files ")) {
						currentDiff.setBinary(true);
						String diffLine = line1.substring(10);
						int halfLen = diffLine.length() / 2;
						currentDiff.setFromFileName(diffLine.substring(3, halfLen));
						currentDiff.setToFileName(diffLine.substring(halfLen + 3));
					}
				}

				String line = null;
				int lineSize = 0;
				while ((line = br.readLine()) != null) {
					lineSize++;
					if (!onlyChangeInfo && maxChangeCount > 0 && lineSize > maxChangeCount) {
						onlyChangeInfo = true;
						currentDiff.setCollapsed(true);

						if (currentDiff.getHunks() != null) {
							currentDiff.getHunks().clear();
						}
					}

					if (line.startsWith("+")) {
						parseToLine(currentDiff, line);
					} else if (line.startsWith("-")) {
						parseFromLine(currentDiff, line);
					} else if (LINE_RANGE_PATTERN.matcher(line).matches()) {
						parseHunkStart(currentDiff, line);
					} else if (!"\\ No newline at end of file".equals(line)) {
						parseNeutralLine(currentDiff, line);
					}
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

		if (currentDiff.getInsertedCount() > 0 && currentDiff.getDeletedCount() == 0) {
			currentDiff.setModifiedType('A');
		} else if (currentDiff.getInsertedCount() == 0 && currentDiff.getDeletedCount() > 0) {
			currentDiff.setModifiedType('D');
		}

		return currentDiff;
	}

	private void parseNeutralLine(FileDiffInfo currentDiff, String currentLine) {
		if (!onlyChangeInfo) {
			/*
			 * //diff --git a/db/1.0.0/db2test.test/hello.bnd
			 * b/db/1.0.0/db2test.test/hello.bnd deleted file mode 100644 index
			 * d725cf0..0000000 --- a/db/1.0.0/db2test.test/hello.bnd +++ /dev/null Binary
			 * files differ
			 */
			Hunk hunk = currentDiff.getLatestHunk();
			if (hunk != null) {
				Line line = new Line(Line.LineType.NEUTRAL, currentLine);
				currentDiff.getLatestHunk().getLines().add(line);
			} else if ("Binary files differ".equals(currentLine)) {
				currentDiff.setBinary(true);
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
			Hunk hunk = currentDiff.getLatestHunk();
			if(hunk != null) {
				hunk.getLines().add(fromLine);
			}
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

	private void parseToFile(FileDiffInfo currentDiff, String currentLine) {
		currentDiff.setToFileName(cutAfterTab(currentLine.substring(4)));
	}

	private void parseFromFile(FileDiffInfo currentDiff, String currentLine) {
		currentDiff.setFromFileName(cutAfterTab(currentLine.substring(4)));
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
