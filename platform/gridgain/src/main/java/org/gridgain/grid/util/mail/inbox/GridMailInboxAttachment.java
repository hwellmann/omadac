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

import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.mail.*;
import java.io.*;

/**
 * This class represents mail attachment. Attachment should use {@link Serializable} 
 * object or file content.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public interface GridMailInboxAttachment {
    /**
     * Gets object that belongs to attachment. If attachment is based on file content method 
     * returns <tt>null</tt>.
     *
     * @param marshaller Marshaller to unmarshal an object. 
     * @return Object in attachment or <tt>null</tt>.
     * @throws GridMailException Thrown in case of any error.
     */
    public Object getContent(GridMarshaller marshaller) throws GridMailException;

    /**
     * Tests whether an attachement's mime type equals to given MIME type.
     *
     * @param mimeType Mime type to check.
     * @return <tt>true</tt> if equals, <tt>false</tt> otherwise.
     * @throws GridMailException Thrown in case of any error.
     */
    boolean isMimeType(String mimeType) throws GridMailException;

    /**
     * Gets file name in attachment.
     *
     * @return File name if file was attached, otherwise <tt>null</tt>.
     * @throws GridMailException Thrown in case of any error.
     */
    public String getFileName() throws GridMailException;

    /**
     * Save attachment on disk.
     *
     * @param file File path where file will be saved.
     * @throws GridMailException Thrown in case of any error.
     */
    public void saveToFile(File file) throws GridMailException;
}
