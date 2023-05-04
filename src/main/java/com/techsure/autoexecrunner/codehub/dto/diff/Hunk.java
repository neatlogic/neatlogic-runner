/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
 */
package com.techsure.autoexecrunner.codehub.dto.diff;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a "hunk" of changes made to a file.
 * <p/>
 * A Hunk consists of one or more lines that either exist only in the first file
 * ("from line"), only in the second file ("to line") or in both files ("neutral
 * line"). Additionally, it contains information about which excerpts of the
 * compared files are compared in this Hunk in the form of line ranges.
 *
 * @author Tom Hombergs <tom.hombergs@gmail.com>
 */
public class Hunk {

	private Range fromFileRange;

	private Range toFileRange;

	private List<Line> lines = new ArrayList<Line>();

	/**
	 * The range of line numbers that this Hunk spans in the first file of the Diff.
	 *
	 * @return range of line numbers in the first file (the "from" file).
	 */
	public Range getFromFileRange() {
		return fromFileRange;
	}

	/**
	 * The range of line numbers that this Hunk spans in the second file of the
	 * Diff.
	 *
	 * @return range of line numbers in the second file (the "to" file).
	 */
	public Range getToFileRange() {
		return toFileRange;
	}

	/**
	 * The lines that are part of this Hunk.
	 *
	 * @return lines of this Hunk.
	 */
	public List<Line> getLines() {
		return lines;
	}

	public void setFromFileRange(Range fromFileRange) {
		this.fromFileRange = fromFileRange;
	}

	public void setToFileRange(Range toFileRange) {
		this.toFileRange = toFileRange;
	}

	public void setLines(List<Line> lines) {
		this.lines = lines;
	}

	public Line getLatestLine() {
		Line line = null;
		if (lines.size() > 1) {
			line = lines.get(lines.size() - 1);
		}
		return line;
	}

	public void removeLatestLine() {
		if (lines.size() > 1) {
			lines.remove(lines.size() - 1);
		}
	}

	@Override
	public String toString() {
		String content = "@@ -" + fromFileRange + " +" + toFileRange + " @@\n";
		for (Line line : lines) {
			content = content + line + "\n";
		}
		return content;
	}
}
