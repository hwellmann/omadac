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

import org.gridgain.grid.util.mail.*;
import java.util.*;

/**
 * Represents mail inbox message.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public interface GridMailInboxMessage {
    /**
     * Gets message UID.
     *
     * @return Message UID.
     * @throws GridMailException Thrown in case of any errors.
     */
    public String getUid() throws GridMailException;

    /**
     * Gets message subject.
     *
     * @return Message subject.
     * @throws GridMailException Thrown in case of any errors.
     */
    public String getSubject() throws GridMailException;

    /**
     * Gets message header.
     *
     * @param name Header name.
     * @return Header value.
     * @throws GridMailException Thrown in case of any errors.
     */
    public String[] getHeader(String name) throws GridMailException;

    /**
     * Gets message received date.
     *
     * @return Message received date.
     * @throws GridMailException Thrown in case of any errors.
     */
    public Date getReceivedDate() throws GridMailException;

    /**
     * Gets mail inbox attachment.
     *
     * @param idx Index of requested attachment.
     * @return Mail inbox attachment.
     * @throws GridMailException Thrown in case of any errors.
     */
    public GridMailInboxAttachment getAttachment(int idx) throws GridMailException;

    /**
     * Gets attachments count.
     *
     * @return Attachments count.
     * @throws GridMailException Thrown in case of any errors.
     */
    public int getAttachmentCount() throws GridMailException;
}
