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
package com.neatlogic.autoexecrunner.codehub.diff.parser;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link ResizingParseWindow} slides through the lines of a input stream and
 * offers methods to get the currently focused line as well as upcoming lines.
 * It is backed by an automatically resizing {@link LinkedList}
 *
 * @author Tom Hombergs <tom.hombergs@gmail.com>
 */
public class ResizingParseWindow implements ParseWindow {

	private BufferedReader reader;

	private LinkedList<String> lineQueue = new LinkedList<>();

	private int lineNumber = 0;

	private List<Pattern> ignorePatterns = new ArrayList<>();

	private boolean isEndOfStream = false;

	public ResizingParseWindow(InputStream in) {
		Reader unbufferedReader = new InputStreamReader(in);
		this.reader = new BufferedReader(unbufferedReader);
	}

	public void addIgnorePattern(String ignorePattern) {
		this.ignorePatterns.add(Pattern.compile(ignorePattern));
	}

	@Override
	public String getFutureLine(int distance) {
		try {
			resizeWindowIfNecessary(distance + 1);
			return lineQueue.get(distance);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public void addLine(int pos, String line) {
		lineQueue.add(pos, line);
	}

	/**
	 * Resizes the sliding window to the given size, if necessary.
	 *
	 * @param newSize the new size of the window (i.e. the number of lines in the
	 *                window).
	 */
	private void resizeWindowIfNecessary(int newSize) {
		try {
			int numberOfLinesToLoad = newSize - this.lineQueue.size();
			for (int i = 0; i < numberOfLinesToLoad; i++) {
				String nextLine = getNextLine();
				if (nextLine != null) {
					lineQueue.addLast(nextLine);
				} else {
					throw new IndexOutOfBoundsException("End of stream has been reached!");
				}
			}
		} catch (IOException e) {
			throw new ApiRuntimeException(e);
		}
	}

	@Override
	public String slideForward() {
		try {
			lineQueue.pollFirst();
			lineNumber++;
			if (lineQueue.isEmpty()) {
				String nextLine = getNextLine();
				if (nextLine != null) {
					lineQueue.addLast(nextLine);
				}
				
				return nextLine;
			} else {
				return lineQueue.peekFirst();
			}
		} catch (IOException e) {
			throw new ApiRuntimeException(e);
		}
	}

	private String getNextLine() throws IOException {
		String nextLine = reader.readLine();
		while (matchesIgnorePattern(nextLine)) {
			nextLine = reader.readLine();
		}
		return getNextLineOrVirtualBlankLineAtEndOfStream(nextLine);
	}

	/**
	 * Guarantees that a virtual blank line is injected at the end of the input
	 * stream to ensure the parser attempts to transition to the {@code END} state,
	 * if necessary, when the end of stream is reached.
	 */
	private String getNextLineOrVirtualBlankLineAtEndOfStream(String nextLine) {
		if ((nextLine == null) && !isEndOfStream) {
			isEndOfStream = true;
			return "";
		}
		
		return nextLine;
	}

	private boolean matchesIgnorePattern(String line) {
		if (line == null) {
			return false;
		} else {
			for (Pattern pattern : ignorePatterns) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches()) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public String getFocusLine() {
		return lineQueue.element();
	}

	@Override
	public int getFocusLineNumber() {
		return lineNumber;
	}
}
