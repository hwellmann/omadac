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

package org.gridgain.grid.resources;

import java.lang.annotation.*;
import java.util.*;

import org.gridgain.apache.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;

/**
 * Annotates a field or a setter method for injection of local node {@link UUID} resource. <tt>Node UUID</tt>
 * is a globally unique node identifier and is provided to grid via {@link GridConfiguration}.
 * <p>
 * Local node ID can be injected into instances of following classes:
 * <ul>
 * <li>{@link GridTask}</li>
 * <li>{@link GridJob}</li>
 * <li>{@link GridSpi}</li>
 * <li>{@link GridLifecycleBean}</li>
 * <li>{@link GridUserResource @GridUserResource}</li>
 * </ul>
 * <p>
 * Here is how injection would typically happen:
 * <pre name="code" class="java">
 * public class MyGridJob implements GridJob {
 *      ...
 *      &#64;GridLocalNodeIdResource
 *      private UUID nodeId;
 *      ...
 *  }
 * </pre>
 * or
 * <pre name="code" class="java">
 * public class MyGridJob implements GridJob {
 *     ...
 *     private UUID nodeId = null;
 *     ...
 *     &#64;GridLocalNodeIdResource
 *     public void setLocalNodeId(UUID nodeId) {
 *          this.nodeId = nodeId;
 *     }
 *     ...
 * }
 * </pre>
 * <p>
 * See {@link GridConfiguration#getNodeId()} for Grid configuration details.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Apache20LicenseCompatible
public @interface GridLocalNodeIdResource {
    // No-op.
}
