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

package org.gridgain.grid.marshaller;

import java.io.*;
import java.util.concurrent.*;
import org.gridgain.apache.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.springframework.context.*;
import javax.management.*;

/**
 * <tt>GridMarshaller</tt> allows to marshal or unmarshal objects in grid. It provides
 * serialization/deserialization mechanism for all instances that are sent across networks
 * or are otherwise serialized.
 * <p>
 * Gridgain provides the following <tt>GridMarshaller</tt> implementations:
 * <ul>
 * <li>{@link org.gridgain.grid.marshaller.jboss.GridJBossMarshaller} - default</li>
 * <li>{@link org.gridgain.grid.marshaller.jdk.GridJdkMarshaller}</li>
 * <li>{@link org.gridgain.grid.marshaller.xstream.GridXstreamMarshaller}</li>
 * </ul>
 * <p>
 * Below are examples of marshaller configuration, usage, and injection into tasks, jobs,
 * and SPI's.
 * <h2 class="header">Java Example</h2>
 * <tt>GridMarshaller</tt> can be explicitely configured in code.
 * <pre name="code" class="java">
 * GridJbossMarshaller marshaller = new GridJbossMarshaller();
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override marshaller.
 * cfg.setMarshaller(marshaller);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridMarshaller can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *     ...
 *     &lt;property name="marshaller"&gt;
 *         &lt;bean class="org.gridgain.grid.marshaller.jboss.GridJBossMarshaller"/&gt;
 *     &lt;/property&gt;
 *     ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 * <h2 class="header">Injection Example</h2>
 * GridMarshaller can be injected in users task, job or SPI as following:
 * <pre name="code" class="java">
 * public class MyGridJob implements GridJob {
 *     ...
 *     &#64;GridMarshallerResource
 *     private GridMarshaller marshaller = null;
 *
 *     public Serializable execute() {
 *         // Use marshaller to serialize/deserialize any object.
 *         ...
 *     }
 * }
 * </pre>
 * or
 * <pre name="code" class="java">
 * public class MyGridJob implements GridJob {
 *     ...
 *     private GridMarshaller marshaller = null;
 *     ...
 *     &#64;GridMarshallerResource
 *     public void setMarshaller(GridMarshaller marshaller) {
 *         this.marshaller = marshaller;
 *     }
 *     ...
 * }
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridMarshaller {
    /**
     * Excluded grid classes from serialization. All marshallers must omit
     * this classes. Fields of these typs should be serialized as <tt>null</tt>.
     */
    public static final Class<?>[] EXCLUDED_GRID_CLASSES = new Class[] {
        GridLogger.class,
        GridTaskSession.class,
        Grid.class,
        MBeanServer.class,
        ExecutorService.class,
        ApplicationContext.class,
        GridLoadBalancer.class,
        GridJobContext.class,
        GridMarshaller.class
    };

    /**
     * Marshals object to the output stream. This method should not close
     * given output stream.
     *
     * @param obj Object to marshal.
     * @param out Output stream to marshal into.
     * @throws GridException If marshalling failed.
     */
    public void marshal(Object obj, OutputStream out) throws GridException;

    /**
     * Unmarshals object from the output stream using given class loader.
     * This method should not close given input stream.
     *
     * @param <T> Type of unmarshalled object.
     * @param in Input stream.
     * @param clsLoader Class loader to use.
     * @return Unmarshalled object.
     * @throws GridException If unmarshalling failed.
     */
    public <T> T unmarshal(InputStream in, ClassLoader clsLoader) throws GridException;
}
