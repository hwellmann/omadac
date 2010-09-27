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

package org.gridgain.grid;

import org.gridgain.apache.*;
import org.gridgain.grid.resources.*;

/**
 * A bean that reacts to grid lifecycle events defined in {@link GridLifecycleEventType}.
 * Use this bean whenever you need to plug some custom logic before or after
 * grid startup and stopping routines.
 * <p>
 * There are four events you can react to:
 * <ul>
 * <li>
 *   {@link GridLifecycleEventType#BEFORE_GRID_START} invoked before grid startup
 *   routine is initiated. Note that grid is not available during this event,
 *   therefore if you injected a grid instance via {@link GridInstanceResource}
 *   annotation, you cannot use it yet.
 * </li>
 * <li>
 *   {@link GridLifecycleEventType#AFTER_GRID_START} invoked right after grid
 *   has started. At this point, if you injected a grid instance via
 *   {@link GridInstanceResource} annotation, you can start using it.
 * </li>
 * <li>
 *   {@link GridLifecycleEventType#BEFORE_GRID_STOP} invoked right before grid
 *   stop routine is initiated. Grid is still available at this stage, so
 *   if you injected a grid instance via  {@link GridInstanceResource} annotation,
 *   you can use it.
 * </li>
 * <li>
 *   {@link GridLifecycleEventType#AFTER_GRID_STOP} invoked right after grid
 *   has stopped. Note that grid is not available during this event.
 * </li>
 * </ul>
 * <h1 class="header">Resource Injection</h1>
 * Lifecycle beans can be injected using IoC (dependency injection) with
 * grid resources. Both, field and method based injection are supported.
 * The following grid resources can be injected:
 * <ul>
 * <li>{@link GridLoggerResource}</li>
 * <li>{@link GridLocalNodeIdResource}</li>
 * <li>{@link GridHomeResource}</li>
 * <li>{@link GridMBeanServerResource}</li>
 * <li>{@link GridExecutorServiceResource}</li>
 * <li>{@link GridMarshallerResource}</li>
 * <li>{@link GridSpringApplicationContextResource}</li>
 * <li>{@link GridSpringResource}</li>
 * <li>{@link GridInstanceResource}</li>
 * </ul>
 * Refer to corresponding resource documentation for more information.
 * <p>
 * <h1 class="header">Usage</h1>
 * If you need to tie your application logic into GridGain lifecycle,
 * you can configure lifecycle beans via standard grid configuration, add your
 * application library dependencies into <tt>GRIDGAIN_HOME/libs/ext</tt> folder, and
 * simply start <tt>GRIDGAIN_HOMNE/gridgain.{sh|bat}</tt> scripts.
 * <p>
 * <h1 class="header">Configuration</h1>
 * Grid lifecycle beans can be configured programmatically as follows:
 * <pre name="code" class="java">
 * Collection&lt;GridLifecycleBean&gt; lifecycleBeans = new ArrayList&lt;GridLifecycleBean&gt;();
 *
 * Collections.addAll(lifecycleBeans, new FooBarLifecycleBean1(), new FooBarLifecycleBean2());
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * cfg.setLifecycleBeans(lifecycleBeans);
 *
 * // Start grid with given configuration.
 * GridFactory.start(cfg);
 * </pre>
 * or from Spring XML configuration file as follows:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.cfg" class="org.gridgain.grid.GridConfigurationAdapter" scope="singleton"&gt;
 *    ...
 *    &lt;property name="lifecycleBeans"&gt;
 *       &lt;list&gt;
 *          &lt;bean class="foo.bar.FooBarLifecycleBean1"/&gt;
 *          &lt;bean class="foo.bar.FooBarLifecycleBean2"/&gt;
 *       &lt;/list&gt;
 *    &lt;/property&gt;
 *    ...
 * &lt;/bean&gt;
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridLifecycleBean {
    /**
     * This method is called when lifecycle event occurs.
     *
     * @param evt Lifecycle event.
     */
    public void onLifecycleEvent(GridLifecycleEventType evt);
}
