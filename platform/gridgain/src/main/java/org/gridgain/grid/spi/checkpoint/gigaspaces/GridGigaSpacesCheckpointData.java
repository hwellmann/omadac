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

package org.gridgain.grid.spi.checkpoint.gigaspaces;

import com.j_spaces.core.client.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Wrapper of all checkpoints that are saved to GigaSpaces. It
 * extends every checkpoint with it's name.
 * <p>
 * Due to GigaSpaces design:
 * <ul>
 *      <li>This class have to be public, and</li>
 *      <li>It have to be in a GigaSpaces class path.</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridGigaSpacesCheckpointData extends MetaDataEntry {
    /**
     * Checkpoint data. It's public due to GigaSpaces design.
     */
    @SuppressWarnings({"PublicField"})
    public byte[] state;

    /**
     * Creates new instance of checkpoint wrapper.
     * This constructor is used internally by GigaSpaces.
     */
    public GridGigaSpacesCheckpointData() {
        // No-op.
    }

    /**
     * Creates new instance of checkpoint data wrapper for the given state.
     *
     * @param state Checkpoint data.
     */
    GridGigaSpacesCheckpointData(byte[] state) {
        this.state = state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridGigaSpacesCheckpointData.class, this);
    }
}
