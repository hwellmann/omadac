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

import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.mail.*;
import javax.mail.*;
import java.util.*;
import java.io.*;

/**
 * This interface defines API for sending one mail. All configuration settings should be
 * applied before messages is sent. To obtain authentication for a network connection
 * use {@link Authenticator}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public interface GridMailOutboxSession {
    /**
     * Sends a prepared mail.
     *
     * @throws GridMailException Thrown if sending failed.
     */
    public void send() throws GridMailException;

    /**
     * Sets the field "from" for this session.
     *
     * @param from Value of the field "from".
     * @throws GridMailException Thrown if setting failed.
     */
    public void setFrom(String from) throws GridMailException;

    /**
     * Sets the field "subject" for this session.
     *
     * @param subj Value of the field "subject".
     * @throws GridMailException Thrown if setting failed.
     */
    public void setSubject(String subj) throws GridMailException;

    /**
     * Sets authenticator for this session..
     *
     * @param auth Value of authenticator.
     * @throws GridMailException Thrown if setting failed.
     */
    public void setAuthenticator(Authenticator auth) throws GridMailException;

    /**
     * Sets connection properties for this session.
     *
     * @param props Value of connection properties.
     * @throws GridMailException Thrown if setting failed.
     */
    public void setProperties(Properties props) throws GridMailException;

    /**
     * Adds object as attachment for this session..
     *
     * @param obj An attachment.
     * @param name Name of attachment.
     * @param idx Index of attachment.
     * @param marshaller Marshaller to marshal an attachment.
     * @throws GridMailException Thrown if adding failed.
     */
    public void addAttachment(Serializable obj, String name, int idx, GridMarshaller marshaller) 
        throws GridMailException;

    /**
     * Adds file as attachment for this session.
     *
     * @param file An attachment.
     * @param idx Index of attachment.
     * @throws GridMailException Thrown if adding failed.
     */
    public void addAttachment(File file, int idx) throws GridMailException;

    /**
     * Adds "To" (primary) recipient for this session.
     *
     * @param to A "To" recipient's mail address.
     * @throws GridMailException Thrown if adding failed.
     */
    public void addToRecipient(String to) throws GridMailException;

    /**
     * Adds "CC" (carbon copy) recipient for this session.for this session.
     *
     * @param cc A "CC" recipient's mail address.
     * @throws GridMailException Thrown if adding failed.
     */
    public void addCcRecipient(String cc) throws GridMailException;

    /**
     * Adds "BCC" (blind carbon copy) recipient for this session.
     *
     * @param bcc A "BCC" recipient's mail address.
     * @throws GridMailException Thrown if adding failed.
     */
    public void addBccRecipient(String bcc) throws GridMailException;

    /**
     * Returns "Message-Id" header value of a sent message. This value
     * available only after successful method {@link #send()} invocation.
     *
     * @return Message ID.
     */
    public String[] getMessageId();
}
