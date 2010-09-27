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

package org.gridgain.grid.test.junit4;

import java.util.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJunit4IgnoredClassRunner extends GridJunit4Runner {
    /** */
    private final Class<?> cls;

    /**
     *
     * @param testClass Test class.
     */
    GridJunit4IgnoredClassRunner(Class<?> testClass) {
        cls = testClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(RunNotifier notifier) {
        notifier.fireTestIgnored(getDescription());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Description getDescription() {
        return Description.createSuiteDescription(cls);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Class<?> getTestClass() {
        return cls;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void copyResults(GridJunit4Runner runner) {
        // No-op.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean setResult(GridJunit4Result res) {
        // No-op.
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    boolean setResult(List<GridJunit4Result> res) {
        for (GridJunit4Result result : res) {
            if (setResult(result) == false) {
                return false;
            }
        }

        return true;
    }    
}
