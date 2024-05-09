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

public interface ParseWindow {

    /**
     * Returns the line currently focused by this window. This is actually the
     * same line as returned by {@link #slideForward()} but calling
     * this method does not slide the window forward a step.
     *
     * @return the currently focused line.
     */
    String getFocusLine();

    /**
     * Returns the number of the current line within the whole document.
     *
     * @return the line number.
     */
    int getFocusLineNumber();

    /**
     * Slides the window forward one line.
     *
     * @return the next line that is in the focus of this window or null if the
     * end of the stream has been reached.
     */
    String slideForward();

    /**
     * Looks ahead from the current line and retrieves a line that will be the
     * focus line after the window has slided forward.
     *
     * @param distance the number of lines to look ahead. Must be greater or equal 0.
     *                 0 returns the focus line. 1 returns the first line after the
     *                 current focus line and so on. Note that all lines up to the
     *                 returned line will be held in memory until the window has
     *                 slided past them, so be careful not to look ahead too far!
     * @return the line identified by the distance parameter that lies ahead of
     *         the focus line. Returns null if the line cannot be read because
     *         it lies behind the end of the stream.
     */
    String getFutureLine(int distance);

    void addLine(int pos, String line);
    
}
