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

package org.gridgain.grid.util.tostring;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.*;
import org.gridgain.apache.*;

/**
 * Provides auto-generation framework for <tt>toString()</tt> output.
 * <p>
 * Default exclusion policy (can be overridden with {@link GridToStringInclude}
 * annotation):
 * <ul>
 * <li>fields with {@link GridToStringExclude} annotations
 * <li>classes that have {@link GridToStringExclude} annotation (current list):
 *      <ul>
 *      <li>GridManager
 *      <li>GridManagerRegistry
 *      <li>GridProcessor
 *      <li>GridProcessorRegistry
 *      <li>GridLogger
 *      <li>GridDiscoveryMetricsProvider
 *      <li>GridByteArrayList
 *      <li>GridFifoQueue
 *      </ul>
 * <li>static fields
 * <li>non-private fields
 * <li>arrays
 * <li>fields of type {@link Object}
 * <li>fields of type {@link Thread}
 * <li>fields of type {@link Runnable}
 * <li>fields of type {@link Serializable}
 * <li>fields of type {@link Externalizable}
 * <li>{@link InputStream} implementations
 * <li>{@link OutputStream} implementations
 * <li>{@link EventListener} implementations
 * <li>{@link Lock} implementations
 * <li>{@link ReadWriteLock} implementations
 * <li>{@link Condition} implementations
 * <li>{@link Map} implementations
 * <li>{@link Collection} implementations
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 */
@Apache20LicenseCompatible
public final class GridToStringBuilder {
    /** */
    private static final Map<String, GridToStringClassDescriptor> classCache = new HashMap<String,
        GridToStringClassDescriptor>();

    /** */
    private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /** */
    private static ThreadLocal<List<GridToStringThreadLocal>> threadCache =
        new ThreadLocal<List<GridToStringThreadLocal>>() {
        /**
         * {@inheritDoc}
         */
        @Override
        protected synchronized List<GridToStringThreadLocal> initialValue() {
            List<GridToStringThreadLocal> list = new ArrayList<GridToStringThreadLocal>(1);

            list.add(new GridToStringThreadLocal());

            return list;
        }
    };

    /**
     * Enforces singleton.
     */
    private GridToStringBuilder() {
        // No-op.
    }

