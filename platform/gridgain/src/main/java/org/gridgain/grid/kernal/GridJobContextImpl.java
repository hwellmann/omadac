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

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.jsr305.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobContextImpl implements GridJobContext {
    /** */
    private UUID jobId = null;

    /** */
    @GridToStringInclude
    private final Map<Serializable, Serializable> attrs = new HashMap<Serializable, Serializable>(1);

    /**
     *
     * @param jobId Job ID.
     */
    public GridJobContextImpl(UUID jobId) {
        assert jobId != null : "ASSERTION [line=50, file=src/java/org/gridgain/grid/kernal/GridJobContextImpl.java]";

        this.jobId = jobId;
    }

    /**
     *
     * @param jobId Job ID.
     * @param attrs Job attributes.
     */
    public GridJobContextImpl(UUID jobId, Map<? extends Serializable, ? extends Serializable> attrs) {
        this(jobId);

        synchronized (this.attrs) {
            this.attrs.putAll(attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     * {@inheritDoc}
     */
    public void setAttribute(Serializable key, @Nullable Serializable val) {
        GridArgumentCheck.checkNull(key, "key");

        synchronized (attrs) {
            attrs.put(key, val);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAttributes(Map<? extends Serializable, ? extends Serializable> attrs) {
        GridArgumentCheck.checkNull(attrs, "attrs");

        synchronized (attrs) {
            this.attrs.putAll(attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Serializable getAttribute(Serializable key) {
        GridArgumentCheck.checkNull(key, "key");

        synchronized (attrs) {
            return attrs.get(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<? extends Serializable, ? extends Serializable> getAttributes() {
        synchronized (attrs) {
            if (attrs.isEmpty() == true) {
                return Collections.emptyMap();
            }

            return new HashMap<Serializable, Serializable>(attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobContextImpl.class, this);
    }
}
