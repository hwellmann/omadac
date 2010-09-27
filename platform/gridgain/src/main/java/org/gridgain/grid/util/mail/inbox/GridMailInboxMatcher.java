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
import org.gridgain.grid.util.tostring.*;

/**
 * Container for message matching rules used by mail inbox when reading messages.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridMailInboxMatcher {
    /** Subject. */
    private String subj = null;

    /** Map of message headers. */
    private Map<String, String> hdrs = new HashMap<String, String>();

    /**
     * Sets rule for mail "Subject".
     *
     * @param subj Rule for mail "Subject".
     */
    public void setSubject(String subj) {
        this.subj = subj;
    }

    /**
     * Gets subject used for messages, or <tt>null</tt> if messages are not unified by subject.
     *
     * @return Subject, or <tt>null</tt> if messages are not unified by subject.
     */
    public String getSubject() {
        return subj;
    }

    /**
     * Appends header to the map of allowed headers.
     *
     * @param name Header name.
     * @param val Header value
     */
    public void addHeader(String name, String val) {
        assert name != null : "ASSERTION [line=65, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMatcher.java]";
        assert val != null : "ASSERTION [line=66, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMatcher.java]";

        hdrs.put(name, val);
    }

    /**
     * Removes header from the map of allowed headers.
     *
     * @param name Header name.
     */
    public void removeHeader(String name) {
        assert name != null : "ASSERTION [line=77, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxMatcher.java]";

        hdrs.remove(name);
    }

    /**
     * Gets headers used for messages, or <tt>null</tt> if messages not unified by headers.
     *
     * @return Map of allowed headers.
     */
    public Map<String, String> getHeaders() {
        Map<String, String> map = new HashMap<String, String>();

        map.putAll(hdrs);

        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMailInboxMatcher.class, this);
    }
}