    /**
     * Produces auto-generated output of string presentation for given object and its declaration class.
     *
     * @param <T> Type of the object.
     * @param cls Declaration class of the object. Note that this should not be a runtime class.
     * @param obj Object to get a string presentation for.
     * @param name0 Additional parameter name.
     * @param val0 Additional parameter value.
     * @param name1 Additional parameter name.
     * @param val1 Additional parameter value.
     * @param name2 Additional parameter name.
     * @param val2 Additional parameter value.
     * @param name3 Additional parameter name.
     * @param val3 Additional parameter value.
     * @return String presentation of the given object.
     */
    public static <T> String toString(Class<T> cls, T obj, String name0, Object val0, String name1, Object val1,
        String name2, Object val2, String name3, Object val3) {
        assert cls != null : "ASSERTION [line=118, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert obj != null : "ASSERTION [line=119, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name0 != null : "ASSERTION [line=120, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name1 != null : "ASSERTION [line=121, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name2 != null : "ASSERTION [line=122, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name3 != null : "ASSERTION [line=123, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        List<GridToStringThreadLocal> list = threadCache.get();

        assert list != null : "ASSERTION [line=127, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        GridToStringThreadLocal tmp = null;

        // Since toString() methods can be chain-called from the same thread we
        // have to keep a list of thread-local objects and remove/add them
        // in each toString() call.
        if (list.isEmpty() == true) {
            tmp = new GridToStringThreadLocal();
        }
        else {
            tmp = list.remove(0);
        }

        Object[] addNames = tmp.getAdditionalNames();
        Object[] addVals = tmp.getAdditionalValues();

        addNames[0] = name0;
        addVals[0] = val0;
        addNames[1] = name1;
        addVals[1] = val1;
        addNames[2] = name2;
        addVals[2] = val2;
        addNames[3] = name3;
        addVals[3] = val3;

        try {
            return toStringImpl(cls, tmp.getStringBuilder(), obj, addNames, addVals, 4);
        }
        finally {
            list.add(tmp);
        }
    }

    /**
     * Produces auto-generated output of string presentation for given object and its declaration class.
     *
     * @param <T> Type of the object.
     * @param cls Declaration class of the object. Note that this should not be a runtime class.
     * @param obj Object to get a string presentation for.
     * @param name0 Additional parameter name.
     * @param val0 Additional parameter value.
     * @param name1 Additional parameter name.
     * @param val1 Additional parameter value.
     * @param name2 Additional parameter name.
     * @param val2 Additional parameter value.
     * @return String presentation of the given object.
     */
    public static <T> String toString(Class<T> cls, T obj, String name0, Object val0, String name1, Object val1,
        String name2, Object val2) {
        assert cls != null : "ASSERTION [line=177, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert obj != null : "ASSERTION [line=178, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name0 != null : "ASSERTION [line=179, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name1 != null : "ASSERTION [line=180, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name2 != null : "ASSERTION [line=181, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        List<GridToStringThreadLocal> list = threadCache.get();

        assert list != null : "ASSERTION [line=185, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        GridToStringThreadLocal tmp = null;

        // Since toString() methods can be chain-called from the same thread we
        // have to keep a list of thread-local objects and remove/add them
        // in each toString() call.
        if (list.isEmpty() == true) {
            tmp = new GridToStringThreadLocal();
        }
        else {
            tmp = list.remove(0);
        }

        Object[] addNames = tmp.getAdditionalNames();
        Object[] addVals = tmp.getAdditionalValues();

        addNames[0] = name0;
        addVals[0] = val0;
        addNames[1] = name1;
        addVals[1] = val1;
        addNames[2] = name2;
        addVals[2] = val2;

        try {
            return toStringImpl(cls, tmp.getStringBuilder(), obj, addNames, addVals, 3);
        }
        finally {
            list.add(tmp);
        }
    }

    /**
     * Produces auto-generated output of string presentation for given object and its declaration class.
     *
     * @param <T> Type of the object.
     * @param cls Declaration class of the object. Note that this should not be a runtime class.
     * @param obj Object to get a string presentation for.
     * @param name0 Additional parameter name.
     * @param val0 Additional parameter value.
     * @param name1 Additional parameter name.
     * @param val1 Additional parameter value.
     * @return String presentation of the given object.
     */
    public static <T> String toString(Class<T> cls, T obj, String name0, Object val0, String name1, Object val1) {
        assert cls != null : "ASSERTION [line=230, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert obj != null : "ASSERTION [line=231, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name0 != null : "ASSERTION [line=232, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name1 != null : "ASSERTION [line=233, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        List<GridToStringThreadLocal> list = threadCache.get();

        assert list != null : "ASSERTION [line=237, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        GridToStringThreadLocal tmp = null;

        // Since toString() methods can be chain-called from the same thread we
        // have to keep a list of thread-local objects and remove/add them
        // in each toString() call.
        if (list.isEmpty() == true) {
            tmp = new GridToStringThreadLocal();
        }
        else {
            tmp = list.remove(0);
        }

        Object[] addNames = tmp.getAdditionalNames();
        Object[] addVals = tmp.getAdditionalValues();

        addNames[0] = name0;
        addVals[0] = val0;
        addNames[1] = name1;
        addVals[1] = val1;

        try {
            return toStringImpl(cls, tmp.getStringBuilder(), obj, addNames, addVals, 2);
        }
        finally {
            list.add(tmp);
        }
    }

    /**
     * Produces auto-generated output of string presentation for given object and its declaration class.
     *
     * @param <T> Type of the object.
     * @param cls Declaration class of the object. Note that this should not be a runtime class.
     * @param obj Object to get a string presentation for.
     * @param name Additional parameter name.
     * @param val Additional parameter value.
     * @return String presentation of the given object.
     */
    public static <T> String toString(Class<T> cls, T obj, String name, Object val) {
        assert cls != null : "ASSERTION [line=278, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert obj != null : "ASSERTION [line=279, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert name != null : "ASSERTION [line=280, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        List<GridToStringThreadLocal> list = threadCache.get();

        assert list != null : "ASSERTION [line=284, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        GridToStringThreadLocal tmp = null;

        // Since toString() methods can be chain-called from the same thread we
        // have to keep a list of thread-local objects and remove/add them
        // in each toString() call.
        if (list.isEmpty() == true) {
            tmp = new GridToStringThreadLocal();
        }
        else {
            tmp = list.remove(0);
        }

        Object[] addNames = tmp.getAdditionalNames();
        Object[] addVals = tmp.getAdditionalValues();

        addNames[0] = name;
        addVals[0] = val;

        try {
            return toStringImpl(cls, tmp.getStringBuilder(), obj, addNames, addVals, 1);
        }
        finally {
            list.add(tmp);
        }
    }

    /**
     * Produces auto-generated output of string presentation for given object and its declaration class.
     *
     * @param <T> Type of the object.
     * @param cls Declaration class of the object. Note that this should not be a runtime class.
     * @param obj Object to get a string presentation for.
     * @return String presentation of the given object.
     */
    public static <T> String toString(Class<T> cls, T obj) {
        assert cls != null : "ASSERTION [line=321, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert obj != null : "ASSERTION [line=322, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        List<GridToStringThreadLocal> list = threadCache.get();

        assert list != null : "ASSERTION [line=326, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        GridToStringThreadLocal tmp = null;

        // Since toString() methods can be chain-called from the same thread we
        // have to keep a list of thread-local objects and remove/add them
        // in each toString() call.
        if (list.isEmpty() == true) {
            tmp = new GridToStringThreadLocal();
        }
        else {
            tmp = list.remove(0);
        }

        try {
            return toStringImpl(cls, tmp.getStringBuilder(), obj, tmp.getAdditionalNames(),
                tmp.getAdditionalValues(), 0);
        }
        finally {
            list.add(tmp);
        }
    }

    /**
     * Creates an uniformed string presentation for the given object.
     *
     * @param cls FIXDOC
     * @param buf FIXDOC
     * @param obj Object for which to get string presentation.
     * @param addNames FIXDOC
     * @param addVals FIXDOC
     * @param addLength FIXDOC
     * @return String presentation of the given object.
     * @param <T> Type of object.
     */
    @SuppressWarnings({"unchecked"})
    private static <T> String toStringImpl(Class<T> cls, StringBuilder buf, T obj, Object[] addNames, Object[] addVals,
        int addLength) {
        assert cls != null : "ASSERTION [line=364, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert buf != null : "ASSERTION [line=365, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert obj != null : "ASSERTION [line=366, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert addNames != null : "ASSERTION [line=367, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert addVals != null : "ASSERTION [line=368, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert addNames.length == addVals.length : "ASSERTION [line=369, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";
        assert addLength <= addNames.length : "ASSERTION [line=370, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        //noinspection CatchGenericClass
        try {
            GridToStringClassDescriptor cd = getClassDescriptor(cls);

            assert cd != null : "ASSERTION [line=376, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

            buf.setLength(0);

            buf.append(cd.getSimpleClassName()).append(" [");

            boolean first = true;

            for (GridToStringFieldDescriptor fd : cd.getFields()) {
                if (first == false) {
                   buf.append(", ");
                }
                else {
                    first = false;
                }

                final String name = fd.getName();

                Field field = cls.getDeclaredField(name);

                field.setAccessible(true);

                buf.append(name).append('=').append(field.get(obj));
            }

            if (addLength > 0) {
                for (int i = 0; i < addLength; i++) {
                    if (first == false) {
                       buf.append(", ");
                    }
                    else {
                        first = false;
                    }

                    buf.append(addNames[i]).append('=').append(addVals[i]);
                }
            }

            buf.append(']');

            return buf.toString();
        }
        // Specifically catching all exceptions.
        catch (Exception e) {
            final String key = cls.getName() + System.identityHashCode(cls.getClassLoader());

            rwLock.writeLock().lock();

            // Remove entry from cache to avoid potential memory leak
            // in case new class loader got loaded under the same identity hash.
            try {
                classCache.remove(key);
            }
            finally {
                rwLock.writeLock().unlock();
            }

            return "<undefined due to exception: " + e.getClass().getSimpleName() + '>';
        }
    }

    /**
     *
     * @param cls FIXDOC
     * @param <T> Type of the object.
     * @return FIXDOC
     */
    private static <T> GridToStringClassDescriptor getClassDescriptor(Class<T> cls) {
        assert cls != null : "ASSERTION [line=444, file=src/java/org/gridgain/grid/util/tostring/GridToStringBuilder.java]";

        final String key = cls.getName() + System.identityHashCode(cls.getClassLoader());

        GridToStringClassDescriptor cd = null;

        rwLock.readLock().lock();

        try {
            cd = classCache.get(key);
        }
        finally {
            rwLock.readLock().unlock();
        }

        if (cd == null) {
            cd = new GridToStringClassDescriptor(cls);

            for (Field f : cls.getDeclaredFields()) {
                boolean add = false;

                Class<?> type = f.getType();

                if (f.isAnnotationPresent(GridToStringInclude.class) == true ||
                    type.isAnnotationPresent(GridToStringInclude.class) == true) {
                    add = true;
                }
                else if (f.isAnnotationPresent(GridToStringExclude.class) == false &&
                    f.getType().isAnnotationPresent(GridToStringExclude.class) == false) {
                    //noinspection ObjectEquality
                    if (
                        // Include only private non-static
                        Modifier.isPrivate(f.getModifiers()) == true &&
                        Modifier.isStatic(f.getModifiers()) == false &&

                        // No direct objects & serializable.
                        Object.class != type &&
                        Serializable.class != type &&
                        Externalizable.class != type &&

                        // No arrays.
                        type.isArray() == false &&

                        // Exclude collections, IO, etc.
                        EventListener.class.isAssignableFrom(type) == false &&
                        Map.class.isAssignableFrom(type) == false &&
                        Collection.class.isAssignableFrom(type) == false &&
                        InputStream.class.isAssignableFrom(type) == false &&
                        OutputStream.class.isAssignableFrom(type) == false &&
                        Thread.class.isAssignableFrom(type) == false &&
                        Runnable.class.isAssignableFrom(type) == false &&
                        Lock.class.isAssignableFrom(type) == false &&
                        ReadWriteLock.class.isAssignableFrom(type) == false &&
                        Condition.class.isAssignableFrom(type) == false
                        ) {
                        add = true;
                    }
                }

                if (add == true) {
                    GridToStringFieldDescriptor fd = new GridToStringFieldDescriptor(f.getName());

                    // Get order, if any.
                    if (f.isAnnotationPresent(GridToStringOrder.class) == true) {
                        fd.setOrder(f.getAnnotation(GridToStringOrder.class).value());
                    }

                    cd.addField(fd);
                }
            }

            cd.sortFields();

            /*
             * Allow multiple puts for the same class - they will simply override.
             */

            rwLock.writeLock().lock();

            try {
                classCache.put(key, cd);
            }
            finally {
                rwLock.writeLock().unlock();
            }
        }

        return cd;
    }
}
