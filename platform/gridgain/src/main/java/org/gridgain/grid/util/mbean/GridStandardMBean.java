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

package org.gridgain.grid.util.mbean;

import java.lang.reflect.*;
import java.util.*;
import javax.management.*;
import org.gridgain.grid.util.*;

/**
 * Extension of standard Java MBean. Overrides some hooks to return
 * annotation based descriptions.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridStandardMBean extends StandardMBean {
    /**
     * Objects maps from primitive classes to primitive object classes.
     */
    private static final Map<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>(8);
    {
        primitiveClasses.put(Boolean.TYPE.toString().toLowerCase(), Boolean.TYPE);
        primitiveClasses.put(Character.TYPE.toString().toLowerCase(), Character.TYPE);
        primitiveClasses.put(Byte.TYPE.toString().toLowerCase(), Byte.TYPE);
        primitiveClasses.put(Short.TYPE.toString().toLowerCase(), Short.TYPE);
        primitiveClasses.put(Integer.TYPE.toString().toLowerCase(), Integer.TYPE);
        primitiveClasses.put(Long.TYPE.toString().toLowerCase(), Long.TYPE);
        primitiveClasses.put(Float.TYPE.toString().toLowerCase(), Float.TYPE);
        primitiveClasses.put(Double.TYPE.toString().toLowerCase(), Double.TYPE);
    }

    /**
     * Make a DynamicMBean out of the object implementation, using the specified
     * mbeanInterface class.
     *
     * @param implementation The implementation of this MBean.
     * @param mbeanInterface The Management Interface exported by this
     *      MBean's implementation. If <tt>null</tt>, then this
     *      object will use standard JMX design pattern to determine
     *      the management interface associated with the given
     *      implementation.
     *      If <tt>null</tt> value passed then information will be built by
     *      {@link StandardMBean}
     *
     * @exception NotCompliantMBeanException if the <tt>mbeanInterface</tt>
     *    does not follow JMX design patterns for Management Interfaces, or
     *    if the given <tt>implementation</tt> does not implement the
     *    specified interface.
     */
    public <T> GridStandardMBean(T implementation,Class<T> mbeanInterface)
        throws NotCompliantMBeanException {
        super(implementation, mbeanInterface);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        String str = super.getDescription(info);

        String methodName = (info.isIs() == true ? "is" : "get") + info.getName();

        //noinspection UnusedCatchParameter
        try {
            // Recursively get method.
            Method mtd = findMethod(getMBeanInterface(), methodName, new Class[]{});

            if (mtd != null) {
                GridMBeanDescription descr = mtd.getAnnotation(GridMBeanDescription.class);

                if (descr != null) {
                    str = descr.value();

                    assert str != null : "ASSERTION [line=95, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]. " + "Failed to find method: " + mtd;
                    assert str.trim().length() > 0 : "ASSERTION [line=96, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]. " + "Method description cannot be empty: " + mtd;

                    // Enforce proper English.
                    assert Character.isUpperCase(str.charAt(0)) == true :
                        "Description must start with upper case: " + str;

                    assert str.charAt(str.length() - 1) == '.' : "ASSERTION [line=102, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]. " + "Description must end with period: " + str;
                }
            }
        }
        catch (SecurityException e) {
            // No-op. Default value will be returned.
        }

        return str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDescription(MBeanInfo info) {
        String str = super.getDescription(info);

        // Return either default one or given by annotation.
        GridMBeanDescription descr = GridUtils.getAnnotation(getMBeanInterface(), GridMBeanDescription.class);

        if (descr != null) {
            str = descr.value();

            assert str != null : "ASSERTION [line=126, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";
            assert str.trim().length() > 0 : "ASSERTION [line=127, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";

            // Enforce proper English.
            assert Character.isUpperCase(str.charAt(0)) == true : "ASSERTION [line=130, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]. " + str;
            assert str.charAt(str.length() - 1) == '.' : "ASSERTION [line=131, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]. " + str;
        }

        return str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDescription(MBeanOperationInfo info) {
        String str = super.getDescription(info);

        //noinspection UnusedCatchParameter
        try {
            Method mthd = getMethod(info);

            GridMBeanDescription descr = mthd.getAnnotation(GridMBeanDescription.class);

            if (descr != null) {
                str = descr.value();

                assert str != null : "ASSERTION [line=153, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";
                assert str.trim().length() > 0 : "ASSERTION [line=154, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";

                // Enforce proper English.
                assert Character.isUpperCase(str.charAt(0)) == true : "ASSERTION [line=157, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]. " + str;
                assert str.charAt(str.length() - 1) == '.' : "ASSERTION [line=158, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]. " + str;
            }
        }
        catch (SecurityException e) {
            // No-op. Default value will be returned.
        }
        catch (ClassNotFoundException e) {
            // No-op. Default value will be returned.
        }

        return str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param, int seq) {
        String str = super.getDescription(op, param, seq);

        //noinspection UnusedCatchParameter
        try {
            Method mthd = getMethod(op);

            GridMBeanParametersDescriptions descrAnn = mthd.getAnnotation(GridMBeanParametersDescriptions.class);

            if (descrAnn != null) {
                assert descrAnn.value() != null : "ASSERTION [line=185, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";
                assert seq < descrAnn.value().length : "ASSERTION [line=186, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";

                str = descrAnn.value()[seq];

                assert str != null : "ASSERTION [line=190, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";
                assert str.trim().length() > 0 : "ASSERTION [line=191, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";

                // Enforce proper English.
                assert Character.isUpperCase(str.charAt(0)) == true : "ASSERTION [line=194, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]. " + str;
                assert str.charAt(str.length() - 1) == '.' : "ASSERTION [line=195, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]. " + str;
            }
        }
        catch (SecurityException e) {
            // No-op. Default value will be returned.
        }
        catch (ClassNotFoundException e) {
            // No-op. Default value will be returned.
        }

        return str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int seq) {
        String str = super.getParameterName(op, param, seq);

        //noinspection UnusedCatchParameter
        try {
            Method mthd = getMethod(op);

            GridMBeanParametersNames namesAnn = mthd.getAnnotation(GridMBeanParametersNames.class);

            if (namesAnn != null) {
                assert namesAnn.value() != null : "ASSERTION [line=222, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";
                assert seq < namesAnn.value().length : "ASSERTION [line=223, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";

                str = namesAnn.value()[seq];

                assert str != null : "ASSERTION [line=227, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";
                assert str.trim().length() > 0 : "ASSERTION [line=228, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";
            }
        }
        catch (SecurityException e) {
            // No-op. Default value will be returned.
        }
        catch (ClassNotFoundException e) {
            // No-op. Default value will be returned.
        }

        return str;
    }

    /**
     * Gets method by operation info.
     *
     * @param op MBean operation info.
     * @return Method.
     * @throws ClassNotFoundException Thrown if parameter type is unknown.
     * @throws SecurityException Thrown if method access is not allowed.
     */
    private Method getMethod(MBeanOperationInfo op) throws ClassNotFoundException, SecurityException {
        String methodName = op.getName();

        MBeanParameterInfo[] signature = op.getSignature();

        Class<?>[] params = new Class<?>[signature.length];

        for (int i = 0; i < signature.length; i++) {
            // Parameter type is either a primitive type or class. Try both.
            Class<?> type = primitiveClasses.get(signature[i].getType().toLowerCase());

            if (type == null) {
                type = Class.forName(signature[i].getType());
            }

            params[i] = type;
        }

        return findMethod(getMBeanInterface(), methodName, params);
    }

    /**
     * Finds method for the given interface.
     *
     * @param itf MBean interface.
     * @param methodName Method name.
     * @param params Method parameters.
     * @return Method.
     */
    @SuppressWarnings("unchecked")
    private Method findMethod(Class itf, String methodName, Class[] params) {
        assert itf.isInterface() == true : "ASSERTION [line=280, file=src/java/org/gridgain/grid/util/mbean/GridStandardMBean.java]";

        Method res = null;

        // Try to get method from given interface.
        //noinspection UnusedCatchParameter
        try {
            res = itf.getDeclaredMethod(methodName, params);

            if (res != null) {
                return res;
            }
        }
        catch (NoSuchMethodException e) {
            // No-op. Default value will be returned.
        }

        // Process recursively super interfaces.
        Class[] superItfs = itf.getInterfaces();

        for (Class superItf: superItfs) {
            res = findMethod(superItf, methodName, params);

            if (res != null) {
                return res;
            }
        }

        return res;
    }
}
