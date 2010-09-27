/*
 * GRIDGAIN - OPEN CLOUD PLATFORM.
 * COPYRIGHT (C) 2005-2008 GRIDGAIN SYSTEMS. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE AS PUBLISHED BY THE FREE SOFTWARE FOUNDATION; EITHER
 * VERSION 2.1 OF THE LICENSE, OR (AT YOUR OPTION) ANY LATER
 * VERSION.
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  SEE THE
 * GNU LESSER GENERAL PUBLIC LICENSE FOR MORE DETAILS.
 *
 * YOU SHOULD HAVE RECEIVED A COPY OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE ALONG WITH THIS LIBRARY; IF NOT, WRITE TO THE FREE
 * SOFTWARE FOUNDATION, INC., 51 FRANKLIN ST, FIFTH FLOOR, BOSTON, MA
 * 02110-1301 USA
 */

package org.gridgain.grid.kernal;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.runnable.*;

/**
 * This class run some basic network diagnostic tests in the background and
 * reports errors or suspicious results.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
final class GridDiagnostic {
    /** */
    private static final int REACH_TIMEOUT = 2000;

    /**
     * Ensure singleton.
     */
    private GridDiagnostic() {
        // No-op.
    }

    /**
     *
     * @param gridName Grid instance name. Can be <tt>null</tt>.
     * @param exec Executor service.
     * @param parentLog Parent logger.
     */
    static void runBackgroundCheck(String gridName, ExecutorService exec, GridLogger parentLog) {
        assert exec != null : "ASSERTION [line=56, file=src/java/org/gridgain/grid/kernal/GridDiagnostic.java]";
        assert parentLog != null : "ASSERTION [line=57, file=src/java/org/gridgain/grid/kernal/GridDiagnostic.java]";

        final GridLogger log = parentLog.getLogger(GridDiagnostic.class);

        try {
            exec.execute(new GridRunnable(gridName, "grid-diagnostic-1", log) {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void body() throws InterruptedException {
                    try {
                        InetAddress localHost = GridNetworkHelper.getLocalHost();

                        if (localHost.isReachable(REACH_TIMEOUT) == false) {
                            log.warning("Default local host is unreachable. This may lead to delays on " +
                                "grid network operations. Check your OS network setting to correct it.");
                        }
                    }
                    catch (IOException e) {
                        log.warning("Failed to perform network diagnostics. It is usually caused by serious " +
                            "network configuration problem. Check your OS network setting to correct it. ", e);
                    }
                }
            });

            exec.execute(new GridRunnable(gridName, "grid-diagnostic-2", log) {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void body() throws InterruptedException {
                    try {
                        InetAddress localHost = GridNetworkHelper.getLocalHost();

                        if (localHost.isLoopbackAddress() == true) {
                            log.warning("Default local host is a loopback address. This can be a sign of " +
                                "potential network configuration problem.");
                        }
                    }
                    catch (IOException e) {
                        log.warning("Failed to perform network diagnostics. It is usually caused by serious " +
                            "network configuration problem. Check your OS network setting to correct it. ", e);
                    }
                }
            });

            exec.execute(new GridRunnable(gridName, "grid-diagnostic-3", log) {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void body() throws InterruptedException {
                    String jdkStrLow = GridOs.getJdkString().toLowerCase();

                    if (jdkStrLow.contains("jrockit") == true && jdkStrLow.contains("1.5.") == true) {
                        log.warning("BEA JRockit VM ver. 1.5.x has shown problems with NIO functionality in our " +
                            "tests that were not reproducible in other VMs. We recommend using Sun VM. Should you " +
                            "have further questions please contact us at support@gridgain.com");
                    }
                }
            });

            exec.execute(new GridRunnable(gridName, "grid-diagnostic-4", log) {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void body() throws InterruptedException {
                    // Sufficiently tested OS.
                    if (GridOs.isSufficientlyTestedOs() == false) {
                        log.warning("This operating system has been tested less rigorously: " + GridOs.getOsString() +
                            ". Our team will appreciate the feedback if you experience any problems running " +
                            "gridgain in this environment. You can always send your feedback to support@gridgain.com");
                    }
                }
            });

            exec.execute(new GridRunnable(gridName, "grid-diagnostic-5", log) {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void body() throws InterruptedException {
                    if (GridOs.isWindows() == true && System.getProperty("com.sun.management.jmxremote") != null) {
                        log.warning("If you are using standard JDK JConsole on Windows you may encounter an " +
                            "undocumented behavior when you cannot locally connect to the JConsole if you logged " +
                            "in into Windows domain. In this case you will need to connect locally as 'remote' (with " +
                            "deafult port 49112). The issue " +
                            "may also be present on Windows Vista without logging into Windows domain. For more " +
                            "information on troubleshooting see the following link: " +
                            "http://java.sun.com/j2se/1.5.0/docs/guide/management/faq.html");
                    }

                    if (GridOs.isWindowsVista() == true && System.getProperty("com.sun.management.jmxremote") != null) {
                        log.warning("Windows Vista has known problem supporting elevated mode of running Java " +
                            "application and JConsole. For more information and the status of the Sun's bug " +
                            "see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6529265");
                    }
                }
            });
        }
        catch (RejectedExecutionException e) {
            log.error("Failed to start background network diagnostics check due to thread pool execution " +
                "rejection. In most cases it indicates a severe configuration problem with GridGain.", e);
        }
    }
}
