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

package org.gridgain.grid.util.mail.inbox.pop3;

import java.io.*;
import java.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Message descriptor contains short information about message from mail inbox.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridPop3MessageDescriptor implements Serializable {
    /** Message UID. */
    private Object uid = null;

    /** Message received date. */
    private Date rcvDate = null;

    /** Whether it was accepted or not. */
    private boolean accepted = false;

    /**
     * Gets message UID.
     *
     * @return Message UID.
     */
    public Object getUid() {
        return uid;
    }

    /**
     * Sets message UID.
     *
     * @param uid Message UID.
     */
    public void setUid(Object uid) {
        this.uid = uid;
    }

    /**
     * Gets message received date.
     *
     * @return Message received date.
     */
    public Date getReceiveDate() {
        return rcvDate;
    }

    /**
     * Sets message received date.
     *
     * @param rcvDate Message received date.
     */
    public void setReceiveDate(Date rcvDate) {
        this.rcvDate = rcvDate;
    }

    /**
     * Tests whether message accepted. Descriptor uses this flag to mark what messages matched for
     * required rules.
     *
     * @return <tt>true</tt> if message accepted, <tt>false</tt> otherwise.
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Sets <tt>true</tt> if message accepted.
     *
     * @param accepted Flag describing that message was accepted.
     */
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridPop3MessageDescriptor.class, this);
    }
}
