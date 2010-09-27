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

package org.gridgain.grid.spi.deployment.uri.scanners.mail;

import java.io.*;
import java.net.*;
import java.util.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.deployment.uri.*;
import org.gridgain.grid.spi.deployment.uri.scanners.*;
import org.gridgain.grid.spi.deployment.uri.util.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.mail.*;
import org.gridgain.grid.util.mail.inbox.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridUriDeploymentMailScanner extends GridUriDeploymentScanner {
    /** */
    static final String DFLT_STORE_FILE_NAME = "grid-email-deploy-msgs.dat";

    /** */
    private static final String MIME_JAR = "application/x-jar";

    /** */
    private static final String MIME_ZIP = "application/zip";

    /** */
    private static final String MIME_OCTETSTREAM = "application/octet-stream";

    /** */
    private final GridMailInbox inbox;

    /** */
    private final GridMailInboxConfiguration cfg;

    /** */
    private String subj = GridUriDeploymentSpi.DFLT_MAIL_SUBJECT;

    /** */
    private boolean allMsgsCheck = false;

    /** */
    private Set<String> msgsFileUidCache = new HashSet<String>();

    /**
     *
     * @param gridName Grid instance name.
     * @param uri Mail URI.
     * @param deployDir Deployment directory.
     * @param freq Scanner frequency.
     * @param filter File name filter.
     * @param listener Deployment filter.
     * @param log Logger to use.
     * @param marshaller Marshaller to marshal and unmarshal objects.
     * @throws GridSpiException Thrown in case of any error.
     */
    public GridUriDeploymentMailScanner(
        String gridName,
        URI uri,
        File deployDir,
        long freq,
        FilenameFilter filter,
        GridUriDeploymentScannerListener listener,
        GridLogger log,
        GridMarshaller marshaller) throws GridSpiException {
        super(gridName, uri, deployDir, freq, filter, listener, log);

        cfg = initializeConfiguration(uri);

        cfg.setLogger(log);

        inbox = initializeMailbox(marshaller);
    }

    /**
     *
     * @param marshaller Marshaller to marshal and unmarshal objects.
     * @return Initialized inbox.
     * @throws GridSpiException Thrown in case of any error.
     */
    private GridMailInbox initializeMailbox(GridMarshaller marshaller) throws GridSpiException {
        GridMailInboxMatcher matcher = new GridMailInboxMatcher();

        matcher.setSubject(subj);

        GridMailInbox inbox = null;

        Exception e1 = null;

        try {
            inbox = GridMailInboxFactory.createInbox(cfg, matcher, marshaller);

            // Check that mailbox can be opened.
            if (getLogger().isInfoEnabled() == true) {
                getLogger().info("Initializing mailbox... This may take a while.");
            }

            inbox.open(true);
        }
        catch (GridMailException e) {
            e1 = e;
        }
        finally {
            GridUtils.close(inbox, false, cfg.getLogger());
        }

        if (e1 != null) {
            throw (GridSpiException)new GridSpiException("Failed to initialize email inbox.", e1).setData(135, "src/java/org/gridgain/grid/spi/deployment/uri/scanners/mail/GridUriDeploymentMailScanner.java");
        }

        return inbox;
    }

    /**
     *
     * @param uri Configuration URI.
     * @return Mail inbox configuration.
     */
    private GridMailInboxConfiguration initializeConfiguration(URI uri) {
        GridMailInboxConfiguration cfg = new GridMailInboxConfiguration();

        GridMailConnectionType connType = GridMailConnectionType.NONE;

        String userInfo = uri.getUserInfo();
        String username = null;
        String pswd = null;

        if (userInfo != null) {
            String[] arr = userInfo.split(";");

            if (arr != null && arr.length > 0) {
                for (String el : arr) {
                    if (el.startsWith("auth=") == true) {
                        connType = GridMailConnectionType.valueOf(el.substring(5).toUpperCase());
                    }
                    else if (el.startsWith("freq=") == true) {
                        // No-op.
                    }
                    else if (el.startsWith("subj=") == true) {
                        subj = el.substring(5);
                    }
                    else if (el.indexOf(':') != -1) {
                        int idx = el.indexOf(':');

                        username = el.substring(0, idx);
                        pswd = el.substring(idx + 1);
                    }
                    else {
                        username = el;
                    }
                }
            }
        }

        cfg.setConnectionType(connType);
        cfg.setProtocol(GridMailInboxProtocol.valueOf(uri.getScheme().toUpperCase()));
        cfg.setHost(uri.getHost());
        cfg.setPort(uri.getPort());
        cfg.setUsername(username);
        cfg.setPassword(pswd);
        cfg.setStoreFileName(DFLT_STORE_FILE_NAME);

        return cfg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process() {
        long start = System.currentTimeMillis();

        processMail();

        if (getLogger().isDebugEnabled() == true) {
            getLogger().debug("Mail scanner time in milliseconds: " + (System.currentTimeMillis() - start));
        }
    }

    /**
     *
     */
    private void processMail() {
        try {
            // Open mailbox with readonly mode.
            inbox.open(true);

            List<GridMailInboxMessage> msgs = allMsgsCheck == true ? inbox.readNew() : inbox.readAll();

            if (msgs != null) {
                for (GridMailInboxMessage msg : msgs) {
                    if (msg.getAttachmentCount() > 0) {
                        for (int i = 0; i < msg.getAttachmentCount(); i++) {
                            GridMailInboxAttachment attach = msg.getAttachment(i);

                            String fileName = attach.getFileName();

                            String msgFileUid = msg.getUid() + '_' + i;

                            if (fileName != null
                                &&
                                (
                                    attach.isMimeType(MIME_OCTETSTREAM) == true ||
                                    attach.isMimeType(MIME_JAR) == true ||
                                    attach.isMimeType(MIME_ZIP) == true
                                )
                                &&
                                getFilter().accept(null, fileName.toLowerCase()) == true
                                &&
                                msgsFileUidCache.contains(msgFileUid) == false
                                ) {
                                try {
                                    File file = createTempFile(fileName, getDeployDirectory());

                                    attach.saveToFile(file);

                                    String fileUri = getFileUri(msgFileUid);

                                    // Delete file when JVM stopped.
                                    file.deleteOnExit();

                                    Date date = msg.getReceivedDate();

                                    assert date != null : "ASSERTION [line=251, file=src/java/org/gridgain/grid/spi/deployment/uri/scanners/mail/GridUriDeploymentMailScanner.java]";

                                    getListener().onNewOrUpdatedFile(file, fileUri, date.getTime());
                                }
                                catch (IOException e) {
                                    getLogger().error("Failed to save: " + fileName, e);
                                }
                            }

                            msgsFileUidCache.add(msgFileUid);
                        }
                    }
                }
            }

            allMsgsCheck = true;

            msgsFileUidCache.clear();
        }
        catch (GridMailException e) {
            getLogger().error("Failed to get messages.", e);
        }
        finally {
            GridUtils.close(inbox, false, getLogger());

            try {
                inbox.flush();
            }
            catch (GridMailException e) {
                getLogger().error("Failed to flush mailbox.", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentMailScanner.class, this,
            "uri", getUri() != null ? GridUriDeploymentUtils.hidePassword(getUri().toString()) : null);
    }
}
