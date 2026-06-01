/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
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
