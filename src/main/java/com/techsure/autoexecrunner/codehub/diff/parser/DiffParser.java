/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
 */
package com.techsure.autoexecrunner.codehub.diff.parser;

import com.techsure.autoexecrunner.codehub.dto.diff.FileDiffInfo;

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
