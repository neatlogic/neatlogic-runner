package com.neatlogic.autoexecrunner.codehub.diff.parser;

import com.neatlogic.autoexecrunner.codehub.dto.commit.CommitInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**   
 * @ClassName   SVNLogParser   
 * @Description 解析  `svn log` 命令的输出结果为 CommitInfo 格式，暂仅支持解析基本信息，不支持解析 diff
 * @author      zouye
 * @date        2021-06-23   
 *    
 */
public class SVNLogParser {
	/** Date formatter for svn timestamp */
	private static final SimpleDateFormat SVN_TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/** State machine constant: expecting header */
	private static final int GET_HEADER = 1;

	/** State machine constant: expecting file information */
	private static final int GET_FILE = 2;

	/** State machine constant: expecting comments */
	private static final int GET_COMMENT = 3;

	/** file info start */
	private static final String FILE_BEGIN_TOKEN = "Changed paths:";

	/** The file section ends with a blank line */
	private static final String FILE_END_TOKEN = "";

	/** The filename starts after 5 characters */
	private static final int FILE_START_INDEX = 5;

	/** The comment section separator with a dashed line */
	private static final String LOG_SEPARATOR = "------------------------------------"
			+ "------------------------------------";

	/** Current status of the parser */
	private int status = GET_HEADER;

	/** List of change log entries */
	private List<CommitInfo> entries;

	/** The current log entry being processed by the parser */
	private CommitInfo currentLogEntry;

	/** The current comment of the entry being processed by the parser */
	private StringBuffer currentComment;

	private Pattern pattern = Pattern.compile(
			"^r(\\d+)\\s+\\|\\s+"                        // revision number
			+ "([^|]+)\\|\\s+"                           // author username
			+ "(\\d+-\\d+-\\d+ "                         // date 2002-08-24
			+ "\\d+:\\d+:\\d+) "                         // time 16:01:00
			+ "([\\-+])(\\d\\d)(\\d\\d).+?\\s+\\|\\s+"   // gmt offset -0400
			+ "(\\d+)\\s+lines?"                         // comment line count
	);

	/** comment line count */
	private int commentLineCount = 0;

	/** next log entry start index */
	private int nextLogItemIndex = 0;

	private int currentIndex = 0;

	private String[] logs = null;

	/**
	 * Default constructor.
	 */
	public SVNLogParser() {
	}

	/**
	 * Parse the input stream into a collection.
	 *
	 * @param logStr svn log output string
	 * @return A collection of ChangeLogEntry's
	 */
	public List<CommitInfo> parse(String logStr) {
		if (logStr == null || logStr.equals("")) {
			return null;
		}
		
		this.currentIndex = 0;
		this.nextLogItemIndex = 0;
		this.entries = new ArrayList<>();

		this.logs = logStr.split("(\r\n|\n)");

		commentLineCount = 0;

		// Current state transitions in the parser's state machine:
		// Get Header -> Get File
		// Get File -> Get Comment or Get (another) File
		// Get Comment -> Get (another) Comment
		String line = null;

		while (currentIndex < logs.length) {
			line = logs[currentIndex];

			switch (status) {
			case GET_HEADER:
				processGetHeader(line);

				break;

			case GET_FILE:
				processGetFile(line);

				break;

			case GET_COMMENT:
				processGetComment(line);

				break;

			default:
				throw new IllegalStateException("Unknown state: " + status);
			}

			currentIndex++;
		}

		return entries;
	}

	/**
	 * Process the current input line in the GET_HEADER state. The author, date, and
	 * the revision of the entry are gathered. Note, Subversion does not have
	 * per-file revisions, instead, the entire repository is given a single revision
	 * number, which is used for the revision number of each file.
	 *
	 * @param line A line of text from the svn log output
	 */
	private void processGetHeader(String line) {
		// ------------------------------------------------------------------------
		if (LOG_SEPARATOR.equals(line)) {
			return;
		}
		
		Matcher matcher = pattern.matcher(line);
		if (!matcher.matches()) {
			return;
		}

		currentLogEntry = new CommitInfo();

		/* set author to be trimmed author field */
		String author = matcher.group(2).trim();
		currentLogEntry.setId(matcher.group(1));
		currentLogEntry.setAuthor(author);
		currentLogEntry.setCommitter(author);

		commentLineCount = Integer.valueOf(matcher.group(7));

		try {
			StringBuffer dateStr = new StringBuffer().append(matcher.group(3)).append(" GMT").append(matcher.group(4))
					.append(matcher.group(5)).append(':').append(matcher.group(6));

			Date date = SVN_TIMESTAMP.parse(dateStr.toString());
			currentLogEntry.setAuthorDate(date);
			currentLogEntry.setCommitterDate(date);
		} catch (ParseException e) {
			System.err.println(e);
		}

		status = GET_FILE;
	}

	/**
	 * Process the current input line in the GET_FILE state. This state adds each
	 * file entry line to the current change log entry. Note, the revision number
	 * for the entire entry is used for the revision number of each file.
	 *
	 * @param line A line of text from the svn log output
	 */
	private void processGetFile(String line) {
		if (line.equals(FILE_BEGIN_TOKEN)) {
			status = GET_FILE;
		} else if (line.equals(FILE_END_TOKEN)) {
			nextLogItemIndex = currentIndex + commentLineCount + 1;
			status = GET_COMMENT;
			currentComment = new StringBuffer();
		} else {
			String name = line.substring(FILE_START_INDEX);
			System.out.println("File: " + name);
			status = GET_FILE;
		}
	}

	/**
	 * Process the current input line in the GET_COMMENT state. This state gathers
	 * all of the comments that are part of a log entry.
	 *
	 * @param line a line of text from the svn log output
	 */
	private void processGetComment(String line) {
		// next log item start
		if (nextLogItemIndex == currentIndex) {
			currentLogEntry.setComment(currentComment.toString());
			entries.add(currentLogEntry);

			status = GET_HEADER;
		} else {
			// 由于使用换行符切割 log，所以遇到""时，实际是一个换行符
			if (line.equals("")) {
				currentComment.append("\n");
			} else {
				if (currentComment.length() != 0) {
					currentComment.append("\n");
				}

				currentComment.append(line);
			}
		}
	}

}
