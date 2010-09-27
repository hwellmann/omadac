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

package org.gridgain.grid.test;

/**
 * GridGain JUnit VM configuration parameters that can be used to
 * override defaults.Note that VM configuration parameters have priority
 * over the same configuration specified in {@link GridifyTest @GridifyTest}
 * annotation.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public enum GridTestVmParameters {
    /**
     * Name of VM parameter to disable grid. The value of the parameter
     * should be either <tt>true</tt> or <tt>false</tt>. The default
     * value is <tt>false</tt>.
     * <p>
     * For example, the parameter <tt>"-DGRID_DISABLED=true"</tt> will
     * disable grid and force all tests to execute locally.
     */
    GRID_DISABLED,

    /**
     * Name of VM parameter to specify full class name of JUnit router. By
     * default {@link GridTestRouterAdapter} name is used.
     * <p>
     * For example, the parameter <tt>"-DGRID_JUNIT_ROUTER=foo.bar.MyJunitRouter"</tt>
     * will specify a custom JUnit router. The specified router muster have an empty
     * constructor.
     */
    GRID_TEST_ROUTER,

    /**
     * Name of VM parameter to specify path to GridGain configuration used
     * to run distributed JUnits. By default <tt>"config/junit/junit-spring.xml"</tt>
     * is used.
     * <p>
     * For example, the parameter <tt>"-DGRID_JUNIT_CONFIG="c:/foo/bar/mygrid-spring.xml"</tt>
     * overrides the default configuration path.
     */
    GRID_CONFIG,

    /**
     * Optional timeout in milliseconds for distributed test suites. By default,
     * test suites never timeout.
     * For example, the parameter <tt>"-DGRID_TEST_TIMEOUT=600000"</tt> will
     * stop test suite execution after <tt>10</tt> minutes.
     */
    GRID_TEST_TIMEOUT,

    /**
     * Name of VM parameter to specify whether tests should be preferably routed
     * to remote nodes. This parameter is used by {@link GridTestRouterAdapter} which
     * will use remote nodes if there any, otherwise local node will still be used.
     * <p>
     * The value of this parameter is either <tt>true</tt> or <tt>false</tt>. For example,
     * the parameter <tt>"-DGRID_ROUTER_PERFER_REMOTE=true"</tt> will tell the router
     * to prefer remote nodes for execution.
     */
    GRID_ROUTER_PREFER_REMOTE,

    /**
     * Name of VM parameter that specifies name of the grid started for distributed junits.
     * This parameter should not be set explicitely. GridGain will detect grid name from
     * the configuration file and set it as system properties, so nested JUnit tests or
     * suites will be able to detect if grid has been started to avoid double starts.
     */
    GRID_NAME,
}
