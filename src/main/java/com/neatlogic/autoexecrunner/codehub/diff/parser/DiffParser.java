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

import com.neatlogic.autoexecrunner.codehub.dto.diff.FileDiffInfo;

import java.io.InputStream;
import java.util.List;

/**
 * Interface to a parser that parses a textual diff between two text files. See the javadoc of the implementation you want to use to see
 * what diff format it is expecting as input.
 *
 * @author Tom Hombergs <tom.hombergs@gmail.com>
 */
public interface DiffParser {

    FileDiffInfo parseSingle(InputStream in);
    
    List<FileDiffInfo> parse(InputStream in);

}
