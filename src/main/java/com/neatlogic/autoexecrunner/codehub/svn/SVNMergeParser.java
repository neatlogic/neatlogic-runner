package com.neatlogic.autoexecrunner.codehub.svn;



import com.neatlogic.autoexecrunner.codehub.constvalue.MergeFileStatus;
import com.neatlogic.autoexecrunner.codehub.dto.merge.MergeFileEntry;
import com.neatlogic.autoexecrunner.codehub.dto.merge.MergeResultInfo;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SVNMergeParser {

    private static final int GET_HEADER = 1;
    private static final int GET_FILE = 2;
    private static final int GET_COMMENT = 3;
    private static final int GET_SUMMARY = 4;
    private static final int GET_ERROR = 5;
    boolean isConflict = false;
    private int currentIndex = 0;
    private String[] logs = null;
    private int status = GET_HEADER;
    private List<MergeFileEntry> mergeEntryList = new ArrayList<MergeFileEntry>();;
    private StringBuilder errorLog = new StringBuilder();
    private StringBuilder summaryLog = new StringBuilder();

    public MergeResultInfo parse(String logStr) {
//        System.out.println(logStr);
        MergeResultInfo mergeResultInfo = new MergeResultInfo();
        if (logStr == null || logStr.equals("")) {
            // 已经合并过, 命令行无输出
            return mergeResultInfo;
        }

        logs = logStr.split("(\r\n|\n)");

        String line = null;

        while (currentIndex < logs.length) {
            line = logs[currentIndex].trim();
            String[] flags = line.split("\\s+", 2);  // flags+file
            // 最容易出错的地方, 注释信息的特征字符串, 目前只遇到这几种
            if (line.startsWith("--- ") || line.startsWith("Searching ") || line.startsWith("Checking ")) {
                status = GET_COMMENT;
                processGetComment(line);
            } else if ((flags.length == 2 && flags[0].length() <= 5 )
                && (line.charAt(0) == 'A' || line.charAt(0) == 'C' || line.charAt(0) == 'D' || line.charAt(0) == 'M' || line.charAt(0) == 'G' || line.charAt(0) == 'U' || line.charAt(0) == 'R' || line.charAt(0) == 'I')) {
                status = GET_FILE;
                processGetFile(line);
            } else if (line.startsWith("Summary of ")) {
                status = GET_SUMMARY;
                processGetSummary(line);
            } else if (line.startsWith("svn: E")) {
                status = GET_ERROR;
                processGetError(line);
            } else if (StringUtils.isNotBlank(line)) {
                if (status == GET_ERROR) {
                    processGetError(line);
                } else if (status == GET_SUMMARY) {
                    processGetSummary(line);
                } else {
                    System.err.println(line);
                    throw new ApiRuntimeException("parse command failed," + logStr);
                }
            }

            currentIndex++;

        }

        mergeResultInfo.setMergeFileEntrys(mergeEntryList);
        mergeResultInfo.setConflict(isConflict);
        mergeResultInfo.setError(errorLog.toString());
        mergeResultInfo.setSummary(summaryLog.toString());
        return mergeResultInfo;
    }

    private void processGetSummary(String line) {
        summaryLog.append(line).append("\n");
    }

    private void processGetFile(String line) {
        String[] data = line.split("\\s+", 2);
        String filePath = data[1];
        if (StringUtils.equals(filePath, ".")) {
            // TODO 分支目录本身的变更不记录
            return;
        }
        // 这里第一列应该是内容, 第二列则是属性修改, 不过不重要只要能识别到冲突就行
        MergeFileEntry mergeFileEntry = new MergeFileEntry(filePath);
        String flag = data[0];
        if (flag.contains("A")) {
            mergeFileEntry.setMergeStatus(MergeFileStatus.ADDED);
        } else if (flag.contains("C")) {
            mergeFileEntry.setMergeStatus(MergeFileStatus.CONFLICTED);
            isConflict = true;
            mergeFileEntry.setConflict(true);
        } else if (flag.contains("D")) {
            mergeFileEntry.setMergeStatus(MergeFileStatus.DELETED);
        } else if (flag.contains("M") || flag.contains("U")) {
            mergeFileEntry.setMergeStatus(MergeFileStatus.UPDATED);
        } else if (flag.contains("R")) {
            mergeFileEntry.setMergeStatus(MergeFileStatus.REPLACED);
        } else if (flag.contains("I")) {
            mergeFileEntry.setMergeStatus(MergeFileStatus.UNTRACKED); // ??
        } else if (flag.contains("G")) {
            mergeFileEntry.setMergeStatus(MergeFileStatus.MERGED);
        } 
        mergeEntryList.add(mergeFileEntry);
    }

    private void processGetComment(String line) {
//        System.out.println(line);
    }

    private void processGetError(String line) {
        errorLog.append(line).append("\n");
    }

    public static void main(String[] args) {
        String logStr = "--- Merging r16279 into '.':\n" +
                "UG   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/agentcommanagement/service/impl/BkAgentComManagementServiceImpl.java\n" +
                " G   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/agentcommanagement\n" +
                "U    dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/chargemanagement/controller/BkLaChargeSumController.java\n" +
                "U    dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/chargemanagement/export/BkLaChargeSumImport.java\n" +
                " G   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/chargemanagement\n" +
                "C    dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/wagemanagement/service/impl/BkLaControlMatrixServiceImpl.java\n" +
                " G   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/wagemanagement\n" +
                " G   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms\n" +
                "U    dj-cms-cloud/lis_cms_cloud_common/src/main/java/com/sinosoft/lis/entity/BkLaComEffectiveEntity.java\n" +
                " G   dj-cms-cloud/lis_cms_cloud_common/src/main/java/com/sinosoft/lis/entity\n" +
                "U    dj-cms-cloud/lis_cms_cloud_datachange/src/main/java/com/sinosoft/lis/bank/commission/pull/service/impl/BkLaCommissionChargeIfaceServiceImpl.java\n" +
                " G   dj-cms-cloud/lis_cms_cloud_datachange/src/main/java/com/sinosoft/lis/bank/commission/pull\n" +
                "U    dj-cms-cloud/lis_cms_cloud_datachange/src/main/java/com/sinosoft/lis/bkchargecalculate/dao/BkLaCommissionChargeDao.java\n" +
                "U    dj-cms-cloud/lis_cms_cloud_datachange/src/main/java/com/sinosoft/lis/bkchargecalculate/dao/BkLaCommissionChargeDao.xml\n" +
                " G   dj-cms-cloud\n" +
                "U    dj-cms-vue/src/views/modules/bank/servicechargemanagement/freezemanagement.vue\n" +
                " G   dj-cms-vue/src/views/modules/bank/servicechargemanagement\n" +
                " G   dj-cms-vue/src/views/modules/bank\n" +
                " G   .\n" +
                "--- Recording mergeinfo for merge of r16279 into '.':\n" +
                " G   .\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-cloud':\n" +
                " G   dj-cms-cloud\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms':\n" +
                " G   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/agentcommanagement':\n" +
                " G   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/agentcommanagement\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/agentcommanagement/service/impl/BkAgentComManagementServiceImpl.java':\n" +
                " G   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/agentcommanagement/service/impl/BkAgentComManagementServiceImpl.java\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/chargemanagement':\n" +
                " G   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/chargemanagement\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/wagemanagement':\n" +
                " G   dj-cms-cloud/lis-cms-cloud-bank/src/main/java/com/sinosoft/lis/cms/wagemanagement\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-cloud/lis_cms_cloud_common/src/main/java/com/sinosoft/lis/entity':\n" +
                " G   dj-cms-cloud/lis_cms_cloud_common/src/main/java/com/sinosoft/lis/entity\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-cloud/lis_cms_cloud_datachange/src/main/java/com/sinosoft/lis/bank/commission/pull':\n" +
                " G   dj-cms-cloud/lis_cms_cloud_datachange/src/main/java/com/sinosoft/lis/bank/commission/pull\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-vue/src/views/modules/bank':\n" +
                " G   dj-cms-vue/src/views/modules/bank\n" +
                "--- Recording mergeinfo for merge of r16279 into 'dj-cms-vue/src/views/modules/bank/servicechargemanagement':\n" +
                " G   dj-cms-vue/src/views/modules/bank/servicechargemanagement\n" +
                "Summary of conflicts:\n" +
                "  Text conflicts: 1";

        new SVNMergeParser().parse(logStr);
    }

}
