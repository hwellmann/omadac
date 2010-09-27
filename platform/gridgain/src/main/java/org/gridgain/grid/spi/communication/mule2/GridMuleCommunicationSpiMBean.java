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

package org.gridgain.grid.spi.communication.mule2;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean that provides read-only access to the Mule communication
 * SPI configuration.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to the Mule communication SPI configuration.")
public interface GridMuleCommunicationSpiMBean extends GridSpiManagementMBean {
    /**
     * Gets either absolute or relative to GridGain installation home folder path to Mule XML
     * configuration file.
     *
     * @return Path to Mule configuration file.
     */
    @GridMBeanDescription("Path to Mule configuration file.")
    public String getConfigurationFile();

    /**
     * Gets name for the component registered in Mule configuration.
     *
     * @return Name of the component.
     */
    @GridMBeanDescription("Name for the component registered in Mule configuration.")
    public String getComponentName();

    /**
     * Gets component input endpoint URI.
     *
     * @return Input endpoint URI.
     */
    @GridMBeanDescription("Input endpoint URI.")
    public String getEndpointUri();
}
