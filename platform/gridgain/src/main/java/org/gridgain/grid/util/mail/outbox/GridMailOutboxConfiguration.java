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

package org.gridgain.grid.util.mail.outbox;

import org.gridgain.grid.util.mail.*;
import org.gridgain.grid.util.tostring.*;
import java.util.*;

/**
 * This class represents mail outbox configuration.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridMailOutboxConfiguration {
    /** Default SMTP port. */
    private static final int DFLT_SMTP_PORT = 25;

    /** Outbox protocol. */
    private GridMailOutboxProtocol proto = GridMailOutboxProtocol.SMTP;

    /** Connection type. */
    private GridMailConnectionType connType = GridMailConnectionType.NONE;

    /** Mail server host. */
    private String host = null;

    /** Mail server port. */
    private int port = DFLT_SMTP_PORT;

    /** Account's user name. */
    private String user = null;

    /** Account's password. */
    private String pswd = null;

    /** Additional connection properties. */
    private Properties props = null;

    /** Value of the field "from" in mails. */
    private String from = null;

    /** Value of the field "subject" in mails. */
    private String subj = null;

    /**
     * Creates a new mail outbox configuration with default initial parameters.
     */
    public GridMailOutboxConfiguration() {
        props = new Properties();
    }

    /**
     * Creates a new mail outbox configuration with given initial parameters.
     *
     * @param proto Outbox protocol.
     * @param connType Connection type.
     * @param host Mail server host.
     * @param port Mail server port.
     * @param user Account's user name.
     * @param pswd Account's password.
     * @param props Additional connection properties.
     * @param from Value of the field "from" in mails.
     * @param subj Value of the field "subject" in mails.
     */
    public GridMailOutboxConfiguration(GridMailOutboxProtocol proto, GridMailConnectionType connType, String host,
        int port, String user, String pswd, Properties props, String from, String subj) {
        assert proto != null : "ASSERTION [line=87, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxConfiguration.java]";
        assert connType != null : "ASSERTION [line=88, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxConfiguration.java]";
        assert host != null : "ASSERTION [line=89, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxConfiguration.java]";
        assert port > 0 : "ASSERTION [line=90, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxConfiguration.java]";
        assert from != null : "ASSERTION [line=91, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxConfiguration.java]";
        assert subj != null : "ASSERTION [line=92, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxConfiguration.java]";

        this.proto = proto;
        this.connType = connType;
        this.host = host;
        this.port = port;
        this.user = user;
        this.pswd = pswd;
        this.props = props;
        this.subj = subj;
        this.from = from;
    }

    /**
     * Returns value of the field "from" in mails.
     *
     * @return Value of the field "from".
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets value of the field "from" in mails.
     *
     * @param from Value of the field "from" to set.
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns value of the field "subject" in mails.
     *
     * @return Value of the field "subject".
     */
    public String getSubject() {
        return subj;
    }

    /**
     * Sets value of the field "subject" in mails.
     *
     * @param subj Value of the field "subject" to set.
     */
    public void setSubject(String subj) {
        this.subj = subj;
    }

    /**
     * Returns outbox protocol.
     *
     * @return Value of outbox protocol.
     */
    public GridMailOutboxProtocol getProtocol() {
        return proto;
    }

    /**
     * Sets outbox protocol.
     *
     * @param proto Value of outbox protocol to set.
     */
    public void setProtocol(GridMailOutboxProtocol proto) {
        this.proto = proto;
    }

    /**
     * Returns additional connection properties.
     *
     * @return Value of additional connection properties.
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
     * Returns account's password.
     *
     * @return Value of account's password.
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
     * Returns account's user name.
     *
     * @return Value of account's user name.
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
     * Returns mail server port.
     *
     * @return Value of mail server port.
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
     * Returns mail server host.
     *
     * @return Value of mail server host.
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
     * Returns connection type.
     *
     * @return Value of connection type.
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMailOutboxConfiguration.class, this);
    }
}
