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
import org.gridgain.apache.*;

/**
 * Annotates a field or a setter method for any custom resources injection.
 * It can be injected into grid tasks and grid jobs. Use it when you would
 * like, for example, to inject something like JDBC connection pool into tasks
 * or jobs - this way your connection pool will be instantiated only once
 * per task and reused for all executions of this task.
 * <p>
 * You can inject other resources into your user resource.
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
 * <h1 class="header">Resource Class</h1>
 * The resource will be created based on the {@link #resourceClass()} value. If
 * If <tt>resourceClass</tt> is not specified, then field type or setter parameter
 * type will be used to infer the class type of the resource. Set {@link #resourceClass()}
 * to a specific value if the class of resource cannot be inferred from field or setter
 * declaration (for example, if field is an interface).
 * <p>
 * <h1 class="header">Resource Life Cycle</h1>
 * User resource will be instantiated once on every node where task is deployed.
 * Basically there will always be only one instance of resource on any
 * grid node for any task class. Every node will instantiate
 * it's own copy of user resources used for every deployed task (see
 * {@link GridUserResourceOnDeployed} and {@link GridUserResourceOnUndeployed}
 * annotation for resource deployment and undeployment callbacks). For this
 * reason <b>resources should not be sent to remote nodes and should
 * always be declared as transient</b> just in case.
 * <p>
 * Note that an instance of user resource will be created for every deployed task.
 * In case if you need a singleton resource instances on grid nodes (not per-task),
 * you can use {@link GridSpringApplicationContextResource} for injecting per-VM
 * singleton resources configured in Spring.
 * <p>
 * <h1 class="header">Examples</h1>
 * Here is how injection would typically happen:
 * <pre name="code" class="java">
 * public class MyGridJob implements GridJob {
 *      ...
 *      &#64;GridUserResource
 *      private transient MyUserResource rsrc;
 *      ...
 *  }
 * </pre>
 * or
 * <pre name="code" class="java">
 * public class MyGridJob implements GridJob {
 *     ...
 *     private transient MyUserResource rsrc = null;
 *     ...
 *     &#64;GridUserResource
 *     public void setMyUserResource(MyUserResource rsrc) {
 *          this.rsrc = rsrc;
 *     }
 *     ...
 * }
 * </pre>
 * where resource class can look like this:
 * <pre name="code" class="java">
 * public class MyUserResource {
 *     ...
 *     // Inject logger (or any other resource).
 *     &#64;GridLoggerResource
 *     private GridLogger log = null;
 *
 *     // Inject grid instance (or any other resource).
 *     &#64;GridInstanceResource
 *     private Grid grid = null;
 *
 *     // Deployment callback.
 *     &#64;GridUserResourceOnDeployed
 *     public void deploy() {
 *        // Some initialization logic.
 *        ...
 *     }
 *
 *     // Undeployment callback.
 *     &#64;GridUserResourceOnUndeployed
 *     public void undeploy() {
 *        // Some clean up logic.
 *        ...
 *     }
 * }
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Apache20LicenseCompatible
public @interface GridUserResource {
    /**
     * Optional resource class. By default the type of the resource variable
     * or setter parameter will be used.
     */
    Class<?> resourceClass() default Void.class;
}
