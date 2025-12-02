package com.ment.chat.client.utils;

public class Utilities {

    /**
     * To avoid logging of SSL information like cipher suites and ssl timeouts.
     */
    public static void clearSslLogging() {
        // Remove JSSE debug if set via JVM arg
        System.clearProperty("javax.net.debug");

        // Lower java.util.logging verbosity for JSSE internals
        java.util.logging.Logger.getLogger("sun.security.ssl")
                .setLevel(java.util.logging.Level.WARNING);
        java.util.logging.Logger.getLogger("javax.net.ssl")
                .setLevel(java.util.logging.Level.WARNING);
    }
}
