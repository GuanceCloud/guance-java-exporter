package com.guance.exporter.guance.utils;

import java.net.InetAddress;

public class GuanceUtils {

    private static String hostName;

    public static String getHostName() {
        try {
            if (hostName == null) {
                hostName = InetAddress.getLocalHost().getHostName();
            }
        } catch (Exception e) {
            hostName = "UNKNOWN";
        }

        return hostName;
    }
}
