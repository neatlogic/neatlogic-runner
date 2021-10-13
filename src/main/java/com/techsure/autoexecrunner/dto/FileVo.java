package com.techsure.autoexecrunner.dto;

/**
 * @author lvzk
 * @since 2021/5/28 13:07
 **/
public class FileVo {
    private String filePath;
    private String fileName;
    private String lastModified;
    private Integer isDirectory;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public Integer getIsDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(Integer isDirectory) {
        this.isDirectory = isDirectory;
    }
}