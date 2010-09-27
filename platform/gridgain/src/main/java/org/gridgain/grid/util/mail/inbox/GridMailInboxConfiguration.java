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

package org.gridgain.grid.util.mail.inbox;

import java.util.*;

import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.mail.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class represents mail inbox configuration.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridMailInboxConfiguration {
    /** Inbox protocol.  */
    private GridMailInboxProtocol proto = GridMailInboxProtocol.POP3;

    /** Mail connection type. */
    private GridMailConnectionType connType = GridMailConnectionType.NONE;

    /** Inbox folder name. */
    private String folderName = "Inbox";

    /** Number of messages read at a time. */
    private int readBatchSize = 100;

    /** Mail server host. */
    private String host = null;

    /** Mail server port. */
    private int port = 110;

    /** Mail server username. */
    private String user = null;

    /** Mail sever password. */
    @GridToStringExclude
    private String pswd = null;

    /** Additional properties. */
    private Properties props = new Properties();

    /** Mail store file name. */
    private String storeFileName = null;

    /** Grid logger. */
    private GridLogger log = null;

    /**
     * Gets logger used by this instance.
     *
     * @return The logger.
     */
    public GridLogger getLogger() {
        return log;
    }

    /**
     * Set logger to be used by this instance.
     *
     * @param log The logger to set.
     */
    public void setLogger(GridLogger log) {
        this.log = log;
    }

    /**
     * Gets inbox protocol.
     *
     * @return Inbox protocol.
     */
    public GridMailInboxProtocol getProtocol() {
        return proto;
    }

    /**
     * Sets inbox protocol.
     *
     * @param proto Inbox protocol to set.
     */
    public void setProtocol(GridMailInboxProtocol proto) {
        this.proto = proto;
    }

    /**
     * Gets inbox folder name.
     *
     * @return Inbox folder name.
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * Sets inbox folder name.
     *
     * @param folderName Inbox folder name.
     */
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    /**
     * Gets connection type.
     *
     * @return Connection type.
     */
    public GridMailConnectionType getConnectionType() {
        return connType;
    }

    /**
     * Sets connection type.
     *
     * @param connType Connection type to set.
     */
    public void setConnectionType(GridMailConnectionType connType) {
        this.connType = connType;
    }

    /**
     * Gets mail server host.
     *
     * @return Mail server host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets mail server host.
     *
     * @param host Mail server host to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Gets mail server port.
     *
     * @return Mail server port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets mail server port.
     *
     * @param port Mail server port to set.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets account's user name.
     *
     * @return Account's user name.
     */
    public String getUsername() {
        return user;
    }

    /**
     * Sets account's user name.
     *
     * @param user Account's user name to set.
     */
    public void setUsername(String user) {
        this.user = user;
    }

    /**
     * Gets account's password.
     *
     * @return Account's password.
     */
    public String getPassword() {
        return pswd;
    }

    /**
     * Sets account's password.
     *
     * @param pswd Account's password to set.
     */
    public void setPassword(String pswd) {
        this.pswd = pswd;
    }

    /**
     * Gets additional connection properties.
     *
     * @return Additional connection properties.
     */
    public Properties getCustomProperties() {
        return props;
    }

    /**
     * Sets additional connection properties.
     *
     * @param props Additional connection properties to set.
     */
    public void setCustomProperties(Properties props) {
        this.props = props;
    }

    /**
     * Gets file path where mail inbox store data.
     *
     * @return File path or <tt>null</tt> if not used.
     */
    public String getStoreFileName() {
        return storeFileName;
    }

    /**
     * Sets file path where mail inbox store data.
     *
     * @param storeFileName File path or <tt>null</tt> if not used.
     */
    public void setStoreFileName(String storeFileName) {
        this.storeFileName = storeFileName;
    }

    /**
     * Gets number of messages read at a time.
     *
     * @return message batch size.
     */
    public int getReadBatchSize() {
        return readBatchSize;
    }

    /**
     * Sets number of messages read at a time.
     *
     * @param readBatchSize Message batch size.
     */
    public void setReadBatchSize(int readBatchSize) {
        this.readBatchSize = readBatchSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMailInboxConfiguration.class, this);
    }
}
