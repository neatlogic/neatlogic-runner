package com.neatlogic.autoexecrunner.util;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.newsclub.net.unix.AFUNIXDatagramSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;

public class SocketUtil {
    protected static final Log logger = LogFactory.getLog(SocketUtil.class);

    /**
     *
     * @param pathToSocket socket文件路径
     * @param informParam 通知参数
     */
    public static void WriteAFUNIXDatagramSocket(String pathToSocket, JSONObject informParam) {
        File socketFile = new File(pathToSocket);
        //runner 不一定存在socket文件，不存在默认无需inform
        if(socketFile.exists()) {
            try (AFUNIXDatagramSocket sock = AFUNIXDatagramSocket.newInstance()) {
                sock.connect(AFUNIXSocketAddress.of(socketFile));
                byte[] buff = informParam.toJSONString().getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);
                ByteBuffer bb = ByteBuffer.wrap(buff);
                sock.getChannel().write(bb);
                //sock.send(datagramPacket);
            } catch (Exception ex) {
                logger.info(ex.getMessage(), ex);
                throw new ApiRuntimeException(ex.getMessage());
            }
        }
    }
}
