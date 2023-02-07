package com.zhongzhi.empty.util;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;

@Slf4j
public final class IOUtils {
    private IOUtils() {
    }

    public static void close(Closeable... cs) {
        if (cs == null) {
            return;
        }

        for (Closeable c : cs) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    log.error("关闭流异常", e);
                }
            }
        }
    }

    public static void close(HttpURLConnection conn) {
        try {
            if (conn != null) {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.error("关闭连接异常", e);
        }
    }
}
