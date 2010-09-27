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
import java.util.*;
import javax.activation.*;
import javax.mail.*;
import javax.mail.Message.*;
import javax.mail.internet.*;

import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.mail.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides default implementation for {@link GridMailOutboxSession}. Default
 * implementation uses Java Mail API for working with mail servers.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridMailOutboxSessionAdapter implements GridMailOutboxSession {
    /** Mail field "From". */
    private String from = null;

    /** Mail field "Subject". */
    private String subj = null;

    /** Collection addresses "TO:". */
    private List<Address> toRcpts = null;

    /** Collection addresses "CC:". */
    private List<Address> ccRcpts = null;

    /** Collection addresses "BCC:". */
    private List<Address> bccRcpts = null;

    /** List of attachments. */
    private List<GridMailOutboxAttachment> attachs = null;

    /** Additional properties to be used by Java Mail API. */
    private Properties props = null;

    /** Session authenticator. */
    private Authenticator auth = null;

    /** Array of message IDs. */
    private String[] msgId = null;

    /**
     * Creates new mail outbox session with all default values.
     */
    public GridMailOutboxSessionAdapter() {
        // No-op.
    }

    /**
     * Creates new mail outbox session with specified configuration.
     *
     * @param from Value of the mail field "from".
     * @param subj Value of the mail field "subject".
     * @param props Additional properties used by Java Mail API.
     * @param auth Authenticator object used to call back to the application
     *      when user name and password is needed.
     */
    public GridMailOutboxSessionAdapter(String from, String subj, Properties props, Authenticator auth) {
        assert from != null : "ASSERTION [line=89, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";
        assert subj != null : "ASSERTION [line=90, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";
        assert props != null : "ASSERTION [line=91, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        this.from = from;
        this.subj = subj;
        this.props = props;
        this.auth = auth;
    }

    /**
     * {@inheritDoc}
     */
    public void setFrom(String from) {
        assert from != null : "ASSERTION [line=103, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        this.from = from;
    }

    /**
     * {@inheritDoc}
     */
    public void setSubject(String subj) {
        assert subj != null : "ASSERTION [line=112, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        this.subj = subj;
    }

    /**
     * {@inheritDoc}
     */
    public void setAuthenticator(Authenticator auth) {
        assert auth != null : "ASSERTION [line=121, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        this.auth = auth;
    }

    /**
     * {@inheritDoc}
     */
    public void setProperties(Properties props) {
        assert props != null : "ASSERTION [line=130, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        this.props = props;
    }

    /**
     * {@inheritDoc}
     */
    public void send() throws GridMailException {
        if (toRcpts == null) {
            throw (GridMailException)new GridMailException("Message has to have at least one 'TO' recipient.").setData(140, "src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java");
        }

        try {
            Session ses = Session.getInstance(props, auth);

            Message mimeMsg = new MimeMessage(ses);

            mimeMsg.setFrom(new InternetAddress(from));
            mimeMsg.setSubject(subj);
            mimeMsg.setSentDate(new Date());

            mimeMsg.setRecipients(RecipientType.TO, toRcpts.toArray(new Address[toRcpts.size()]));

            if (ccRcpts != null) {
                mimeMsg.setRecipients(RecipientType.CC, ccRcpts.toArray(new Address[ccRcpts.size()]));
            }

            if (bccRcpts != null) {
                mimeMsg.setRecipients(RecipientType.CC, bccRcpts.toArray(new Address[bccRcpts.size()]));
            }

            Multipart body = new MimeMultipart();

            if (attachs != null) {
                for (GridMailOutboxAttachment attach : attachs) {
                    BodyPart part = null;

                    if (attach.isFileAttachment() == true) {
                        part = createBodyPart(attach.getFile());
                    }
                    else {
                        part = createBodyPart(attach.getData(), attach.getName());
                    }

                    body.addBodyPart(part, attach.getIndex());
                }
            }

            mimeMsg.setContent(body);

            String protocol = props.getProperty("mail.transport.protocol");

            if (protocol != null && protocol.toLowerCase().equals("smtps") == true) {
                ses.setProtocolForAddress("rfc822", "smtps");
            }

            Transport.send(mimeMsg);

            msgId = mimeMsg.getHeader("Message-Id");
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Failed to send message.", e).setData(192, "src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getMessageId() {
        return msgId;
    }

    /**
     * {@inheritDoc}
     */
    public void addAttachment(Serializable obj, String name, int idx, GridMarshaller marshaller) 
        throws GridMailException {
        assert obj != null : "ASSERTION [line=208, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";
        assert name != null : "ASSERTION [line=209, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";
        assert idx >= 0 : "ASSERTION [line=210, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";
        assert marshaller != null : "ASSERTION [line=211, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        if (attachs == null) {
            attachs = new ArrayList<GridMailOutboxAttachment>();
        }
        
        try {
            attachs.add(new GridMailOutboxAttachment(GridMarshalHelper.marshal(marshaller, obj).getArray(), name, idx));
        }
        catch (GridException e) {
            throw (GridMailException)new GridMailException("Failed to add attachment.", e).setData(221, "src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addAttachment(File file, int idx) {
        assert file != null : "ASSERTION [line=229, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";
        assert idx >= 0 : "ASSERTION [line=230, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        if (attachs == null) {
            attachs = new ArrayList<GridMailOutboxAttachment>();
        }

        attachs.add(new GridMailOutboxAttachment(file, idx));
    }

    /**
     * {@inheritDoc}
     */
    public void addToRecipient(String to) throws GridMailException {
        assert to != null : "ASSERTION [line=243, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        toRcpts = addRecipient(toRcpts, to);
    }

    /**
     * {@inheritDoc}
     */
    public void addCcRecipient(String cc) throws GridMailException {
        assert cc != null : "ASSERTION [line=252, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        ccRcpts = addRecipient(ccRcpts, cc);
    }

    /**
     * {@inheritDoc}
     */
    public void addBccRecipient(String bcc) throws GridMailException {
        assert bcc != null : "ASSERTION [line=261, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        bccRcpts = addRecipient(bccRcpts, bcc);
    }

    /**
     * Adds address to collection of recipients.
     *
     * @param addrs Collection of addresses.
     * @param addr Address to add in collection.
     * @return Result collection of recipients.
     * @throws GridMailException Thrown in case of invalid address.
     */
    private List<Address> addRecipient(List<Address> addrs, String addr) throws GridMailException {
        assert addr != null : "ASSERTION [line=275, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        if (addrs == null) {
            addrs = new ArrayList<Address>();
        }

        try {
            addrs.add(new InternetAddress(addr));
        }
        catch (AddressException e) {
            throw (GridMailException)new GridMailException("Invalid email address: " + addr, e).setData(285, "src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java");
        }

        return addrs;
    }

    /**
     * Creates mail attachment with byte array (serialized object).
     *
     * @param data Data to put in attachment.
     * @param name Attachment name.
     * @return New body part object.
     * @throws MessagingException Thrown if Java Mail API error occurs.
     */
    private BodyPart createBodyPart(byte[] data, String name) throws MessagingException {
        assert data != null : "ASSERTION [line=300, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";
        assert name != null : "ASSERTION [line=301, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        BodyPart part = new MimeBodyPart();

        part.setDataHandler(new DataHandler(new GridMailOutboxDataSource("grid.email.datasource", data)));
        part.setFileName(name);

        return part;
    }

    /**
     * Creates attachment with file content.
     *
     * @param file File to put in attachment.
     * @return New body part object where file content placed.
     * @throws MessagingException Thrown if Java Mail API error occurs.
     */
    public BodyPart createBodyPart(File file) throws MessagingException {
        assert file != null : "ASSERTION [line=319, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxSessionAdapter.java]";

        BodyPart part = new MimeBodyPart();

        // Add attachment.
        part.setDataHandler(new DataHandler(new FileDataSource(file)));
        part.setFileName(file.getName());

        return part;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMailOutboxSessionAdapter.class, this);
    }
}
