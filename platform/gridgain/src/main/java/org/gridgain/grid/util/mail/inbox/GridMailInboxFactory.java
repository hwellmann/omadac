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
import org.gridgain.grid.util.mail.inbox.imap.*;
import org.gridgain.grid.util.mail.inbox.pop3.*;

/**
 * This class provides factory for creating {@link GridMailInbox}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridMailInboxFactory {
    /**
     * Enforces singleton.
     */
    private GridMailInboxFactory() {
        // No-op.
    }

    /**
     * Creates a mail outbox with specified configuration.
     *
     * @param cfg Inbox configuration.
     * @param matcher Object containing rules for messages filtering.
     * @param marshaller Marshaller to marshal and unmarshal objects.
     * @return Newly created mail inbox.
     */
    public static GridMailInbox createInbox(GridMailInboxConfiguration cfg, GridMailInboxMatcher matcher,
        GridMarshaller marshaller) {
        assert cfg != null : "ASSERTION [line=52, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxFactory.java]";
        assert matcher != null : "ASSERTION [line=53, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxFactory.java]";

        // Inbox configuration must have mail protocol set and logger.
        assert cfg.getProtocol() != null : "ASSERTION [line=56, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxFactory.java]";
        assert cfg.getLogger() != null : "ASSERTION [line=57, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxFactory.java]";

        switch (cfg.getProtocol()) {
            case POP3:
            case POP3S: { return new GridPop3Inbox(cfg, matcher, marshaller); }

            case IMAP:
            case IMAPS: { return new GridImapInbox(cfg, matcher); }

            default: {
                // Unsupported inbox mail protocol.
                assert false : "ASSERTION [line=68, file=src/java/org/gridgain/grid/util/mail/inbox/GridMailInboxFactory.java]";

                // Never reached.
                return null;
            }
        }
    }
}
