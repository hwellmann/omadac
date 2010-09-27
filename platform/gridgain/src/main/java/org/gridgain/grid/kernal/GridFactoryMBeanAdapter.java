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

import org.gridgain.grid.*;

/**
 * Management bean that provides access to {@link GridFactory}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridFactoryMBeanAdapter implements GridFactoryMBean {
    /**
     * {@inheritDoc}
     */
    public String getState() {
        return GridFactory.getState().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getState(String name) {
        if (name.length() == 0) {
            name = null;
        }

        return GridFactory.getState(name).toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean stop(boolean cancel) {
        return GridFactory.stop(cancel);
    }

    /**
     * {@inheritDoc}
     */
    public boolean stop(String name, boolean cancel) {
        return GridFactory.stop(name, cancel);
    }

    /**
     * {@inheritDoc}
     */
    public void stopAll(boolean cancel) {
        GridFactory.stopAll(cancel);
    }

    /**
     * {@inheritDoc}
     */
    public boolean stop(boolean cancel, boolean wait) {
        return GridFactory.stop(cancel, wait);
    }

    /**
     * {@inheritDoc}
     */
    public boolean stop(String name, boolean cancel, boolean wait) {
        return GridFactory.stop(name, cancel, wait);
    }

    /**
     * {@inheritDoc}
     */
    public void stopAll(boolean cancel, boolean wait) {
        GridFactory.stopAll(cancel, wait);
    }
}
