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

import java.io.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class represents mail attachment.
 * Attachment should use {@link Serializable} object or file content.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMailOutboxAttachment {
    /** Attachment data (serialized object). */
    private final byte[] data;

    /** Attachment name. */
    private final String name;

    /** Attachment file (if any). */
    private final File file;

    /** Attachment index. */
    private final int idx;

    /**
     * Creates attachment based on serialized object.
     *
     * @param data Data (object) placed in attachment.
     * @param name Attachment name in mail.
     * @param idx Index in the list of mail attachments.
     */
    GridMailOutboxAttachment(byte[] data, String name, int idx) {
        assert data != null : "ASSERTION [line=55, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxAttachment.java]";
        assert name != null : "ASSERTION [line=56, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxAttachment.java]";
        assert idx >= 0 : "ASSERTION [line=57, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxAttachment.java]";

        this.data = data;
        this.name = name;
        this.idx = idx;

        file = null;
    }

    /**
     * Creates attachment based on file content.
     *
     * @param file File placed in attachment.
     * @param idx Index in the list of mail attachments.
     */
    GridMailOutboxAttachment(File file, int idx) {
        this.file = file;
        this.idx = idx;

        data = null;
        name = null;
    }

    /**
     * Tests whether this attachment is based on file content.
     *
     * @return <tt>true</tt> if attachment based on file content.
     */
    public boolean isFileAttachment() {
        return file != null;
    }

    /**
     * Returns the byte array of serialized object that belongs to this 
     * attachment. If attachment is based on file content this method 
     * returns <tt>null</tt>.
     *
     * @return Byte array in attachment or <tt>null</tt>.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the attachment name.
     *
     * @return Attachment name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the attachment file. If attachment is based on object data this
     * method returns <tt>null</tt>.
     *
     * @return File in attachment or <tt>null</tt>.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the attachment index.
     *
     * @return Index number.
     */
    public int getIndex() {
        return idx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMailOutboxAttachment.class, this);
    }
}
