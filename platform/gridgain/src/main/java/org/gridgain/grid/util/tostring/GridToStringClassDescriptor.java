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

import java.util.*;
import org.gridgain.apache.*;

/**
 * FIXDOC: provide class description here.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 */
@Apache20LicenseCompatible
class GridToStringClassDescriptor {
    /** */
    private final String sqn;
    
    /** */
    private final String fqn;
    
    /** */
    private List<GridToStringFieldDescriptor> fields = new ArrayList<GridToStringFieldDescriptor>();
    
    /**
     * @param cls FIXDOC
     */
    GridToStringClassDescriptor(Class<?> cls) {
        assert cls != null : "ASSERTION [line=47, file=src/java/org/gridgain/grid/util/tostring/GridToStringClassDescriptor.java]";
        
        fqn = cls.getName();
        sqn = cls.getSimpleName();
    }
    
    /**
     * 
     * @param field FIXDOC
     */
    void addField(GridToStringFieldDescriptor field) {
        assert field != null : "ASSERTION [line=58, file=src/java/org/gridgain/grid/util/tostring/GridToStringClassDescriptor.java]";
        
        fields.add(field);
    }
    
    /**
     * 
     */
    void sortFields() {
        Collections.sort(fields, new Comparator<GridToStringFieldDescriptor>() {
            /**
             * {@inheritDoc}
             */
            public int compare(GridToStringFieldDescriptor arg0, GridToStringFieldDescriptor arg1) {
                return arg0.getOrder() < arg1.getOrder() ? -1 : arg0.getOrder() > arg1.getOrder() ? 1 : 0;
            }
        });
    }

    /**
     * @return FIXDOC
     */
    String getSimpleClassName() {
        return sqn;
    }
    
    /**
     * @return FIXDOC
     */
    String getFullyQualifiedClassName() {
        return fqn;
    }

    /**
     * @return FIXDOC
     */
    List<GridToStringFieldDescriptor> getFields() {
        return fields;
    }    
}
