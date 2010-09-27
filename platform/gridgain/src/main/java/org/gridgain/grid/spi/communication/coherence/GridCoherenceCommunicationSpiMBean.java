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

package org.gridgain.grid.spi.communication.coherence;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean that provides access to the Coherence-based communication 
 * SPI configuration.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean provides access to the Coherence-based communication SPI configuration.")
public interface GridCoherenceCommunicationSpiMBean extends GridSpiManagementMBean {
    /**
     * Gets Coherence member identifier string representation.
     *
     * @return Coherence member identifier string representation.
     */
    @GridMBeanDescription("Coherence member identifier string representation.")
    public String getLocalUid();

    /**
     * Gets sending acknowledgment property.
     *
     * @return Sending acknowledgment property.
     */
    @GridMBeanDescription("Sending acknowledgment property.")
    public boolean isAcknowledgment();

    /**
     * Gets Coherence service invocation name.
     *
     * @return Coherence service invocation name.
     */
    @GridMBeanDescription("Coherence service invocation name.")
    public String getServiceName();
}
