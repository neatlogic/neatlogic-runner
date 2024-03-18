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
package com.neatlogic.autoexecrunner.codehub.dto.diff;

/**
 * Represents a line of a Diff. A line is either contained in both files ("neutral"), only in the first file ("from"),
 * or only in the second file ("to").
 *
 * @author Tom Hombergs <tom.hombergs@gmail.com>
 */
public class Line {

    /**
     * All possible types a line can have.
     */
    public enum LineType {

        /**
         * This line is only contained in the first file of the Diff (the "from" file).
         */
        FROM,

        /**
         * This line is only contained in the second file of the Diff (the "to" file).
         */
        TO,

        /**
         * This line is contained in both filed of the Diff, and is thus considered "neutral".
         */
        NEUTRAL

    }

    private final LineType lineType;

    private final String content;

    public Line(LineType lineType, String content) {
        this.lineType = lineType;
        this.content = content;
    }

    /**
     * The type of this line.
     *
     * @return the type of this line.
     */
    public LineType getLineType() {
        return lineType;
    }

    /**
     * The actual content of the line as String.
     *
     * @return the actual line content.
     */
    public String getContent() {
        return content;
    }

	@Override
	public String toString() {
		if (lineType == LineType.FROM) {
			return "-" + content;
		} else if (lineType == LineType.TO) {
			return "+" + content;
		} else {
			return content;
		}
	}
}
