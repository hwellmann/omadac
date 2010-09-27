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

package org.gridgain.grid.test.aop.jboss;

import java.lang.reflect.*;
import java.util.*;
import junit.framework.*;
import static org.gridgain.grid.test.GridTestVmParameters.*;
import org.gridgain.grid.test.*;
import org.gridgain.grid.test.junit3.*;
import org.gridgain.grid.test.junit4.*;
import org.jboss.aop.*;
import org.jboss.aop.advice.*;
import org.jboss.aop.joinpoint.*;
import org.jboss.aop.pointcut.*;
import org.junit.runner.notification.*;
import org.junit.runners.*;

/**
 * AspectJ aspect that intercepts on {@link GridifyTest} annotation to
 * execute annotated tests on remote nodes.
 * <p>
 * See {@link GridifyTest} documentation for more information about execution of
 * <tt>gridified</tt> JUnits.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridifyTest
 */
@SuppressWarnings({"ProhibitedExceptionDeclared"})
@Aspect(scope = Scope.PER_VM)
public class GridifyJunitJbossAspect {
    /** Definition of <tt>cflow</tt> pointcut. */
    @CFlowStackDef(cflows={@CFlowDef(expr= "* org.gridgain.grid.test.junit4.GridJunit4Suite->*(..)", called=false)})
    public static final CFlowStack JUNIT4_CFLOW_STACK = null;

    /**
     * Executes JUnit3 tests annotated with {@link GridifyTest @GridifyTest} annotation
     * on the grid.
     *
     * @param invoc Join point provided by JBoss AOP.
     * @return Method execution result.
     * @throws Throwable If execution failed.
     */
    @Bind(pointcut = "execution(* *->@org.gridgain.grid.test.GridifyTest(..)) AND " +
        "execution(public static $instanceof{junit.framework.Test} *->suite())")
    public Object gridifyJunit3(MethodInvocation invoc) throws Throwable {
        Method mtd = invoc.getMethod();

        GridifyTest ann = mtd.getAnnotation(GridifyTest.class);

        assert ann != null : "ASSERTION [line=71, file=src/java/org/gridgain/grid/test/aop/jboss/GridifyJunitJbossAspect.java]. " + "Intercepted method does not have gridify annotation.";

        Test test = (Test)invoc.invokeNext();

        TestSuite suite = null;

        if (test instanceof TestSuite == true) {
            suite = (TestSuite)test;
        }
        else {
            suite = new TestSuite();

            suite.addTest(test);
        }

        // Pickup class loader of caller code. This is considered as 
        // entire test suite class loader.
        ClassLoader clsLdr = invoc.getActualMethod().getDeclaringClass().getClassLoader();
        
        GridJunit3TestSuite gridSuite = new GridJunit3TestSuite(suite, clsLdr);

        Properties props = System.getProperties();

        // System property is given priority.
        if (props.containsKey(GRID_TEST_ROUTER.name()) == false) {
            gridSuite.setRouterClass(ann.routerClass());
        }

        // System property is given priority.
        if (props.containsKey(GRID_CONFIG.name()) == false) {
            gridSuite.setConfigurationPath(ann.configPath());
        }

        // System property is given priority.
        if (props.containsKey(GRID_DISABLED.name()) == false) {
            gridSuite.setDisabled(ann.disabled());
        }

        // System property is given priority.
        if (props.containsKey(GRID_TEST_TIMEOUT.name()) == false) {
            gridSuite.setTimeout(ann.timeout());
        }

        return gridSuite;
    }

    /**
     * Executes JUnit4 tests annotated with {@link GridifyTest @GridifyTest} annotation
     * on the grid.
     *
     * @param invoc Join point provided by JBoss AOP.
     * @return Method execution result.
     * @throws Throwable If execution failed.
     */
    @Bind(pointcut = "execution(public * org.junit.runners.Suite->run(org.junit.runner.notification.RunNotifier))",
        cflow = "org.gridgain.grid.test.aop.jboss.GridifyJunitJbossAspect.JUNIT4_CFLOW_STACK")
    public Object gridifyJunit4(MethodInvocation invoc) throws Throwable {
        Suite suite = (Suite)invoc.getTargetObject();

        // We create class with caller class loader, 
        // thus JUnit 4 task will pick up proper class loader.
        ClassLoader clsLoader = invoc.getActualMethod().getDeclaringClass().getClassLoader();
        
        Class<?> cls = Class.forName(suite.getDescription().getDisplayName(), true, clsLoader);

        if (cls.getAnnotation(GridifyTest.class) != null) {
            new GridJunit4Suite(cls, clsLoader).run((RunNotifier)invoc.getArguments()[0]);

            return null;
        }

        return invoc.invokeNext();
    }
}
