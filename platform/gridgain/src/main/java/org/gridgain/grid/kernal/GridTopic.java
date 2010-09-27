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

package org.gridgain.grid.kernal;

import java.util.*;
import org.gridgain.grid.util.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public enum GridTopic {
    /** */
    JOB,

    /** */
    TASK,

    /** */
    CHECKPOINT,

    /** */
    CANCEL,

    /** */
    CLASSLOAD_TOPIC,

    /** */
    EVTSTORAGE_TOPIC;

    /** */
    private final String topic = getClass().getSimpleName().toUpperCase() + '-' + name().toUpperCase();

    /**
     *
     * @return Grid message topic.
     */
    public String topic() {
        return topic;
    }

    /**
     * This method uses cached instances of {@link StringBuilder} to avoid
     * constant resizing and object creation.
     *
     * @param ids Topic IDs.
     * @return Grid message topic with specified IDs.
     */
    public String topic(UUID... ids) {
        StringBuilder buf = GridStringBuilderFactory.acquire();

        try {
            buf.append(topic);

            for (UUID id : ids) {
                buf.append('-').
                    append(id.getLeastSignificantBits()).
                    append('-').
                    append(id.getMostSignificantBits());
            }

            return buf.toString();
        }
        finally {
            GridStringBuilderFactory.release(buf);
        }
    }
}
