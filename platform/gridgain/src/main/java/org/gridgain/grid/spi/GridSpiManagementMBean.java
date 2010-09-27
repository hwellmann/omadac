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

package org.gridgain.grid.spi;

import java.util.*;

import org.gridgain.apache.*;
import org.gridgain.grid.util.mbean.*;

/**
 * This interface defines basic MBean for all SPI implementations. Every SPI implementation
 * should provide implementation for this MBean interface. Note that SPI implementation can extend this
 * interface as necessary.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridSpiManagementMBean {
    /**
     * Gets SPI provider's author.
     *
     * @return SPI provider's author.
     */
    @GridMBeanDescription("SPI provider's author.")
    public String getAuthor();

    /**
     * Gets vendor's URL.
     *
     * @return Vendor's URL.
     */
    @GridMBeanDescription("Vendor's URL.")
    public String getVendorUrl();

    /**
     * Gets vendor's email (info or support).
     *
     * @return Vendor's email (info or support).
     */
    @GridMBeanDescription("Vendor's email (info or support).")
    public String getVendorEmail();

    /**
     * Gets SPI implementation version.
     *
     * @return SPI implementation version.
     */
    @GridMBeanDescription("SPI implementation version.")
    public String getVersion();

    /**
     * Gets string presentation of the start timestamp.
     *
     * @return String presentation of the start timestamp.
     */
    @GridMBeanDescription("String presentation of the start timestamp.")
    public String getStartTimestampFormatted();

    /**
     * Gets string presentation of up-time for this SPI.
     *
     * @return String presentation of up-time for this SPI.
     */
    @GridMBeanDescription("String presentation of up-time for this SPI.")
    public String getUpTimeFormatted();

    /**
     * Get start timestamp of this SPI.
     *
     * @return Start timestamp of this SPI.
     */
    @GridMBeanDescription("Start timestamp of this SPI.")
    public long getStartTimestamp();

    /**
     * Gets up-time of this SPI in ms.
     *
     * @return Up-time of this SPI.
     */
    @GridMBeanDescription("Up-time of this SPI in ms.")
    public long getUpTime();

    /**
     * Gets Gridgain installation home folder (i.e. ${GRIDGAIN_HOME});
     *
     * @return Gridgain installation home folder.
     */
    @GridMBeanDescription("Gridgain installation home folder.")
    public String getGridGainHome();

    /**
     * Gets ID of the local node.
     *
     * @return ID of the local node.
     */
    @GridMBeanDescription("ID of the local node.")
    public UUID getLocalNodeId();

    /**
     * Gets name of the SPI. 
     *
     * @return Name of the SPI.
     */
    @GridMBeanDescription("Name of the SPI.")
    public String getName();
}
