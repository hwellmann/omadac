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

package org.gridgain.grid.spi.discovery.coherence;

import java.io.*;
import java.net.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;
import com.tangosol.util.*;
import com.tangosol.net.*;

/**
 * Contains data from Coherence {@link Member} interface. An instance of this class
 * can be accessed by calling {@link GridNode#getAttribute(String) GridNode.getAttribute(GridCoherenceDiscoverySpi.ATTR_COHERENCE_MBR)}
 * method.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridCoherenceMember implements Serializable {
    /** */
    private int id = -1;

    /** */
    private int machineId = -1;

    /** */
    private String machineName = null;

    /** */
    private String mbrName = null;

    /** */
    private InetAddress addr = null;

    /** */
    private int port = -1;

    /** */
    private long tstamp = -1;

    /** */
    private UID uid = null;

    /**
     * Creates bean with member data.
     *
     * @param mbr Coherence node (member).
     */
    public GridCoherenceMember(Member mbr) {
        id = mbr.getId();
        machineId = mbr.getMachineId();
        machineName = mbr.getMachineName();
        mbrName = mbr.getMemberName();
        addr = mbr.getAddress();
        port = mbr.getPort();
        tstamp = mbr.getTimestamp();
        uid = mbr.getUid();
    }

    /**
     * Return a small number that uniquely identifies the Member at this
     * point in time and does not change for the life of this Member.
     *
     * @return Mini-id of the Member.
     */
    public int getId() {
        return id;
    }

    /**
     * Return the Member's machine Id.
     *
     * @return Member's machine Id.
     */
    public int getMachineId() {
        return machineId;
    }

    /**
     * Determine the configured name for the Machine (such as a host name)
     * in which this Member resides.
     *
     * @return configured Machine name or null.
     */
    public String getMachineName() {
        return machineName;
    }

    /**
     * Determine the configured name for the Member.
     *
     * @return Configured Member name or null
     */
    public String getMemberName() {
        return mbrName;
    }

    /**
     * Return the IP address of the Member's DatagramSocket
     * for point-to-point communication.
     *
     * @return IP address of the Member's DatagramSocket.
     */
    public InetAddress getAddress() {
        return addr;
    }

    /**
     * Return the port of the Member's DatagramSocket
     * for point-to-point communication.
     *
     * @return Port of the Member's DatagramSocket.
     */
    public int getPort() {
        return port;
    }

    /**
     * Return the date/time value (in cluster time)
     * that the Member joined.
     *
     * @return Cluster date/time value that the Member joined.
     */
    public long getTimestamp() {
        return tstamp;
    }

    /**
     * Return the unique Coherence identifier of the Member.
     *
     * @return Unique identifier of the Member.
     */
    public UID getUid() {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCoherenceMember.class, this);
    }
}
