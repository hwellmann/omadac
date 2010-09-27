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

package org.gridgain.grid.util;

import org.gridgain.grid.util.tostring.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <V1> First value type.
 * @param <V2> Second value type.
 */
public class GridPair<V1, V2> {
    /** */
    @GridToStringInclude
    private V1 val1 = null;

    /** */
    @GridToStringInclude
    private V2 val2 = null;

    /**
     * Empty constructor.
     */
    public GridPair() {
        // No-op.
    }

    /**
     *
     * @param val1 First value.
     * @param val2 Second value.
     */
    public GridPair(V1 val1, V2 val2) {
        this.val1 = val1;
        this.val2 = val2;
    }

    /**
     *
     * @return First value.
     */
    public V1 getValue1() {
        return val1;
    }

    /**
     *
     * @return Second value.
     */
    public V2 getValue2() {
        return val2;
    }

    /**
     *
     * @param val1 First value.
     */
    public void setValue1(V1 val1) {
        this.val1 = val1;
    }

    /**
     *
     * @param val2 Second value.
     */
    public void setValue2(V2 val2) {
        this.val2 = val2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return val1 == null ? 0 : val1.hashCode() * 31 + (val2 == null ? 0 : val2.hashCode());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GridPair == false) {
            return false;
        }

        GridPair pair = (GridPair)obj;

        // Both nulls or equals.
        return ((val1 == null ? pair.val1 == null : val1.equals(pair.val1) == true) &&
            (val2 == null ? pair.val2 == null : val2.equals(pair.val2) == true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridPair.class, this);
    }
}
