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

package org.gridgain.grid.util.mail.inbox.imap;

import java.util.*;
import java.util.Map.*;
import javax.mail.*;
import javax.mail.search.*;
import org.gridgain.grid.util.mail.*;
import org.gridgain.grid.util.mail.inbox.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides IMAP implementation for {@link GridMailInbox}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridImapInbox implements GridMailInbox {
    /** Mail inbox configuration. */
    private final GridMailInboxConfiguration cfg;

    /** Mail inbox matcher. */
    private final GridMailInboxMatcher matcher;

    /** Message store. */
    private Store store = null;

    /** Message folder. */
    private Folder folder = null;

    /** Session authenticator. */
    private Authenticator auth = null;

    /** Connection properties. */
    private Properties props = new Properties();

    /** Last message UID. */
    private Long lastMsgUid = null;

    /** Value between sessions must be the same. Shows that any cached UIDs are not stale. */
    private Long uidValidity = null;

    /** Whether or not inbox is opened. */
    private boolean isOpened = false;

    /**
     * Creates a mail inbox with specified configuration and message filter.
     * All messages from mail inbox will be filtered with rules defined in
     * <tt>matcher</tt> argument.
     *
     * @param cfg Mail inbox configuration.
     * @param matcher Message filter.
     */
    public GridImapInbox(GridMailInboxConfiguration cfg, GridMailInboxMatcher matcher) {
        assert cfg.getProtocol() != null : "ASSERTION [line=75, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";
        assert cfg.getConnectionType() != null : "ASSERTION [line=76, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";
        assert cfg.getHost() != null : "ASSERTION [line=77, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";
        assert cfg.getPort() > 0 : "ASSERTION [line=78, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";
        assert matcher != null : "ASSERTION [line=79, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";
        assert cfg.getProtocol() == GridMailInboxProtocol.IMAP || cfg.getProtocol() == GridMailInboxProtocol.IMAPS : "ASSERTION [line=80, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";

        this.cfg = cfg;
        this.matcher = matcher;

        prepareParameters();
    }

    /**
     * {@inheritDoc}
     */
    public void open(boolean readOnly) throws GridMailException {
        if (isOpened == true) {
            throw (GridMailException)new GridMailException("IMAP mailbox opened already.").setData(93, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }

        // Mail properties must be initialized.
        assert props != null : "ASSERTION [line=97, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";

        try {
            store = Session.getInstance(props, auth).getStore();

            store.connect();

            folder = store.getFolder(cfg.getFolderName());

            folder.open(readOnly == true ? Folder.READ_ONLY : Folder.READ_WRITE);
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Failed to open IMAP mailbox.", e).setData(109, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }

        // Calculate last message uid.
        initializeLastMessageUid();

        isOpened = true;
    }

    /**
     * {@inheritDoc}
     */
    public void close(boolean purge) throws GridMailException {
        Exception e = null;

        try {
            if (folder != null) {
                folder.close(purge);
            }
        }
        catch (MessagingException e1) {
            e = e1;
        }

        try {
            if (store != null) {
                store.close();
            }
        }
        catch (MessagingException e1) {
            // Don't loose the initial exception.
            if (e == null) {
                e = e1;
            }
        }

        folder = null;
        store = null;

        isOpened = false;

        if (e != null) {
            throw (GridMailException)new GridMailException("Failed to close IMAP mailbox.", e).setData(151, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<GridMailInboxMessage> readNew() throws GridMailException {
        if (isOpened == false) {
            throw (GridMailException)new GridMailException("IMAP mailbox not opened.").setData(160, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }

        List<GridMailInboxMessage> msgList = null;

        try {
            UIDFolder uidFolder = (UIDFolder)folder;

            Message[] msgs = uidFolder.getMessagesByUID(lastMsgUid + 1, UIDFolder.LASTUID);

            if (msgs != null && msgs.length > 0) {
                // Method getMessagesByUID() always return last message for range where lastUid not present.
                if (msgs.length == 1 && lastMsgUid == uidFolder.getUID(msgs[0])) {
                    return null;
                }

                // Get last message UID.
                long uid = uidFolder.getUID(msgs[msgs.length - 1]);

                List<SearchTerm> terms = makeSearchTerms(matcher);

                terms.add(new FlagTerm(new Flags(Flags.Flag.DELETED), false));

                // Construct search term.
                SearchTerm term = new AndTerm(terms.toArray(new SearchTerm[terms.size()]));

                msgs = folder.search(term, msgs);

                // Set last message UID to new position.
                lastMsgUid = uid;

                if (msgs != null && msgs.length > 0) {
                    msgList = new ArrayList<GridMailInboxMessage>(msgs.length);

                    for (Message msg : msgs) {
                        if (msg.isExpunged() == true) {
                            // Ignore expunged messages.
                            continue;
                        }

                        long msgUid = uidFolder.getUID(msg);

                        assert msgUid > 0 : "ASSERTION [line=202, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";

                        msgList.add(new GridMailInboxMessageAdapter(msg, String.valueOf(msgUid), cfg.getLogger()));
                    }
                }
            }
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Failed to get new IMAP messages.", e).setData(210, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }

        return msgList;
    }

    /**
     * {@inheritDoc}
     */
    public List<GridMailInboxMessage> readAll() throws GridMailException {
        if (isOpened == false) {
            throw (GridMailException)new GridMailException("IMAP mailbox not opened.").setData(221, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }

        List<GridMailInboxMessage> msgsList = null;

        try {
            UIDFolder uidFolder = (UIDFolder)folder;

            Message[] msgs = uidFolder.getMessagesByUID(1, UIDFolder.LASTUID);

            if (msgs != null && msgs.length > 0) {
                // Get last message UID.
                long uid = uidFolder.getUID(msgs[msgs.length - 1]);

                List<SearchTerm> terms = makeSearchTerms(matcher);

                terms.add(new FlagTerm(new Flags(Flags.Flag.DELETED), false));

                // Construct search term.
                SearchTerm term = new AndTerm(terms.toArray(new SearchTerm[terms.size()]));

                msgs = folder.search(term, msgs);

                // Set last message UID to new position.
                lastMsgUid = uid;

                if (msgs != null && msgs.length > 0) {
                    msgsList = new ArrayList<GridMailInboxMessage>(msgs.length);

                    for (Message msg : msgs) {
                        if (msg.isExpunged() == true) {
                            // Ignore expunged messages.
                            continue;
                        }

                        long msgUid = uidFolder.getUID(msg);

                        assert msgUid > 0 : "ASSERTION [line=258, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";

                        msgsList.add(new GridMailInboxMessageAdapter(msg, String.valueOf(msgUid), cfg.getLogger()));
                    }
                }
            }
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Failed to get all IMAP messages.", e).setData(266, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }

        return msgsList;
    }

    /**
     * {@inheritDoc}
     */
    public int removeOld(Date date) throws GridMailException {
        assert date != null : "ASSERTION [line=276, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";

        if (isOpened == false) {
            throw (GridMailException)new GridMailException("IMAP mailbox not opened.").setData(279, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }

        if (folder.isOpen() == false || folder.isOpen() == true && folder.getMode() != Folder.READ_WRITE) {
            throw (GridMailException)new GridMailException("IMAP mailbox is not opened or opened in readonly mode.").setData(283, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }

        try {
            List<SearchTerm> termList = makeSearchTerms(matcher);

            termList.add(new ReceivedDateTerm(ComparisonTerm.LT, date));
            termList.add(new FlagTerm(new Flags(Flags.Flag.DELETED), false));

            // Construct search term.
            SearchTerm term = new AndTerm(termList.toArray(new SearchTerm[termList.size()]));

            Message[] foundMsgs = folder.search(term);

            int n = 0;

            if (foundMsgs != null && foundMsgs.length > 0) {
                for (Message msg : foundMsgs) {
                    msg.setFlag(Flags.Flag.DELETED, true);
                }

                n = foundMsgs.length;
            }

            return n;
        }
        catch (MessagingException e) {
            throw (GridMailException)new GridMailException("Failed to search old IMAP messages.", e).setData(310, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws GridMailException {
        // No-op.
    }

    /**
     * Prepares Java Mail properties.
     */
    private void prepareParameters() {
        String protoName = cfg.getProtocol().toString().toLowerCase();

        // Session properties.
        props.setProperty("mail.store.protocol", protoName);

        String mailProto = "mail." + protoName;

        props.setProperty(mailProto + ".host", cfg.getHost());
        props.setProperty(mailProto + ".port", Integer.toString(cfg.getPort()));

        if (cfg.getConnectionType() == GridMailConnectionType.STARTTLS) {
            props.setProperty(mailProto + ".starttls.enable", "true");
        }
        else if (cfg.getConnectionType() == GridMailConnectionType.SSL) {
            props.setProperty(mailProto + ".ssl", "true");
        }

        // Add property for authentication by username.
        if (cfg.getUsername() != null) {
            props.setProperty(mailProto + ".auth", "true");

            auth = new Authenticator() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(cfg.getUsername(), cfg.getPassword());
                }
            };
        }

        if (cfg.getCustomProperties() != null) {
            props.putAll(cfg.getCustomProperties());
        }
    }

    /**
     * Creates list of search rules for IMAP protocol.
     *
     * @param matcher Message filter.
     * @return List of search rules.
     */
    private List<SearchTerm> makeSearchTerms(GridMailInboxMatcher matcher) {
        assert matcher != null : "ASSERTION [line=369, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";

        List<SearchTerm> terms = new ArrayList<SearchTerm>();

        if (matcher.getSubject() != null) {
            // Note: don't use SubjectTerm. It doesn't work in JavaMail 1.4
            terms.add(new HeaderTerm("Subject", matcher.getSubject()));
        }

        if (matcher.getHeaders() != null) {
            for (Entry<String, String> entry : matcher.getHeaders().entrySet()) {
                if (entry.getValue() != null) {
                    terms.add(new HeaderTerm(entry.getKey(), entry.getValue()));
                }
            }
        }

        return terms;
    }

    /**
     * Prepares parameter used as last message resolver.
     *
     * @throws GridMailException Thrown in case of any error.
     */
    private void initializeLastMessageUid() throws GridMailException {
        try {
            UIDFolder uidFolder = (UIDFolder)folder;

            if (uidValidity == null || uidValidity != uidFolder.getUIDValidity()) {
                uidValidity = uidFolder.getUIDValidity();

                lastMsgUid = getLastMessageUid();
            }

            assert uidValidity != null : "ASSERTION [line=404, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";
            assert lastMsgUid != null : "ASSERTION [line=405, file=src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java]";
        }
        catch (MessagingException e) {
            // Reset values if some errors occurred.
            uidValidity = null;
            lastMsgUid = null;

            throw (GridMailException)new GridMailException("Failed to initialize last IMAP message UID.", e).setData(412, "src/java/org/gridgain/grid/util/mail/inbox/imap/GridImapInbox.java");
        }
    }

    /**
     * Calculates last message UID.
     *
     * @return Last message UID.
     * @throws MessagingException Thrown in case of any error.
     */
    private long getLastMessageUid() throws MessagingException {
        int msgCnt = folder.getMessageCount();

        return msgCnt == 0 ? 0 : ((UIDFolder)folder).getUID(folder.getMessage(msgCnt));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridImapInbox.class, this);
    }
}
