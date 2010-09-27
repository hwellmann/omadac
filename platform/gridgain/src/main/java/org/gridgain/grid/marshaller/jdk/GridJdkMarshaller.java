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

package org.gridgain.grid.marshaller.jdk;

import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.*;
import java.io.*;

/**
 * Implementation of {@link GridMarshaller} based on JDK serialization mechanism.
 * <p>
 * Note, <tt>GridJdkMarshaller</tt> marshal and unmarshal objects except objects
 * with types defined in {@link #EXCLUDED_GRID_CLASSES}
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This marshaller has no mandatory configuration parameters.
 * <h2 class="header">Java Example</h2>
 * GridJdkMarshaller needs to be explicitly configured to override default JBoss marshaller.
 * <pre name="code" class="java">
 * GridJdkMarshaller marshaller = new GridJdkMarshaller();
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default marshaller.
 * cfg.setMarshaller(marshaller);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridJdkMarshaller can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *     ...
 *     &lt;property name="marshaller"&gt;
 *         &lt;bean class="org.gridgain.grid.marshaller.jdk.GridJdkMarshaller"/&gt;
 *     &lt;/property&gt;
 *     ...
 * &lt;/bean&gt;
 * </pre>
 *  <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 * <h2 class="header">Injection Example</h2>
 * GridJBossMarshaller can be injected in users task, job or SPI as following:
 * <pre name="code" class="java">
 * public class MyGridJob implements GridJob {
 *     ...
 *     &#64;GridMarshallerResource
 *     private GridMarshaller marshaller = null;
 *     ...
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
 * <br>
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJdkMarshaller implements GridMarshaller {
    /**
     * {@inheritDoc}
     */
    public void marshal(Object obj, OutputStream out) throws GridException {
        assert out != null : "ASSERTION [line=99, file=src/java/org/gridgain/grid/marshaller/jdk/GridJdkMarshaller.java]";

        ObjectOutputStream objOut = null;

        try {
            objOut = new GridJdkMarshallerObjectOutputStream(new GridJdkMarshallerOutputStreamWrapper(out));

            // Make sure that we serialize only task, without class loader.
            objOut.writeObject(obj);

            objOut.flush();
        }
        catch (IOException e) {
            throw (GridException)new GridException("Failed to serialize object: " + obj, e).setData(112, "src/java/org/gridgain/grid/marshaller/jdk/GridJdkMarshaller.java");
        }
        finally{
            GridUtils.close(objOut, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public <T> T unmarshal(InputStream in, ClassLoader clsLoader) throws GridException {
        assert in != null : "ASSERTION [line=124, file=src/java/org/gridgain/grid/marshaller/jdk/GridJdkMarshaller.java]";

        if (clsLoader == null) {
            clsLoader = getClass().getClassLoader();
        }

        ObjectInputStream objIn = null;

        try {
            objIn = new GridJdkMarshallerObjectInputStream(new GridJdkMarshallerInputStreamWrapper(in), clsLoader);

            return (T)objIn.readObject();
        }
        catch (ClassNotFoundException e) {
            throw (GridException)new GridException("Failed to deserialize object with given class loader: " + clsLoader, e).setData(138, "src/java/org/gridgain/grid/marshaller/jdk/GridJdkMarshaller.java");
        }
        catch (IOException e) {
            throw (GridException)new GridException("Failed to deserialize object with given class loader: " + clsLoader, e).setData(141, "src/java/org/gridgain/grid/marshaller/jdk/GridJdkMarshaller.java");
        }
        finally{
            GridUtils.close(objIn, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJdkMarshaller.class, this);
    }
}
