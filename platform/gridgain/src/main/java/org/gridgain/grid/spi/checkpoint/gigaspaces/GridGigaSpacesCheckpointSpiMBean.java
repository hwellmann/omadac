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

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;
import org.openspaces.core.*;

/**
 * Management bean that provides general administrative and configuration information
 * about GigaSpaces checkpoint SPI.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean provides information about GigaSpaces checkpoint SPI.")
public interface GridGigaSpacesCheckpointSpiMBean extends GridSpiManagementMBean {
    /**
     * Gets GigaSpaces URL.
     *
     * @return GigaSpaces URL.
     */
    @GridMBeanDescription("GigaSpaces URL.")
    public String getSpaceUrl();
    
    /**
     * Gets space object used by SPI.
     * 
     * @return Space object to be used by this SPI.
     */
    @GridMBeanDescription("Space object to be used by this SPI.")
    public GigaSpace getSpace();
}
