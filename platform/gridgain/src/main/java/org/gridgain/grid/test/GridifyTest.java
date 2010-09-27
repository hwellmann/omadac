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

package org.gridgain.grid.test;

import java.lang.annotation.*;
import org.junit.runners.*;

/**
 * Annotation for grid-enabling JUnit3 and Junit4 tests.
 * <h1 class="header">JUnit3</h1>
 * To enable JUnit3 tests using <tt>GridifyTest</tt> annotation, simply attach this
 * annotation to <tt>"static suite()"</tt> method for a test suite you would like to
 * grid-enable.
 * <pre name="code" class="java">
 * public class GridifyJunit3ExampleTestSuite {
 *     // Standard JUnit3 suite method. Note we attach &#64;GridifyTest
 *     // annotation to it, so it will be grid-enabled.
 *     &#64;GridifyTest
 *     public static TestSuite suite() {
 *         TestSuite suite = new TestSuite("Example Test Suite");
 * 
 *         // Nested test suite to run tests A and B sequentially.
 *         TestSuite nested = new TestSuite("Example Nested Sequential Suite");
 * 
 *         nested.addTestSuite(TestA.class);
 *         nested.addTestSuite(TestB.class);
 * 
 *         // Add tests A and B.
 *         suite.addTest(nested);
 * 
 *         // Add other tests.
 *         suite.addTestSuite(TestC.class);
 *         suite.addTestSuite(TestD.class);
 * 
 *         return suite;
 *     }
 * } 
 * </pre>
 * <h1 class="header">JUnit4</h1>
 * To enable JUnit4 tests using <tt>GridifyTest</tt> annotation, you need to attach
 * this annotation to the same class that has {@link Suite} annotation (only {@link Suite}
 * runners can be grid-enabled in JUnit4).
 * <pre name="code" class="java">
 *  &#64;RunWith(Suite.class)
 *  &#64;SuiteClasses({
 *      GridJunit4ExampleNestedSuite.class, // Nested suite that will execute tests A and B added to it sequentially.
 *      TestC.class, // Test C will run in parallel with other tests.
 *      TestD.class // TestD will run in parallel with other tests.
 *  })
 *  &#64;GridifyTest // Run this suite on the grid.
 *  public class GridifyJunit4ExampleSuite {
 *      // No-op.
 *  } 
 * </pre>
 * <pre name="code" class="java">
 * &#64;RunWith(Suite.class)
 * &#64;SuiteClasses({
 *     TestA.class,
 *     TestB.class
 * })
 * public class GridJunit4ExampleNestedSuite {
 *     // No-op.
 * } 
 * </pre>
 * <p>
 * <h1 class="header">Jboss AOP Configuration</h1>
 * The following configuration needs to be applied to enable JBoss byte code
 * weaving. Note that GridGain is not shipped with JBoss and necessary
 * libraries will have to be downloaded separately (they come standard
 * if you have JBoss installed already):
 * <ul>
 * <li>
 *      The following JVM configuration must be present:
 *      <ul>
 *      <li><tt>-javaagent:[path to jboss-aop-jdk50-4.x.x.jar]</tt></li>
 *      <li><tt>-Djboss.aop.class.path=[path to gridgain.jar]</tt></li>
 *      <li><tt>-Djboss.aop.exclude=org,com -Djboss.aop.include=org.gridgain.examples</tt></li>
 *      </ul>
 * </li>
 * <li>
 *      The following JARs should be in a classpath (all located under <tt>[GRIDGAIN_HOME]/libs</tt> folder):
 *      <ul>
 *      <li><tt>javassist-3.x.x.jar</tt></li>
 *      <li><tt>jboss-aop-jdk50-4.x.x.jar</tt></li>
 *      <li><tt>jboss-aspect-library-jdk50-4.x.x.jar</tt></li>
 *      <li><tt>jboss-common-4.x.x.jar</tt></li>
 *      <li><tt>trove-1.0.2.jar</tt></li>
 *      </ul>
 * </li>
 * </ul>
 * <p>
 * <h1 class="header">AspectJ AOP Configuration</h1>
 * The following configuration needs to be applied to enable AspectJ byte code
 * weaving.
 * <ul>
 * <li>
 *      JVM configuration should include:
 *      <tt>-javaagent:[GRIDGAIN_HOME]/libs/aspectjweaver-1.5.3.jar</tt>
 * </li>
 * <li>
 *      Classpath should contain the <tt>[GRIDGAIN_HOME]/config/aop/aspectj</tt> folder.
 * </li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GridifyTest {
    /**
     * Optional configuration path. Default is <tt>"config/junit/junit-spring.xml"</tt>.
     */
    String configPath() default "config/junit/junit-spring.xml";

    /**
     * Optional router class. Default is {@link GridTestRouterAdapter} class.
     */
    Class<? extends GridTestRouter> routerClass() default GridTestRouterAdapter.class;

    /**
     * Indicates whether grid is disabled or not.
     */
    boolean disabled() default false;

    /**
     * Distributed test suite timeout in milliseconds, default is <tt>0</tt> which means that
     * tests will never expire.
     */
    long timeout() default 0;
}
