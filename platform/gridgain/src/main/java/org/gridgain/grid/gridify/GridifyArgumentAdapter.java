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

package org.gridgain.grid.gridify;

import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Convenience adapter for {@link GridifyArgument} interface. This adapter
 * should be used in custom grid job implementations.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * <tt>gridified</tt> methods.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see Gridify
 */
public class GridifyArgumentAdapter implements GridifyArgument {
    /** Method class. */
    private Class<?> cls = null;

    /** Method name. */
    private String mtdName = null;

    /** Method parameter types. */
    private Class<?>[] types = null;

    /** Method parameters. */
    private Object[] params = null;

    /** Method execution state. */
    private Object target = null;

    /**
     * Empty constructor.
     */
    public GridifyArgumentAdapter() {
        // No-op.
    }

    /**
     * Copy constructor.
     *
     * @param orig Copy to create this instance from.
     * @param newParams Optional array of new parameters to override the the ondes from <tt>orig</tt>.
     */
    public GridifyArgumentAdapter(GridifyArgument orig, Object... newParams) {
        GridArgumentCheck.checkNull(orig, "orig");

        // Copy original's fields.
        cls = orig.getMethodClass();
        mtdName = orig.getMethodName();
        target = orig.getTarget();

        types = new Class[orig.getMethodParameterTypes().length];
        params = new Object[orig.getMethodParameters().length];

        System.arraycopy(orig.getMethodParameters(), 0, params, 0, params.length);
        System.arraycopy(orig.getMethodParameterTypes(), 0, types, 0, types.length);

        // Override parameters, if any.
        if (newParams.length > 0) {
            setMethodParameters(newParams);
        }
    }

    /**
     * Creates a fully initialized gridify argument.
     *
     * @param cls Method class.
     * @param name Method name.
     * @param types Method parameter types.
     * @param params Method parameters.
     * @param target Target object.
     */
    public GridifyArgumentAdapter(Class<?> cls, String name, Class<?>[] types, Object[] params, Object target) {
        this.cls = cls;
        this.mtdName = name;
        this.types = types;
        this.params = params;
        this.target = target;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getMethodClass() {
        return cls;
    }

    /**
     * {@inheritDoc}
     */
    public String getMethodName() {
        return mtdName;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?>[] getMethodParameterTypes() {
        return types;
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getMethodParameters() {
        return params;
    }

    /**
     * Sets method class.
     *
     * @param cls Method class.
     */
    public void setMethodClass(Class<?> cls) {
        this.cls = cls;
    }

    /**
     * Sets method name.
     *
     * @param mtdName Method name.
     */
    public void setMethodName(String mtdName) {
        this.mtdName = mtdName;
    }

    /**
     * Sets method parameter types.
     *
     * @param types Method parameter types.
     */
    public void setMethodParameterTypes(Class<?>... types) {
        this.types = types;
    }

    /**
     * Updates parameter type.
     *
     * @param type Parameter type to set.
     * @param index Index of the parameter.
     */
    public void updateMethodParameterType(Class<?> type, int index) {
        types[index] = type;
    }

    /**
     * Sets method parameters.
     *
     * @param params Method parameters.
     */
    public void setMethodParameters(Object... params) {
        this.params = params;
    }

    /**
     * Updates method parameter.
     *
     * @param param Method parameter value to set.
     * @param index Parameter's index.
     */
    public void updateMethodParameter(Object param, int index) {
        params[index] = param;
    }

    /**
     * Sets target object for method execution.
     *
     * @param target Target object for method execution.
     */
    public void setTarget(Object target) {
        this.target = target;
    }

    /**
     * Gets target object for method execution.
     *
     * @return Target object for method execution.
     */
    public Object getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridifyArgumentAdapter.class, this);
    }
}
