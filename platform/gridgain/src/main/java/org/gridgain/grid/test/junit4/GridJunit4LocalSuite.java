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

import org.junit.internal.builders.JUnit4Builder;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;


/**
 * Parallel local runner for the grid. Use this runner when your test
 * suite should only be run locally but in parallel with other local
 * or distributed tests. Having local tests execute in parallel with
 * distributed tests will still give a significant performance boost
 * in many cases.
 * <p>
 * To use local test suite within distributed test suite, simply add
 * it to distributed test suite as follows:
 * <pre name="code" class="java">
 * &#64;RunWith(GridJunit4Suite.class)
 * &#64;SuiteClasses({
 *     TestA.class,
 *     TestB.class,
 *     GridJunit4ExampleNestedLocalSuite.class, // Local suite that will execute its test C locally.
 * })
 * public class GridJunit4ExampleSuite {
 *     // No-op.
 * } 
 * </pre>
 * <pre name="code" class="java">
 * &#64;RunWith(GridJunit4LocalSuite.class) // Specify local suite to run tests.
 * &#64;SuiteClasses({
 *     TestC.class,
 *     TestD.class
 * })
 * public class GridJunit4ExampleNestedLocalSuite {
 *     // No-op.
 * } 
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJunit4LocalSuite extends Suite {
    /**
     * Constructor required by JUnit4.
     *
     * @param cls Suite class.
     * @throws InitializationError If error occurred during initialization.
     */
    public GridJunit4LocalSuite(Class<?> cls) throws InitializationError {
        super(cls, new JUnit4Builder());
    }
}
