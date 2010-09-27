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

package org.gridgain.grid.loaders.weblogic;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.loaders.*;
import weblogic.common.*;

/**
 * GridGain loader for WebLogic implemented as a pair of start and shutdown
 * classes. This is a shutdown class. Please consult WebLogic documentation
 * on how to configure startup classes in Weblogic. Weblogic loader should
 * be used for tight integration with Weblogic AS. Specifically, Weblogic
 * loader integrates GridGain with Weblogic logging, MBean server, work
 * manager (<a target=_blank href="http://jcp.org/en/jsr/detail?id=237">JSR-237</a>).
 * <p>
 * The following steps should be taken to configure startup and shutdown classes:
 * <ol>
 * <li>
 *      Add Startup and Shutdown Class in admin console (<tt>Environment -> Startup & Shutdown Classes -> New</tt>).
 * </li>
 * <li>
 *      Add the following parameters for startup class:
 *      <ul>
 *      <li>Name: <tt>GridWeblogicStartup</tt></li>
 *      <li>Classname: <tt>org.gridgain.grid.loaders.weblogic.GridWeblogicStartup</tt></li>
 *      <li>Arguments: <tt>cfgFilePath=config/default-spring.xml</tt></li>
 *      </ul>
 * </li>
 * <li>
 *      Add the following parameters for shutdown class:
 *      <ul>
 *      <li>Name: <tt>GridWeblogicShutdown</tt></li>
 *      <li>Classname: <tt>org.gridgain.grid.loaders.weblogic.GridWeblogicShutdown</tt></li>
 *      </ul>
 * </li>
 * <li>
 *      Change classpath for WebLogic server in startup script:
 *      <tt>CLASSPATH="${CLASSPATH}:${GRIDGAIN_HOME}/gridgain.jar:${GRIDGAIN_HOME}/libs/"</tt>
 * </li>
 * </ol>
 * <p>
 * For more information see
 * <a target=wiki href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Configuring+and+Starting+GridGain">Configuring and Starting GridGain</a> and
 * <a target=_blank href="http://edocs.bea.com/wls/docs100/ConsoleHelp/taskhelp/startup_shutdown/UseStartupAndShutdownClasses.html">Startup and Shutdown Classes.</a>
 * <p>
 * <b>Note</b>: Weblogic is not shipped with GridGain. If you don't have Weblogic, you need to
 * download it separately. See <a target=_blank href="http://www.bea.com">http://www.bea.com</a> for
 * more information.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridLoader(description = "Weblogic loader")
public class GridWeblogicShutdown implements GridWeblogicShutdownMBean {
    /**
     * See <a href="http://edocs.bea.com/wls/docs81/javadocs/weblogic/common/T3ShutdownDef.html">
     * http://edocs.bea.com/wls/docs81/javadocs/weblogic/common/T3ShutdownDef.html</a> for more
     * information.
     *
     * @param str Virtual class name.
     * @param params Name-value parameters supplied with shutdown class registration.
     * @return Return string.
     * @throws Exception Thrown if error occurred.
     */
    @Deprecated
    @SuppressWarnings({ "deprecation", "unchecked" })
    public String shutdown(String str, Hashtable params) throws Exception {
        GridFactory.stopAll(true);

        return getClass().getSimpleName() + " stopped successfully.";
    }

    /**
     * See <a href="http://edocs.bea.com/wls/docs81/javadocs/weblogic/common/T3ShutdownDef.html">
     * http://edocs.bea.com/wls/docs81/javadocs/weblogic/common/T3ShutdownDef.html</a> for more
     * information.
     *
     * @param t3ServicesDef Weblogic services accessor.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public void setServices(T3ServicesDef t3ServicesDef) {
        // No-op.
    }
}
