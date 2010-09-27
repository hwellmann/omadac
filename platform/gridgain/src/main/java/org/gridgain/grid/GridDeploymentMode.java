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
 * Grid deployment mode. Deployment mode is specified at grid startup via
 * {@link GridConfiguration#getDeploymentMode()} configuration property
 * (it can also be specified in Spring XML configuration file). The main
 * difference between all deployment modes is how classes and user resources
 * are loaded on remote nodes via peer-class-loading mechanism. User resources
 * can be instances of caches, databased connections, or any other class
 * specified by user with {@link GridUserResource @GridUserResource} annotation.
 * <p>
 * Refer to {@link GridUserResource} documentation and examples for more
 * information on how user resources are created and injected.
 * <p>
 * The following deployment modes are supported:
 * <ul>
 * <li>{@link #PRIVATE}</li>
 * <li>{@link #ISOLATED}</li>
 * <li>{@link #SHARED}</li>
 * <li>{@link #CONTINUOUS}</li>
 * </ul>
 * <h1 class="header">User Version</h1>
 * User version comes into play whenever you would like to redeploy tasks deployed
 * in {@link #SHARED} or {@link #CONTINUOUS} modes. By default, GridGain will
 * automatically detect if class-loader changed or a node is restarted. However,
 * if you would like to change and redeploy code on a subset of nodes, or in
 * case of {@link #CONTINUOUS} mode to kill the ever living deployment, you should
 * change ther user version.
 * <p>
 * User version is specified in <tt>META-INF/gridgain.xml</tt> file as follows:
 * <pre name="code" class="xml">
 *    &lt;!-- User version. --&gt;
 *    &lt;bean id="userVersion" class="java.lang.String"&gt;
 *        &lt;constructor-arg value="0"/&gt;
 *    &lt;/bean>
 * </pre>
 * By default, all gridgain startup scripts (<tt>gridgain.sh</tt> or <tt>gridgain.bat</tt>)
 * pick up user version from <tt>GRIDGAIN_HOME/config/userversion</tt> folder. Usually, it
 * is just enough to update user version under that folder, however, in case of <tt>GAR</tt>
 * or <tt>JAR</tt> deployment, you should remember to provide <tt>META-INF/gridgain.xml</tt>
 * file with desired user version in it.
 * <p>
 * <h1 class="header">Always-Local Development</h1>
 * GridGain deployment (regardless of mode) allows you to develop everything as you would
 * locally. You never need to specifically write any kind of code for remote nodes. For
 * example, if you need to use a distributed cache from your {@link GridJob}, then you can
 * the following:
 * <ol>
 *  <li>
 *      Simply startup stand-alone GridGain nodes by executing
 *      <tt>GRIDGAIN_HOME/gridgain.{sh|bat}</tt> scripts.
 *  <li>
 *      Inject your cache instance into your jobs via
 *      {@link GridUserResource @GridUserResource} annotation. The cache can be initialized
 *      and destroyed with {@link GridUserResourceOnDeployed @GridUserResourceOnDeployed} and
 *      {@link GridUserResourceOnUndeployed @GridUserResourceOnUndeployed} annotations.
 *  </li>
 *  <li>
 *      Now, all jobs executing locally or remotely can have a single instance of cache
 *      on every node, and all jobs can access instances stored by any other job without
 *      any need for explicit deployment.
 *  </li>
 * </ol>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public enum GridDeploymentMode {
    /**
     * In this mode deployed classes do not share user resources
     * (see {@link GridUserResource}). Basicaly, user resources are created
     * once per deployed task class and then get reused for all executions.
     * <p>
     * Note that classes deployed within the same class loader on master
     * node, will still share the same class loader remotely on worker nodes.
     * However, tasks deployed from different master nodes will not
     * share the same class loader on worker nodes, which is useful in
     * development when different developers can be working on different
     * versions of the same classes.
     * <p>
     * Also note that resources are associated with task deployment,
     * not task execution. If the same deployed task gets executed multiple
     * times, then it will keep reusing the same user resources
     * every time.
     */
    PRIVATE,

    /**
     * Unlike {@link #PRIVATE} mode, where different deployed tasks will
     * never use the same instance of user resources, in <tt>ISOLATED</tt>
     * mode, tasks or classes deployed within the same class loader
     * will share the same instances of user resources (see {@link GridUserResource}).
     * This means that if multiple tasks classes are loaded by the same
     * class loader on master node, then they will share instances
     * of user resources on worker nodes. In other words, user resources
     * get initialized once per class loader and then get reused for all
     * consecutive executions.
     * <p>
     * Note that classes deployed within the same class loader on master
     * node, will still share the same class loader remotely on worker nodes.
     * However, tasks deployed from different master nodes will not
     * share the same class loader on worker nodes, which is especially
     * useful when different developers can be working on different versions
     * of the same classes.
     * <p>
     * <tt>ISOLATED</tt> deployment mode is default mode used by the grid.
     */
    ISOLATED,

    /**
     * Same as {@link #ISOLATED}, but now tasks from
     * different master nodes with the same user version and same
     * class loader will share the same class loader on remote
     * nodes. Classes will be undeployed whenever all master
     * nodes leave grid or user version changes.
     * <p>
     * The advantage of this approach is that it allows tasks coming from
     * different master nodes share the same instances of user resources
     * (see {@link GridUserResource}) on worker nodes. This allows for all
     * tasks executing on remote nodes to reuse, for example, the same instances of
     * connection pools or caches. When using this mode, you can
     * startup multiple stand-alone GridGain worker nodes, define user resources
     * on master nodes and have them initialize once on worker nodes regardless
     * of which master node they came from.
     * <p>
     * This method is specifically useful in production as, in comparison
     * to {@link #ISOLATED} deployment mode, which has a scope of single
     * class loader on a single master node, {@link #SHARED} mode broadens the
     * deployment scope to all master nodes.
     * <p>
     * Note that classes deployed in {@link #SHARED} mode will be undeployed if
     * all master nodes left grid or if user version changed. User version can
     * be specified in <tt>META-INF/gridgain.xml</tt> file as a Spring bean
     * property with name <tt>userVersion</tt>. This file has to be in the class
     * path of the class used for task execution.
     */
    SHARED,

    /**
     * Same as {@link #SHARED} deployment mode, but user resources
     * (see {@link GridUserResource}) will not be undeployed even after all master
     * nodes left grid. Tasks from different master nodes with the same user
     * version and same class loader will share the same class loader on remote
     * worker nodes. Classes will be undeployed whenever user version changes.
     * <p>
     * The advantage of this approach is that it allows tasks coming for
     * different master nodes share the same instances of user resources
     * (see {@link GridUserResource}) on worker nodes. This allows for all
     * tasks executing on remote nodes to reuse, for example, the same instances of
     * connection pools or caches. When using this mode, you can
     * startup multiple stand-alone GridGain worker nodes, define user resources
     * on master nodes and have them initialize once on worker nodes regardless
     * of which master node they came from.
     * <p>
     * This method is specifically useful in production as, in comparison
     * to {@link #ISOLATED} deployment mode, which has a scope of single
     * class loader on a single master node, {@link #CONTINUOUS} mode broadens
     * the deployment scope to all master nodes.
     * <p>
     * Note that classes deployed in {@link #CONTINUOUS} mode will be undeployed
     * only if user version changes. User version can be specified in
     * <tt>META-INF/gridgain.xml</tt> file as a Spring bean property with name
     * <tt>userVersion</tt>. This file has to be in the class
     * path of the class used for task execution.
     */
    CONTINUOUS
}
