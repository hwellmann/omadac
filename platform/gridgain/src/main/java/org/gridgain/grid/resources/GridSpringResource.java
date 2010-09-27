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
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;

/**
 * Annotates a field or a setter method for injection of resource
 * from Spring <tt>ApplicationContext</tt>. Use it whenever you would
 * like to inject resources specified in Spring application context of XML
 * configuration.
 * <p>
 * Logger can be injected into instances of following classes:
 * <ul>
 * <li>{@link GridTask}</li>
 * <li>{@link GridJob}</li>
 * <li>{@link GridSpi}</li>
 * <li>{@link GridLifecycleBean}</li>
 * <li>{@link GridUserResource @GridUserResource}</li>
 * </ul>
 * <p>
 * <h1 class="header">Resource Name</h1>
 * This is a mandatory parameter. Resource name will be used to access
 * Spring resources from Spring <tt>ApplicationContext</tt> or XML configuration.
 * <p>
 * Note that Spring resources cannot be peer-class-loaded. They must be available in
 * every <tt>ApplcationContext</tt> or Spring XML configuration on every grid node.
 * For this reason, if injected into a {@link java.io.Serializable} class, they must
 * be declared as <tt>transient</tt>.
 * <p>
 * The lifecycle of Spring resources is controled by Spring container.
 * <p>
 * <h1 class="header">Examples</h1>
 * Here is how injection would typically happen:
 * <pre name="code" class="java">
 * public class MyGridJob implements GridJob {
 *      ...
 *      &#64;GridSpringResource(resourceName = "bean-name")
 *      private transient MyUserBean rsrc;
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
 *     private transient MyUserBean rsrc = null;
 *     ...
 *     &#64;GridSpringResource(resourceName = "bean-name")
 *     public void setMyUserBean(MyUserBean rsrc) {
 *          this.rsrc = rsrc;
 *     }
 *     ...
 * }
 * </pre>
 * and user resource <tt>MyUserResource</tt>
 * <pre name="code" class="java">
 * public class MyUserResource {
 *     ...
 *     &#64;GridSpringResource(resourceName = "bean-name")
 *     private MyUserBean rsrc;
 *     ...
 *     // Inject logger (or any other resource).
 *     &#64;GridLoggerResource
 *     private GridLogger log = null;
 *
 *     // Inject grid instance (or any other resource).
 *     &#64;GridInstanceResource
 *     private Grid grid = null;
 *     ...
 * }
 * </pre>
 * where spring bean resource class can look like this:
 * <pre name="code" class="java">
 * public class MyUserBean {
 *     ...
 * }
 * </pre>
 * and Spring file
 * <pre name="code" class="xml">
 * &lt;bean id="bean-name" class="my.foo.MyUserBean" singleton="true"&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Apache20LicenseCompatible
public @interface GridSpringResource {
    /**
     * Resource bean name in provided <tt>ApplicationContext</tt> to look up
     * a Spring bean.
     */
    String resourceName();
}
