/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
 */
package com.techsure.autoexecrunner.codehub.dto.diff;

/**
 * Represents a range of line numbers that spans a window on a text file.
 *
 * @author Tom Hombergs <tom.hombergs@gmail.com>
 */
public class Range {

    private final int lineStart;

    private final int lineCount;

    public Range(int lineStart, int lineCount) {
        this.lineStart = lineStart;
        this.lineCount = lineCount;
    }

    /**
     * The line number at which this range starts (inclusive).
     *
     * @return the line number at which this range starts.
     */
    public int getLineStart() {
        return lineStart;
    }

    /**
     * The count of lines in this range.
     *
     * @return the count of lines in this range.
     */
    public int getLineCount() {
        return lineCount;
    }

    @Override
    public String toString() {
    	return lineStart + "," + lineCount;
    }
}
