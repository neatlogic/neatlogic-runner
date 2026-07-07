package com.neatlogic.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.dto.FileVo;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileSafeUtil;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class JobOutPutFileBathDownloadApi extends PrivateBinaryStreamApiComponentBase {
    private static final Log logger = LogFactory.getLog(JobOutPutFileBathDownloadApi.class);

    @Override
    public String getName() {
        return "批量下载作业输出文件";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式")
    })
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        String jobPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder());
        File outputOpRoot = FileSafeUtil.getPath("output-op", jobPath);
        List<FileVo> outputFileVoList = getOutPutFileList(outputOpRoot);
        if(outputFileVoList.size() == 1){
            String fileName = FileUtil.getEncodedFileName(request.getHeader("User-Agent"), outputFileVoList.get(0).getFileName());
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
            File outputFile = FileSafeUtil.getDownloadFile(outputFileVoList.get(0).getFilePath(), outputOpRoot.getPath());
            FileUtil.downloadFileByPath(outputFile.getPath(),response);
        }else {
            String fileName = FileUtil.getEncodedFileName(request.getHeader("User-Agent"), "outputFile." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip");
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
            try (ZipOutputStream zipOs = new ZipOutputStream(response.getOutputStream())) {
                for (FileVo fileVo : outputFileVoList){
                    File outputFile = FileSafeUtil.getDownloadFile(fileVo.getFilePath(), outputOpRoot.getPath());
                    FileInputStream fileInputStream = new FileInputStream(outputFile);
                    byte[] byteStr = new byte[1024 * 1024]; // 每次读取1M
                    int strLength = fileInputStream.read(byteStr);
                    while(strLength != -1) {
                        zipOs.putNextEntry(new ZipEntry(fileVo.getRenameFileName()));
                        zipOs.write(byteStr, 0, strLength);
                        strLength = fileInputStream.read(byteStr);
                    }
                    zipOs.closeEntry();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }


    /**
     * 获取附件列表
     *
     * @param outputOpPath output-op
     * @return 附件列表
     */
    private List<FileVo> getOutPutFileList(File outputOpPath) throws IOException {
        List<FileVo> outputFileList = new ArrayList<>();
        List<FileVo> fileVoList = FileUtil.readFileList(outputOpPath.getPath());
        //循环output-op下的文件夹
        for (FileVo file : fileVoList) {
            if (!isSubPath(outputOpPath, file)) {
                continue;
            }
            List<FileVo> nodeFileTmpList = FileUtil.readFileList(file.getFilePath());
            for (FileVo nodeFile : nodeFileTmpList) {
                if (!isSubPath(outputOpPath, nodeFile)) {
                    continue;
                }
                //循环"文件输出_xxx" 文件夹
                List<FileVo> outputFileTmpList = FileUtil.readFileList(nodeFile.getFilePath());
                outputFileTmpList = outputFileTmpList.stream().filter(o -> isSubPath(outputOpPath, o)).collect(Collectors.toList());
                //如果大于一个输出文件，则附件名则需要带上一个文件夹的名前缀
                if (outputFileTmpList.size() > 1 || (outputFileList.size() > 0 && outputFileTmpList.size() > 0)) {
                    outputFileTmpList = outputFileTmpList.stream().peek(o -> o.setRenameFileName(file.getFileName() + "-" + o.getFileName())).collect(Collectors.toList());
                }
                outputFileList.addAll(outputFileTmpList);
            }
        }
        return outputFileList;
    }

    private boolean isSubPath(File outputOpPath, FileVo fileVo) {
        try {
            return FileSafeUtil.isSubPath(outputOpPath, new File(fileVo.getFilePath()).getCanonicalFile());
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public String getToken() {
        return "/job/output/file/batch/download";
    }
}
