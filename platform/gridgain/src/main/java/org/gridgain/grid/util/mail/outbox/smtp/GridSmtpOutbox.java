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

package org.gridgain.grid.util.mail.outbox.smtp;

import java.util.*;
import javax.mail.*;
import org.gridgain.grid.util.mail.*;
import org.gridgain.grid.util.mail.outbox.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides SMTP implementation for {@link GridMailOutbox}. This implementation
 * is based on Java Mail API.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridSmtpOutbox implements GridMailOutbox {
    /** Mail outbox configuration. */
    private GridMailOutboxConfiguration cfg = null;

    /** Whether or not parameters are prepared. */
    private boolean isPrep = false;

    /** Additional properties. */
    private Properties props = new Properties();

    /** Session authenticator. */
    private Authenticator auth = null;

    /**
     * Creates new SMTP outbox with all default values.
     */
    public GridSmtpOutbox() {
        // No-op.
    }

    /**
     * Creates new SMTP mail outbox with specified configuration.
     *
     * @param cfg Mail outbox configuration.
     */
    public GridSmtpOutbox(GridMailOutboxConfiguration cfg) {
        assert cfg != null : "ASSERTION [line=63, file=src/java/org/gridgain/grid/util/mail/outbox/smtp/GridSmtpOutbox.java]";

        this.cfg = cfg;
    }

    /**
     * Returns session object for working with mail outbox.
     *
     * @return Session object.
     */
    public GridMailOutboxSession getSession() {
        if (isPrep == false) {
            prepareParameters();

            isPrep = true;
        }

        return new GridMailOutboxSessionAdapter(cfg.getFrom(), cfg.getSubject(), props , auth);
    }

    /**
     * Returns mail outbox configuration.
     *
     * @return Mail outbox configuration.
     */
    public GridMailOutboxConfiguration getConfiguration() {
        return cfg;
    }

    /**
     * Sets mail outbox configuration. Configuration must be defined before method
     * {@link #getSession()} called.
     *
     * @param cfg Mail outbox configuration.
     */
    public void setConfiguration(GridMailOutboxConfiguration cfg) {
        assert cfg != null : "ASSERTION [line=99, file=src/java/org/gridgain/grid/util/mail/outbox/smtp/GridSmtpOutbox.java]";

        this.cfg = cfg;
    }

    /**
     * Prepares Java Mail API properties.
     */
    private void prepareParameters() {
        String protoName = cfg.getProtocol().toString().toLowerCase();

        // Session properties.
        props.setProperty("mail.transport.protocol", protoName);

        String mailProto = "mail." + protoName;

        props.setProperty(mailProto + ".host", cfg.getHost());
        props.setProperty(mailProto + ".port", Integer.toString(cfg.getPort()));

        if (cfg.getConnectionType() == GridMailConnectionType.STARTTLS) {
            props.setProperty(mailProto + ".starttls.enable", "true");
        }
        else if (cfg.getConnectionType() == GridMailConnectionType.SSL) {
            props.setProperty(mailProto + ".ssl", "true");
        }

        // Add property for authentication by username.
        if (cfg.getUsername() != null && cfg.getUsername().length() > 0) {
            props.setProperty(mailProto + ".auth", "true");

            auth = new Authenticator() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(cfg.getUsername(), cfg.getPassword());
                }
            };
        }

        if (cfg.getCustomProperties() != null) {
            props.putAll(cfg.getCustomProperties());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridSmtpOutbox.class, this);
    }
}
