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
import javax.mail.*;
import javax.mail.internet.*;

import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.mail.*;

/**
 * This is an adapter for inbox attachment.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridMailInboxAttachmentAdapter implements GridMailInboxAttachment {
    /** Mail body part. */
    private final Part part;

    /** Grid logger. */
    private final GridLogger log;

    /**
     * Creates new attachment adapter with given parameters.
     *
     * @param part Mail body part.
     * @param log Grid logger.
     */
    public GridMailInboxAttachmentAdapter(Part part, GridLogger log) {
        assert part != null : "ASSERTION [line=55, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java]";
        assert log != null : "ASSERTION [line=56, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java]";

        this.part = part;
        this.log = log.getLogger(getClass());
    }

    /**
     * {@inheritDoc}
     */
    public Object getContent(GridMarshaller marshaller) throws GridMailException {
        assert marshaller != null : "ASSERTION [line=66, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java]";
        
        InputStream in = null;
        
        try {
            in = part.getInputStream();

            return GridMarshalHelper.unmarshal(marshaller, in, getClass().getClassLoader());
        }
        catch (IOException e) {
            throw (GridMailException)new GridMailException("Error when getting attachment content.", e).setData(76, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java");
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Error when getting attachment content.", e).setData(79, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java");
        }
        catch (GridException e) {
            throw (GridMailException)new GridMailException("Error when getting attachment content.", e).setData(82, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java");
        }
        finally {
            GridUtils.close(in, log);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMimeType(String mimeType) throws GridMailException {
        try {
            return part.isMimeType(mimeType);
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Error when getting attachment mime type.", e).setData(97, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getFileName() throws GridMailException {
        try {
            return part.getFileName();
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Error when getting attachment file name.", e).setData(109, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void saveToFile(File file) throws GridMailException {
        assert file != null : "ASSERTION [line=117, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java]";

        if (part instanceof MimeBodyPart == false) {
            throw (GridMailException)new GridMailException("Only MIME messages can be saved.").setData(120, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java");
        }

        try {
            ((MimeBodyPart)part).saveFile(file);
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Error when saving attachment file: " + file, e).setData(127, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java");
        }
        catch (IOException e) {
            throw (GridMailException)new GridMailException("Error when saving attachment file: " + file, e).setData(130, "src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxAttachmentAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMailInboxAttachmentAdapter.class, this);
    }
}
