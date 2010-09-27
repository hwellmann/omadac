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
import org.w3c.dom.*;
import org.w3c.tidy.*;

/**
 * This class is responsible for notification about new version availability. Note that this class
 * does not send any information and merely accesses the <tt>www.gridgain.org</tt> web site for the
 * latest version data.
 * <p>
 * Note also that this connectivity is not necessary to successfully start the system as it will
 * gracefully ignore any errors occurred during notification and verification process.
 * See {@link #HTTP_URL} for specific access URL used.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridUpdateNotifier {
    /*
     * *********************************************************
     * DO NOT CHANGE THIS URL OR HOW IT IS PUT IN ONE LINE.    *
     * THIS URL IS HANDLED BY POST-BUILD PROCESS AND IT HAS TO *
     * BE PLACED EXACTLY HOW IT IS SHOWING.                    *
     * *********************************************************
     */
    /** Access URL to be used to access latest version data. */
    private static final String HTTP_URL =
        "http://www.gridgain.org/update_status.php";

    /** Ant-enhanced system version. */
    private static final String VER = "2.1.1";

    /** Ant-augmented build number. */
    private static final String BUILD = "26022009";

    /** Asynchronous checked. */
    private GridRunnable checker = null;

    /** Latest version. */
    private volatile String latestVer = null;

    /** HTML parsing helper. */
    private final Tidy tidy;

    /** Grid name. */
    private final String gridName;
    
    /**  Whether or not to report only new version. */
    private final boolean reportOnlyNew;

    /**
     * Creates new notifier with default values.
     *
     * @param gridName gridName
     * @param reportOnlyNew Whether or not to report only new version.
     */
    GridUpdateNotifier(String gridName, boolean reportOnlyNew) {
        tidy = new Tidy();

        tidy.setQuiet(true);
        tidy.setOnlyErrors(true);
        tidy.setShowWarnings(false);
        tidy.setCharEncoding(Configuration.UTF8);

        this.gridName = gridName;
        this.reportOnlyNew = reportOnlyNew;
    }

    /**
     * Starts asynchronous process for retrieving latest version data from {@link #HTTP_URL}.
     *
     * @param exec Executor service.
     * @param log Logger.
     */
    void requestStatus(ExecutorService exec, GridLogger log) {
        assert log != null : "ASSERTION [line=103, file=src/java/org/gridgain/grid/kernal/GridUpdateNotifier.java]";

        log = log.getLogger(getClass());

        if (checker == null) {
            try {
                exec.execute(checker = new UpdateChecker(log));
            }
            catch (RejectedExecutionException e) {
                log.error("Failed to schedule a thread due to execution rejection (safely ignoring): " +
                    e.getMessage());
            }
        }
    }

    /**
     * Logs out latest version notification if such was received and available.
     *
     * @param log Logger.
     */
    void reportStatus(GridLogger log) {
        assert log != null : "ASSERTION [line=124, file=src/java/org/gridgain/grid/kernal/GridUpdateNotifier.java]";

        log = log.getLogger(getClass());

        GridUtils.cancel(checker);
        GridUtils.join(checker, log);

        String latestVer = this.latestVer;

        if (latestVer != null) {
            if (latestVer.equals(VER + '-' + BUILD) == true) {
                if (reportOnlyNew == false && log.isInfoEnabled() == true) {
                    log.info("Your version is up to date.");
                }
            }
            else {
                log.warning("New version is available at www.gridgain.org: " + latestVer);
            }

        }
        else {
            if (reportOnlyNew == false) {
                log.warning("Update status is not available.");
            }
        }
    }

    /**
     * Asynchronous checker of the latest version available.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private class UpdateChecker extends GridRunnable {
        /** Logger. */
        private final GridLogger log;

        /**
         * Creates checked with given logger.
         *
         * @param log Logger.
         */
        UpdateChecker(GridLogger log) {
            super(gridName, "grid-version-checker", log);

            this.log = log.getLogger(getClass());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void body() throws InterruptedException {
            try {
                URLConnection conn = new URL(HTTP_URL + (HTTP_URL.endsWith(".php") ? '?' : '&') + "p=" + gridName)
                    .openConnection();

                if (isCancelled() == true) {
                    return;
                }

                // Timeout after 3 seconds.
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                InputStream in = null;

                Document dom = null;

                try {
                    in = conn.getInputStream();

                    if (in == null) {
                        throw new IOException("Failed to open connection to: " + HTTP_URL);
                    }

                    dom = tidy.parseDOM(in, null);
                }
                finally {
                    GridUtils.close(in, log);
                }

                if (dom != null) {
                    latestVer = obtainVersionFrom(dom);
                }
            }
            catch (IOException e) {
                log.warning("Failed to receive update information from (safely ignoring): " + e.getMessage());
            }
        }

        /**
         * Gets the version from the current <tt>node</tt>, if one exists.
         *
         * @param node W3C DOM node.
         * @return Version or <tt>null</tt> if one's not found.
         */
        private String obtainVersionFrom(org.w3c.dom.Node node) {
            assert node != null : "ASSERTION [line=221, file=src/java/org/gridgain/grid/kernal/GridUpdateNotifier.java]";

            if (node instanceof Element == true && "meta".equals(node.getNodeName().toLowerCase()) == true) {
                Element meta = (Element)node;

                String name = meta.getAttribute("name");

                if ("version".equals(name) == true) {
                    String content = meta.getAttribute("content");

                    if (content != null && content.length() > 0) {
                        return content;
                    }
                }
            }

            NodeList childNodes = node.getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                String ver = obtainVersionFrom(childNodes.item(i));

                if (ver != null) {
                    return ver;
                }
            }

            return null;
        }
    }
}
