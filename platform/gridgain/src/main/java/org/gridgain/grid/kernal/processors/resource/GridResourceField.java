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
import org.gridgain.grid.util.tostring.*;

/**
 * Wrapper for data where resource should be injected.
 * Bean contains {@link Field} and {@link Annotation} for that class field.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridResourceField {
    /** Field where resource should be injected. */
    private final Field field;

    /** Resource annotation. */
    private final Annotation ann;

    /**
     * Creates new bean.
     *
     * @param field Field where resource should be injected.
     * @param ann Resource annotation.
     */
    GridResourceField(Field field, Annotation ann) {
        assert field != null : "ASSERTION [line=49, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceField.java]";
        assert ann != null || field.getName().startsWith("this$") == true : "ASSERTION [line=50, file=src/java/org/gridgain/grid/kernal/processors/resource/GridResourceField.java]";

        this.field = field;
        this.ann = ann;
    }

    /**
     * Gets class field object.
     *
     * @return Class field.
     */
    public Field getField() {
        return field;
    }

    /**
     * Gets annotation for class field object.
     *
     * @return Field annotation.
     */
    public Annotation getAnnotation() {
        return ann;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridResourceField.class, this);
    }
}
