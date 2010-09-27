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
import org.gridgain.grid.util.mail.*;

/**
 * This interface defines methods for working with mail inbox.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public interface GridMailInbox {
    /**
     * Connects and opens mail inbox.
     *
     * @param readOnly Open mode flag. If <tt>true</tt> then mail inbox opens
     *      in read-only mode, otherwise read-write mode.
     * @throws GridMailException Thrown if any error occurs.
     */
    public void open(boolean readOnly) throws GridMailException;

    /**
     * Closes mail inbox and terminates connection.
     *
     * @param purge Expunges all deleted messages if this flag is <tt>true</tt>.
     * @throws GridMailException Thrown if any error occurs.
     */
    public void close(boolean purge) throws GridMailException;

    /**
     * Returns list of new mail messages since the last read. This method should be used 
     * after calling {@link #readAll()} method.
     *
     * @return List of new messages since the last read.
     * @throws GridMailException Thrown if any error occurs.
     */
    public List<GridMailInboxMessage> readNew() throws GridMailException;

    /**
     * Returns list of all mail messages in mail inbox.
     *
     * @return List of all messages in mail inbox.
     * @throws GridMailException Thrown if an error occurs.
     */
    public List<GridMailInboxMessage> readAll() throws GridMailException;

    /**
     * Deletes messages in mail inbox with received date before argument date.
     * Note that some mail providers don't support mail's received date.
     *
     * @param date All messages received before this date will be deleted.
     * @return Number of messages marked as deleted.
     * @throws GridMailException Thrown if an error occurs.
     */
    public int removeOld(Date date) throws GridMailException;

    /**
     * Flushes working data.
     *
     * @throws GridMailException Thrown if an error occurs.
     */
    public void flush() throws GridMailException;
}
