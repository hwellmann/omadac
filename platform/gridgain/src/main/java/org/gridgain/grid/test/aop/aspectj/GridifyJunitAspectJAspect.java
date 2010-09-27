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

package org.gridgain.grid.test.aop.aspectj;

import java.lang.reflect.*;
import java.util.*;
import junit.framework.*;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.*;
import static org.gridgain.grid.test.GridTestVmParameters.*;
import org.gridgain.grid.test.*;
import org.gridgain.grid.test.junit3.*;
import org.gridgain.grid.test.junit4.*;
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
@Aspect
public class GridifyJunitAspectJAspect {
    /**
     * Executes JUnit3 tests annotated with {@link GridifyTest @GridifyTest} annotation
     * on the grid.
     *
     * @param joinPoint Join point provided by AspectJ AOP.
     * @return Method execution result.
     * @throws Throwable If execution failed.
     */
    @Around("execution(static junit.framework.Test+ *.suite(..))")
    public Object gridifyJunit3(ProceedingJoinPoint joinPoint) throws Throwable {
        Method mtd = ((MethodSignature)joinPoint.getSignature()).getMethod();

        GridifyTest ann = mtd.getAnnotation(GridifyTest.class);

        if (ann == null) {
            return joinPoint.proceed();
        }

        Test test = (Test)joinPoint.proceed();

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
        ClassLoader clsLdr = joinPoint.getSignature().getDeclaringType().getClassLoader();
        
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
     * @param joinPoint Join point provided by AspectJ AOP.
     * @return Method execution result.
     * @throws Throwable If execution failed.
     */
    @Around("execution(public void (org.junit.runners.Suite).run(org.junit.runner.notification.RunNotifier))"
        + "&& !cflow(target(org.gridgain.grid.test.junit4.GridJunit4Suite))")
    public Object gridifyJunit4(ProceedingJoinPoint joinPoint) throws Throwable {
        Suite suite = (Suite)joinPoint.getTarget();

        // We create class with caller class loader, 
        // thus JUnit 4 task will pick up proper class loader.
        ClassLoader clsLoader = joinPoint.getSignature().getDeclaringType().getClassLoader();
        
        Class<?> cls = Class.forName(suite.getDescription().getDisplayName(), true, clsLoader);

        if (cls.getAnnotation(GridifyTest.class) != null) {
            new GridJunit4Suite(cls, clsLoader).run((RunNotifier)joinPoint.getArgs()[0]);

            return null;
        }

        return joinPoint.proceed();
    }
}
