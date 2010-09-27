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

import org.gridgain.grid.util.mail.outbox.smtp.*;

/**
 * This class provides factory for creating {@link GridMailOutbox} instances.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridMailOutboxFactory {
    /**
     * Enforces singleton.
     */
    private GridMailOutboxFactory() {
        // No-op.
    }
    
    /**
     * Creates mail outbox with specified configuration.
     *
     * @param cfg Configuration of a creating outbox.
     * @return Mail outbox.
     */
    public static GridMailOutbox createOutbox(GridMailOutboxConfiguration cfg) {
        assert cfg != null : "ASSERTION [line=47, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxFactory.java]";

        // Outbox configuration must have mail protocol set.
        assert cfg.getProtocol() != null : "ASSERTION [line=50, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxFactory.java]";

        // Unsupported outbox mail protocol.
        assert cfg.getProtocol() == GridMailOutboxProtocol.SMTP || cfg.getProtocol() == GridMailOutboxProtocol.SMTPS : "ASSERTION [line=53, file=src/java/org/gridgain/grid/util/mail/outbox/GridMailOutboxFactory.java]";

        return new GridSmtpOutbox(cfg);
    }
}
