package com.neatlogic.autoexecrunner.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.RequestContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.constvalue.SystemUser;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ResourcepoolPingService {

    private static final Logger logger = LoggerFactory.getLogger(ResourcepoolPingService.class);
    private static final Set<String> RUNNING_TASK_SET = ConcurrentHashMap.newKeySet();
    private static final int MAX_SCAN_CONCURRENCY = 2;
    private static final int SCAN_QUEUE_SIZE = 100;
    private static final int MAX_THREAD_COUNT = 5;
    private static final int IP_PER_BATCH = 1024;
    private static final int MAX_SYNC_IP_COUNT = 4096;
    private static final int PING_TIMEOUT_SECONDS = 3;
    private static final int PING_TIMEOUT_MILLISECONDS = PING_TIMEOUT_SECONDS * 1000;
    private static final long ASYNC_BATCH_INTERVAL_MS = 100L;
    private static final AtomicInteger SCAN_THREAD_INDEX = new AtomicInteger(1);
    private static final AtomicInteger PING_THREAD_INDEX = new AtomicInteger(1);
    private static final ThreadPoolExecutor SCAN_EXECUTOR = new ThreadPoolExecutor(
            1,
            MAX_SCAN_CONCURRENCY,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(SCAN_QUEUE_SIZE),
            newNamedDaemonThreadFactory("RESOURCEPOOL-SCAN-", SCAN_THREAD_INDEX),
            new ThreadPoolExecutor.AbortPolicy()
    );
    private static final ExecutorService PING_EXECUTOR = new ThreadPoolExecutor(
            MAX_THREAD_COUNT,
            MAX_THREAD_COUNT,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(MAX_THREAD_COUNT * 20),
            newNamedDaemonThreadFactory("RESOURCEPOOL-PING-", PING_THREAD_INDEX),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public JSONObject ping(List<String> ipList, Boolean async, String callbackUrl, String tenant) {
        ipList = normalizeIpList(ipList);
        if (CollectionUtils.isEmpty(ipList)) {
            throw new ApiRuntimeException("ipList不能为空");
        }
        String taskName = "PING-IPLIST-" + Integer.toHexString(ipList.hashCode());
        if (!RUNNING_TASK_SET.add(taskName)) {
            throw new ApiRuntimeException("IP列表正在检测中");
        }
        return doPingIpList(ipList, taskName, async, callbackUrl, tenant);
    }

    public JSONObject ping(String network, Integer mask, Boolean async, String callbackUrl, String tenant) {
        if (StringUtils.isBlank(network)) {
            throw new ApiRuntimeException("network不能为空");
        }
        if (mask == null || mask < 1 || mask > 32) {
            throw new ApiRuntimeException("mask必须在1-32之间");
        }
        Ipv4Network ipv4Network = new Ipv4Network(network, mask);
        String taskName = "PING-THREAD-" + ipv4Network.getNetworkId() + "/" + mask;
        if (!RUNNING_TASK_SET.add(taskName)) {
            throw new ApiRuntimeException("网段：" + ipv4Network.getNetworkId() + "/" + mask + "正在检测中");
        }
        return doPingNetwork(ipv4Network, taskName, async, callbackUrl, tenant);
    }

    private JSONObject doPingIpList(List<String> ipList, String taskName, Boolean async, String callbackUrl, String tenant) {
        boolean asyncFlag = Boolean.TRUE.equals(async);
        JSONObject result = new JSONObject();
        if (asyncFlag) {
            if (StringUtils.isBlank(callbackUrl)) {
                RUNNING_TASK_SET.remove(taskName);
                throw new ApiRuntimeException("异步ping需要callbackUrl");
            }
            executeScanTask(taskName, () -> {
                try {
                    doPingIpListByBatch(ipList, taskName, callbackUrl, tenant);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    RUNNING_TASK_SET.remove(taskName);
                }
            });
            result.put("message", "已成功发起异步ping");
            fillScanQueueInfo(result);
            return result;
        }

        try {
            if (ipList.size() > MAX_SYNC_IP_COUNT) {
                throw new ApiRuntimeException("同步ping最多支持" + MAX_SYNC_IP_COUNT + "个IP，请使用异步方式");
            }
            JSONArray pingStatusArray = pingIpList(ipList, taskName);
            result.put("pingStatusArray", pingStatusArray);
            return result;
        } finally {
            RUNNING_TASK_SET.remove(taskName);
        }
    }

    private JSONObject doPingNetwork(Ipv4Network ipv4Network, String taskName, Boolean async, String callbackUrl, String tenant) {
        boolean asyncFlag = Boolean.TRUE.equals(async);
        JSONObject result = new JSONObject();
        if (asyncFlag) {
            if (StringUtils.isBlank(callbackUrl)) {
                RUNNING_TASK_SET.remove(taskName);
                throw new ApiRuntimeException("异步ping需要callbackUrl");
            }
            executeScanTask(taskName, () -> {
                try {
                    doPingNetworkByBatch(ipv4Network, taskName, callbackUrl, tenant);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    RUNNING_TASK_SET.remove(taskName);
                }
            });
            result.put("message", "已成功发起异步ping");
            result.put("ipCount", ipv4Network.getAvailableIpCount());
            result.put("batchSize", IP_PER_BATCH);
            fillScanQueueInfo(result);
            return result;
        }

        try {
            long ipCount = ipv4Network.getAvailableIpCount();
            if (ipCount > MAX_SYNC_IP_COUNT) {
                throw new ApiRuntimeException("同步ping最多支持" + MAX_SYNC_IP_COUNT + "个IP，请使用异步方式");
            }
            JSONArray pingStatusArray = new JSONArray();
            ipv4Network.forEachAvailableIpBatch(IP_PER_BATCH, batch -> pingStatusArray.addAll(pingIpList(batch, taskName)));
            result.put("pingStatusArray", pingStatusArray);
            return result;
        } finally {
            RUNNING_TASK_SET.remove(taskName);
        }
    }

    public JSONObject ping(JSONObject paramObj) {
        String network = paramObj.getString("network");
        Integer mask = paramObj.getInteger("mask");
        Boolean async = paramObj.getBoolean("async");
        String callbackUrl = paramObj.getString("callbackUrl");
        String tenant = paramObj.getString("tenant");
        List<String> ipList = getParamIpList(paramObj);
        if (StringUtils.isBlank(tenant) && RequestContext.get() != null) {
            HttpServletRequest request = RequestContext.get().getRequest();
            if (request != null) {
                tenant = request.getHeader("Tenant");
            }
        }
        if (CollectionUtils.isNotEmpty(ipList)) {
            return ping(ipList, async, callbackUrl, tenant);
        }
        return ping(network, mask, async, callbackUrl, tenant);
    }

    private List<String> getParamIpList(JSONObject paramObj) {
        List<String> ipList = new ArrayList<>();
        String ip = paramObj.getString("ip");
        if (StringUtils.isNotBlank(ip)) {
            ipList.add(ip);
        }
        JSONArray paramIpList = paramObj.getJSONArray("ipList");
        if (CollectionUtils.isNotEmpty(paramIpList)) {
            for (int i = 0; i < paramIpList.size(); i++) {
                String value = paramIpList.getString(i);
                if (StringUtils.isNotBlank(value)) {
                    ipList.add(value);
                }
            }
        }
        return ipList;
    }

    private List<String> normalizeIpList(List<String> ipList) {
        if (CollectionUtils.isEmpty(ipList)) {
            return Collections.emptyList();
        }
        Set<String> resultSet = new LinkedHashSet<>();
        for (String ip : ipList) {
            String value = StringUtils.trim(ip);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            if (!Ipv4Network.isIpv4(value) && !isIpv6(value)) {
                throw new ApiRuntimeException("IP格式不正确：" + value);
            }
            resultSet.add(value);
        }
        return new ArrayList<>(resultSet);
    }

    private static boolean isIpv6(String ip) {
        if (StringUtils.isBlank(ip) || !ip.contains(":")) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(StringUtils.substringBefore(ip, "%"));
            return address instanceof Inet6Address;
        } catch (Exception e) {
            return false;
        }
    }

    private JSONArray pingIpList(List<String> ipList, String taskName) {
        JSONArray pingStatusArray = new JSONArray();
        if (CollectionUtils.isEmpty(ipList)) {
            return pingStatusArray;
        }
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>(ipList);
        List<JSONObject> resultList = Collections.synchronizedList(new ArrayList<>());
        int threadCount = Math.max(1, Math.min(ipList.size(), MAX_THREAD_COUNT));
        threadCount = Math.min(threadCount, MAX_THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            PING_EXECUTOR.execute(new IpPingRunner(latch, queue, resultList, taskName + "-" + i));
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiRuntimeException("ping任务被中断");
        }
        pingStatusArray.addAll(resultList);
        return pingStatusArray;
    }

    private void doPingIpListByBatch(List<String> ipList, String taskName, String callbackUrl, String tenant) {
        for (int start = 0; start < ipList.size(); start += IP_PER_BATCH) {
            int end = Math.min(start + IP_PER_BATCH, ipList.size());
            JSONArray pingStatusArray = pingIpList(ipList.subList(start, end), taskName + "-" + start);
            callback(callbackUrl, tenant, pingStatusArray);
            sleepQuietly(ASYNC_BATCH_INTERVAL_MS);
        }
    }

    private void doPingNetworkByBatch(Ipv4Network ipv4Network, String taskName, String callbackUrl, String tenant) {
        ipv4Network.forEachAvailableIpBatch(IP_PER_BATCH, batch -> {
            JSONArray pingStatusArray = pingIpList(batch, taskName + "-" + batch.get(0));
            callback(callbackUrl, tenant, pingStatusArray);
            sleepQuietly(ASYNC_BATCH_INTERVAL_MS);
        });
    }

    private void callback(String callbackUrl, String tenant, JSONArray pingStatusArray) {
        if (StringUtils.isBlank(callbackUrl)) {
            return;
        }
        String url = buildCallbackUrl(callbackUrl);
        JSONObject payload = new JSONObject();
        payload.put("pingStatusArray", pingStatusArray);
        try {
            HttpRequestUtil requestUtil = HttpRequestUtil.post(url)
                    .setPayload(payload.toJSONString())
                    .setAuthType(AuthenticateType.HMAC)
                    .setUsername(SystemUser.AUTOEXEC.getUserId())
                    .setToken(SystemUser.AUTOEXEC.getToken());
            if (StringUtils.isNotBlank(tenant)) {
                requestUtil.setTenant(tenant);
            }
            requestUtil.sendRequest();
            if (StringUtils.isNotBlank(requestUtil.getError())) {
                logger.error("resourcepool ping callback failed, url: {}, error: {}", url, requestUtil.getError());
            }
        } catch (Exception e) {
            logger.error("resourcepool ping callback failed, url: {}", url, e);
        }
    }

    private String buildCallbackUrl(String callbackUrl) {
        if (StringUtils.startsWithIgnoreCase(callbackUrl, "http://") || StringUtils.startsWithIgnoreCase(callbackUrl, "https://")) {
            return callbackUrl;
        }
        String root = StringUtils.removeEnd(Config.NEATLOGIC_ROOT(), "/");
        if (StringUtils.isBlank(root)) {
            throw new ApiRuntimeException("未配置neatlogic.root，无法生成ping回调地址");
        }
        if (StringUtils.startsWith(callbackUrl, "/")) {
            return root + callbackUrl;
        }
        return root + "/api/rest/" + callbackUrl;
    }

    private static void executeScanTask(String taskName, Runnable runnable) {
        try {
            SCAN_EXECUTOR.execute(runnable);
        } catch (RejectedExecutionException e) {
            RUNNING_TASK_SET.remove(taskName);
            throw new ApiRuntimeException("资源池ping任务队列已满，请稍后重试");
        }
    }

    private static void fillScanQueueInfo(JSONObject result) {
        result.put("maxScanConcurrency", MAX_SCAN_CONCURRENCY);
        result.put("scanActiveCount", SCAN_EXECUTOR.getActiveCount());
        result.put("scanQueueSize", SCAN_EXECUTOR.getQueue().size());
    }

    private static ThreadFactory newNamedDaemonThreadFactory(String prefix, AtomicInteger index) {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName(prefix + index.getAndIncrement());
            return thread;
        };
    }

    private static void sleepQuietly(long millis) {
        if (millis <= 0L) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class IpPingRunner implements Runnable {
        private final CountDownLatch latch;
        private final ConcurrentLinkedQueue<String> queue;
        private final List<JSONObject> resultList;
        private final String threadName;

        private IpPingRunner(CountDownLatch latch, ConcurrentLinkedQueue<String> queue, List<JSONObject> resultList, String threadName) {
            this.latch = latch;
            this.queue = queue;
            this.resultList = resultList;
            this.threadName = threadName;
        }

        @Override
        public void run() {
            String originThreadName = Thread.currentThread().getName();
            try {
                Thread.currentThread().setName(threadName);
                String ip;
                while ((ip = queue.poll()) != null) {
                    JSONObject result = new JSONObject();
                    result.put("ip", ip);
                    result.put("ping", ping(ip) ? 1 : 0);
                    resultList.add(result);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                Thread.currentThread().setName(originThreadName);
                latch.countDown();
            }
        }

        private boolean ping(String ip) {
            Process process = null;
            try {
                ProcessBuilder builder = new ProcessBuilder(buildPingCommand(ip));
                process = builder.redirectErrorStream(true).start();
                if (!process.waitFor(PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    return false;
                }
                return process.exitValue() == 0;
            } catch (Exception e) {
                logger.warn("ping ip {} failed: {}", ip, e.getMessage());
                return false;
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }

        private List<String> buildPingCommand(String ip) {
            List<String> command = new ArrayList<>();
            command.add("ping");
            if (isIpv6(ip)) {
                command.add("-6");
            }
            command.add("-c");
            command.add("1");
            fillPingTimeoutArgs(command);
            command.add(ip);
            return command;
        }

        private void fillPingTimeoutArgs(List<String> command) {
            String osName = StringUtils.defaultString(System.getProperty("os.name")).toLowerCase();
            if (osName.contains("linux")) {
                command.add("-W");
                command.add(String.valueOf(PING_TIMEOUT_SECONDS));
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                command.add("-W");
                command.add(String.valueOf(PING_TIMEOUT_MILLISECONDS));
            } else {
                command.add("-W");
                command.add(String.valueOf(PING_TIMEOUT_SECONDS));
            }
        }
    }

    private static class Ipv4Network {
        private final String ip;
        private final int mask;

        private Ipv4Network(String ip, int mask) {
            if (!isIpv4(ip)) {
                throw new ApiRuntimeException("network格式必须是IPv4地址");
            }
            this.ip = ip;
            this.mask = mask;
        }

        private String getNetworkId() {
            return longToIp(ipToLong(ip) & maskToLong(mask));
        }

        private long getAvailableIpCount() {
            if (mask == 32) {
                return 1L;
            }
            long size = 1L << (32 - mask);
            return Math.max(0L, size - 2L);
        }

        private void forEachAvailableIpBatch(int batchSize, IpBatchConsumer consumer) {
            long networkId = ipToLong(ip) & maskToLong(mask);
            if (mask == 32) {
                List<String> ipList = new ArrayList<>(1);
                ipList.add(longToIp(networkId));
                consumer.accept(ipList);
                return;
            }
            long ipCount = getAvailableIpCount();
            List<String> ipList = new ArrayList<>(Math.min(batchSize, (int) Math.min(ipCount, Integer.MAX_VALUE)));
            for (long i = 1; i <= ipCount; i++) {
                ipList.add(longToIp(networkId + i));
                if (ipList.size() >= batchSize) {
                    consumer.accept(ipList);
                    ipList = new ArrayList<>(Math.min(batchSize, (int) Math.min(ipCount - i, Integer.MAX_VALUE)));
                }
            }
            if (CollectionUtils.isNotEmpty(ipList)) {
                consumer.accept(ipList);
            }
        }

        private static boolean isIpv4(String ip) {
            String[] ipPartArray = StringUtils.split(ip, ".");
            if (ipPartArray == null || ipPartArray.length != 4) {
                return false;
            }
            for (String ipPart : ipPartArray) {
                if (!StringUtils.isNumeric(ipPart)) {
                    return false;
                }
                int value = Integer.parseInt(ipPart);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        }

        private static long ipToLong(String ip) {
            String[] ipPartArray = StringUtils.split(ip, ".");
            long result = 0L;
            for (String ipPart : ipPartArray) {
                result = (result << 8) + Integer.parseInt(ipPart);
            }
            return result;
        }

        private static long maskToLong(int mask) {
            return 0xffffffffL << (32 - mask) & 0xffffffffL;
        }

        private static String longToIp(long value) {
            return ((value >> 24) & 0xff) + "."
                    + ((value >> 16) & 0xff) + "."
                    + ((value >> 8) & 0xff) + "."
                    + (value & 0xff);
        }
    }

    private interface IpBatchConsumer {
        void accept(List<String> ipList);
    }
}
