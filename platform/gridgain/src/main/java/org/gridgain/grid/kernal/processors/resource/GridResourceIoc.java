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

package org.gridgain.grid.kernal.processors.resource;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.util.*;

/**
 * Resource container contains caches for classes used for injection.
 * Caches used to improve the efficiency of standard Java reflection mechanism.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridResourceIoc {
    /** Number of entries to keep in annotation cache. */
    private static final int CLASS_CACHE_SIZE = 2000;

    /** Task class resource mapping. */
    private final Map<ClassLoader, Set<Class<?>>> taskMap = new HashMap<ClassLoader, Set<Class<?>>>();

    /** Field cache. */
    private final GridBoundedLinkedHashMap<Class<?>, Map<Class<? extends Annotation>,
        List<GridResourceField>>> fieldCache = new GridBoundedLinkedHashMap<Class<?>, Map<Class<? extends Annotation>,
            List<GridResourceField>>>(CLASS_CACHE_SIZE);

    /** Method cache. */
    private final GridBoundedLinkedHashMap<Class<?>, Map<Class<? extends Annotation>,
        List<GridResourceMethod>>> mtdCache = new GridBoundedLinkedHashMap<Class<?>, Map<Class<? extends Annotation>,
            List<GridResourceMethod>>>(CLASS_CACHE_SIZE);

    /** */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * @param ldr Class loader.
     */
    void onUndeployed(ClassLoader ldr) {
        lock.writeLock().lock();

        try {
            Set<Class<?>> clss = taskMap.remove(ldr);

            if (clss != null) {
                fieldCache.keySet().removeAll(clss);
                mtdCache.keySet().removeAll(clss);
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all internal caches.
     */
    void undeployAll() {
        lock.writeLock().lock();

        try {
            taskMap.clear();
            mtdCache.clear();
            fieldCache.clear();
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Injects given resource via field or setter with specified annotations
     * on provided target object.
     *
     * @param target Target object.
     * @param annCls Setter annotation.
     * @param injector Resource to inject.
     * @param depCls Deployment class.
     * @throws GridException Thrown in case of any errors during injection.
     */
    @SuppressWarnings("unchecked")
    void injectResource(Object target, Class<? extends Annotation> annCls, GridResourceInjector injector,
        GridDeploymentClass depCls) throws GridException {

        for (Class<?> cls = target.getClass(); cls.equals(Object.class) == false; cls = cls.getSuperclass()) {
            for (GridResourceField field : getFieldsWithAnnotation(depCls, cls, annCls)) {
                Field f = field.getField();

                // Special handling for anonymous classes.
                if (f.getName().startsWith("this$") == true) {
                    f.setAccessible(true);

                    try {
                        // Recursion.
                        injectResource(f.get(target), annCls, injector, depCls);
                    }
                    catch (IllegalAccessException e) {
                        throw (GridException)new GridException("Failed to inject resource [field=" + f.getName() +
                            ", target=" + target + ']', e).setData(121, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceIoc.java");
                    }
                }
                else {
                    injector.inject(field, target, depCls);
                }
            }

            for (GridResourceMethod mtd : getMethodsWithAnnotation(depCls, cls, annCls)) {
                injector.inject(mtd, target, depCls);
            }
        }
    }

    /**
     * For tests only.
     *
     * @param cls Class for test.
     * @return FIXDOC
     */
    boolean isCached(Class<?> cls) {
        return isCached(cls.getName());
    }

    /**
     * For tests only.
     *
     * @param clsName Class for test.
     * @return FIXDOC
     */
    boolean isCached(String clsName) {
        lock.readLock().lock();

        try {
            for (Class<?> aClass : fieldCache.keySet()) {
                if (aClass.getName().equals(clsName)) {
                    return true;
                }
            }

            for (Class<?> aClass : mtdCache.keySet()) {
                if (aClass.getName().equals(clsName)) {
                    return true;
                }
            }

            return false;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets set of methods with given annotation.
     *
     * @param depCls Deployed class.
     * @param rsrcCls Class in which search for methods.
     * @param annCls Annotation.
     * @return Set of methods with given annotations.
     */
    private List<GridResourceMethod> getMethodsWithAnnotation(GridDeploymentClass depCls, Class<?> rsrcCls,
        Class<? extends Annotation> annCls) {
        List<GridResourceMethod> mtds = getMethodsFromCache(rsrcCls, annCls);

        if (mtds == null) {
            mtds = new ArrayList<GridResourceMethod>();

            for (Method mtd : rsrcCls.getDeclaredMethods()) {
                Annotation ann = mtd.getAnnotation(annCls);

                if (ann != null) {
                    mtds.add(new GridResourceMethod(mtd, ann));
                }
            }

            cacheMethods(depCls, rsrcCls, annCls, mtds);
        }

        return mtds;
    }

    /**
     * Gets all entries from the specified class or its super-classes that have
     * been annotated with annotation provided.
     *
     * @param rsrcCls Class in which search for methods.
     * @param depCls Deployed class.
     * @param annCls Annotation.
     * @return Set of entries with given annotations.
     */
    private List<GridResourceField> getFieldsWithAnnotation(GridDeploymentClass depCls, Class<?> rsrcCls,
        Class<? extends Annotation> annCls) {
        List<GridResourceField> fields = getFieldsFromCache(rsrcCls, annCls);

        if (fields == null) {
            fields = new ArrayList<GridResourceField>();

            for (Field field : rsrcCls.getDeclaredFields()) {
                // Account for anonymous inner classes.
                Annotation ann = field.getAnnotation(annCls);

                if (ann != null || field.getName().startsWith("this$") == true) {
                    fields.add(new GridResourceField(field, ann));
                }
            }

            cacheFields(depCls, rsrcCls, annCls, fields);
        }

        return fields;
    }

    /**
     * Gets all fields for a given class with given annotation from cache.
     *
     * @param cls Class to get fields from.
     * @param annCls Annotation class for fields.
     * @return List of fields with given annotation, possibly <tt>null</tt>.
     */
    private List<GridResourceField> getFieldsFromCache(Class<?> cls, Class<? extends Annotation> annCls) {
        lock.readLock().lock();

        try {
            Map<Class<? extends Annotation>, List<GridResourceField>> annCache = fieldCache.get(cls);

            if (annCache != null) {
                return annCache.get(annCls);
            }
        }
        finally {
            lock.readLock().unlock();
        }

        return null;
    }

    /**
     * Caches list of fields with given annotation from given class.
     *
     * @param rsrcCls Class the fields belong to.
     * @param depCls Deployed class.
     * @param annCls Annotation class for the fields.
     * @param fields Fields to cache.
     */
    private void cacheFields(GridDeploymentClass depCls, Class<?> rsrcCls, Class<? extends Annotation> annCls,
        List<GridResourceField> fields) {
        lock.writeLock().lock();

        try {
            if (depCls != null) {
                Set<Class<?>> clss = taskMap.get(depCls.getClassLoader());

                if (clss == null) {
                    taskMap.put(depCls.getClassLoader(), clss = new HashSet<Class<?>>());
                }

                clss.add(rsrcCls);
            }

            Map<Class<? extends Annotation>, List<GridResourceField>> annCache = fieldCache.get(rsrcCls);

            if (annCache == null) {
                fieldCache.put(rsrcCls, annCache = new HashMap<Class<? extends Annotation>, List<GridResourceField>>());
            }

            annCache.put(annCls, fields);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets all methods for a given class with given annotation from cache.
     *
     * @param cls Class to get methods from.
     * @param annCls Annotation class for fields.
     * @return List of methods with given annotation, possibly <tt>null</tt>.
     */
    private List<GridResourceMethod> getMethodsFromCache(Class<?> cls, Class<? extends Annotation> annCls) {
        lock.readLock().lock();

        try {
            Map<Class<? extends Annotation>, List<GridResourceMethod>> annCache = mtdCache.get(cls);

            if (annCache != null) {
                return annCache.get(annCls);
            }
        }
        finally {
            lock.readLock().unlock();
        }

        return null;
    }

    /**
     * Caches list of methods with given annotation from given class.
     *
     * @param rsrcCls Class the fields belong to.
     * @param depCls Deployed class.
     * @param annCls Annotation class for the fields.
     * @param mtds Methods to cache.
     */
    private void cacheMethods(GridDeploymentClass depCls, Class<?> rsrcCls, Class<? extends Annotation> annCls,
        List<GridResourceMethod> mtds) {
        lock.writeLock().lock();

        try {
            if (depCls != null) {
                Set<Class<?>> clss = taskMap.get(depCls.getClassLoader());

                if (clss == null) {
                    taskMap.put(depCls.getClassLoader(), clss = new HashSet<Class<?>>());
                }

                clss.add(rsrcCls);
            }

            Map<Class<? extends Annotation>, List<GridResourceMethod>> annCache = mtdCache.get(rsrcCls);

            if (annCache == null) {
                mtdCache.put(rsrcCls, annCache = new HashMap<Class<? extends Annotation>, List<GridResourceMethod>>());
            }

            annCache.put(annCls, mtds);
        }
        finally {
            lock.writeLock().unlock();
        }
    }
}
