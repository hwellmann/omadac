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

import java.io.*;
import java.lang.reflect.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.tostring.*;
import org.springframework.context.*;

/**
 * Spring bean injector implementation works with resources provided
 * by Spring <tt>ApplicationContext</tt>.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridResourceSpringBeanInjector implements GridResourceInjector {
    /** */
    private ApplicationContext springCtx = null;

    /**
     * Creates injector object.
     *
     * @param springCtx Spring context.
     */
    public GridResourceSpringBeanInjector(ApplicationContext springCtx) {
        this.springCtx = springCtx;
    }

    /**
     * {@inheritDoc}
     */
    public void inject(GridResourceField field, Object target,
        GridDeploymentClass depCls) throws GridException {
        GridSpringResource ann = (GridSpringResource)field.getAnnotation();

        assert ann != null : "ASSERTION [line=59, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceSpringBeanInjector.java]";

        // Note: injected non-serializable user resources should not mark
        // injected spring beans with transient modifier.

        // Check for 'transient' modifier only in serializable classes.
        if (Modifier.isTransient(field.getField().getModifiers()) == false &&
            Serializable.class.isAssignableFrom(field.getField().getDeclaringClass()) == true) {
            throw (GridException)new GridException("@GridSpringResource must only be used with 'transient' fields: " +
                field.getField()).setData(67, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceSpringBeanInjector.java");
        }

        String name = ann.resourceName();

        Object bean = null;

        if (springCtx != null) {
            bean = springCtx.getBean(name);

            GridResourceUtils.inject(field.getField(), target, bean);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void inject(GridResourceMethod mtd, Object target,
        GridDeploymentClass depCls) throws GridException {
        GridSpringResource ann = (GridSpringResource)mtd.getAnnotation();

        assert ann != null : "ASSERTION [line=89, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceSpringBeanInjector.java]";

        if (mtd.getMethod().getParameterTypes().length != 1) {
            throw (GridException)new GridException("Method injection setter must have only one parameter: " + mtd.getMethod()).setData(92, "src/java/org/gridgain/grid/kernal/processors/resource/GridResourceSpringBeanInjector.java");
        }

        String name = ann.resourceName();

        Object bean = null;

        if (springCtx != null) {
            bean = springCtx.getBean(name);

            GridResourceUtils.inject(mtd.getMethod(), target, bean);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(ClassLoader ldr) {
        // No-op. There is no cache.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridResourceSpringBeanInjector.class, this);
    }
}
