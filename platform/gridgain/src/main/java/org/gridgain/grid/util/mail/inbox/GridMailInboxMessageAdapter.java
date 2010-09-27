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

import java.io.*;
import java.util.*;
import javax.mail.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.mail.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides default implementation for {@link GridMailInboxMessage}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridMailInboxMessageAdapter implements GridMailInboxMessage {
    /** Mail message. */
    private final Message msg;

    /** Message UID. */
    private final String uid;

    /** Grid logger. */
    private final GridLogger log;

    /** Message's received date. */
    private Date rcvDate = null;

    /**
     * Creates message with given arguments.
     *
     * @param msg Mail message.
     * @param uid Message UID.
     * @param log Logger to log.
     */
    public GridMailInboxMessageAdapter(Message msg, String uid, GridLogger log) {
        assert msg != null : "ASSERTION [line=58, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java]";
        assert uid != null : "ASSERTION [line=59, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java]";
        assert log != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java]";

        this.msg = msg;
        this.uid = uid;
        this.log = log.getLogger(getClass());
    }

    /**
     * Creates message with given arguments.
     *
     * @param msg Mail message.
     * @param uid Message UID.
     * @param rcvDate Received date.
     * @param log Logger to log.
     */
    public GridMailInboxMessageAdapter(Message msg, String uid, Date rcvDate, GridLogger log) {
        assert msg != null : "ASSERTION [line=76, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java]";
        assert uid != null : "ASSERTION [line=77, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java]";
        assert rcvDate != null : "ASSERTION [line=78, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java]";
        assert log != null : "ASSERTION [line=79, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java]";

        this.msg = msg;
        this.uid = uid;
        this.rcvDate = rcvDate;
        this.log = log.getLogger(getClass());
    }

    /**
     * {@inheritDoc}
     */
    public String getUid() throws GridMailException {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public String getSubject() throws GridMailException {
        try {
            return msg.getSubject();
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Error when getting subject.", e).setData(102, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getHeader(String name) throws GridMailException {
        assert name != null : "ASSERTION [line=110, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java]";

        try {
            return msg.getHeader(name);
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Error when getting message header: " + name, e).setData(116, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getReceivedDate() throws GridMailException {
        try {
            if (rcvDate != null) {
                return rcvDate;
            }

            if (msg.getReceivedDate() != null) {
                return msg.getReceivedDate();
            }

            return new Date();
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Error when getting message received date." , e).setData(136, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getAttachmentCount() throws GridMailException {
        int n = 0;

        try {
            if (msg.getContent() instanceof Multipart == true) {
                n = ((Multipart)msg.getContent()).getCount();
            }
        }
        catch (IOException e) {
            throw (GridMailException)new GridMailException("Error when getting attachments count .", e).setData(152, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java");
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Error when getting attachments count .", e).setData(155, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java");
        }

        return n;
    }

    /**
     * {@inheritDoc}
     */
    public GridMailInboxAttachment getAttachment(int idx) throws GridMailException {
        try {
            if (msg.getContent() instanceof Multipart == true) {
                Multipart body = (Multipart)msg.getContent();

                if (body.getCount() <= idx) {
                    return null;
                }

                return new GridMailInboxAttachmentAdapter(body.getBodyPart(idx), log);
            }

            throw (GridMailException)new GridMailException("Error when getting attachment: " + idx).setData(176, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java");
        }
        catch (IOException e) {
            throw (GridMailException)new GridMailException("Error when getting attachment: " + idx, e).setData(179, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java");
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Error when getting attachment: " + idx, e).setData(182, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMessageAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMailInboxMessageAdapter.class, this);
    }
}
