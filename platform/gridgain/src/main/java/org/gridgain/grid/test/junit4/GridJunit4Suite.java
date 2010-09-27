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


import static org.gridgain.grid.test.GridTestVmParameters.GRID_CONFIG;
import static org.gridgain.grid.test.GridTestVmParameters.GRID_DISABLED;
import static org.gridgain.grid.test.GridTestVmParameters.GRID_NAME;
import static org.gridgain.grid.test.GridTestVmParameters.GRID_TEST_ROUTER;
import static org.gridgain.grid.test.GridTestVmParameters.GRID_TEST_TIMEOUT;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.gridgain.grid.Grid;
import org.gridgain.grid.GridException;
import org.gridgain.grid.GridFactory;
import org.gridgain.grid.GridFactoryState;
import org.gridgain.grid.GridRuntimeException;
import org.gridgain.grid.GridTaskFuture;
import org.gridgain.grid.GridTaskListener;
import org.gridgain.grid.test.GridTestRouter;
import org.gridgain.grid.test.GridTestRouterAdapter;
import org.gridgain.grid.test.GridTestVmParameters;
import org.gridgain.grid.test.GridifyTest;
import org.gridgain.grid.util.GridUtils;
import org.junit.internal.builders.JUnit4Builder;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

/**
 * Test suite runner for distributing JUnit4 tests. Simply add tests to this suite runner
 * just like you would for regular JUnit4 suites, and these tests will be executed in parallel
 * on the grid. Note that if there are no other grid nodes, this suite runner will still
 * ensure parallel test execution within single VM.
 * <p>
 * Below is an example of distributed JUnit4 test suite:
 * <pre name="code" class="java">
 * &#64;RunWith(GridJunit4Suite.class)
 * &#64;SuiteClasses({
 *     TestA.class, // TestA will run in parallel on the grid.
 *     TestB.class, // TestB will run in parallel on the grid.
 *     TestC.class, // TestC will run in parallel on the grid.
 *     TestD.class // TestD will run in parallel on the grid.
 * })
 * public class GridJunit4ExampleSuite {
 *     // No-op.
 * }
 * </pre>
 * If you have four tests A, B, C, and D, and if you need to run A and B sequentially, then you
 * should create a nested test suite with test A and B as follows:
 * <pre name="code" class="java">
 * &#64;RunWith(GridJunit4Suite.class)
 * &#64;SuiteClasses({
 *     GridJunit4ExampleNestedSuite.class, // Nested suite that will execute tests A and B added to it sequentially.
 *     TestC.class, // TestC will run in parallel on the grid.
 *     TestD.class // TestD will run in parallel on the grid.
 * })
 * public class GridJunit4ExampleSuite {
 *     // No-op.
 * }
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
 * Note that you can also grid-enable existing JUnit4 tests using {@link GridifyTest @GridifyTest}
 * annotation which you can attach to the same class you attach {@link RunWith} annotation to.
 * Refer to {@link GridifyTest @GridifyTest} documentation for more information.
 * <p>
 * Also note that some tests can only be executed locally mostly due to some environment issues. However
 * they still can benefit from parallel execution with other tests. GridGain supports it via
 * {@link GridJunit4LocalSuite} suites that can be added as nested suites to <tt>GridJunit4Suite</tt>. Refer
 * to {@link GridJunit4LocalSuite} documentation for more information.
 * <h1 class="header">Logging</h1>
 * When running distributed JUnit, all the logging that is done to {@link System#out} or {@link System#err}
 * is preserved. GridGain will accumulate all logging that is done on remote nodes, send them back
 * to originating node and associate all log statements with their corresponding tests. This way,
 * for example, if you are running tests from IDEA or Eclipse (or any other IDE) you would still
 * see the logs as if it was a local run. However, since remote nodes keep all log statements done within
 * a single individual test case in memory, you must make sure that enough memory is allocated
 * on every node and that individual test cases do not spit out gigabytes of log statements.
 * Also note, that logs will be sent back to originating node upon completion of every test,
 * so don't be alarmed if you don't see any log statements for a while and then all of them
 * appear at once.
 * <p>
 * GridGain achieves such log transparency via reassigning {@link System#out} or {@link System#err} to
 * internal {@link PrintStream} implementation. However, when using <tt>Log4J</tt>
 * within your tests you must make sure that it is configured with {@link ConsoleAppender} and that
 * {@link ConsoleAppender#setFollow(boolean)} attribute is set to <tt>true</tt>. Logging to files
 * is not supported yet and is planned for next point release.
 * <p>
 * <h1 class="header">Test Suite Nesting</h1>
 * <tt>GridJunit4TestSuite</tt> instances can be nested within each other as deep as needed.
 * However all nested distributed test suites will be treated just like regular JUnit test suites.
 * This approach becomes convenient when you have several distributed test suites that you
 * would like to be able to execute separately in distributed fashion, but at the same time
 * you would like to be able to execute them as a part of larger distributed suites.
 * <p>
 * <h1 class="header">Configuration</h1>
 * To run distributed JUnit tests you need to start other instances of GridGain. You can do
 * so by running <tt>GRIDGAIN_HOME/bin/gridgain-junit.{sh|bat}</tt> script, which will
 * start default configuration. If configuration other than default is required, then
 * use regular <tt>GRIDGAIN_HOME/bin/gridgain.{sh|bat}</tt> script and pass your own
 * Spring XML configuration file as a parameter to the script.
 * <p>
 * You can use the following configuration parameters to configure distributed test suite
 * locally. These parameters are set via {@link GridifyTest} annotation. Note that GridGain
 * will check these parameters even if AOP is not enabled. Also note that many parameters
 * can be overridden by setting corresponding VM parameters defined in {@link GridTestVmParameters}
 * at VM startup.
 * <table class="doctable">
 *   <tr>
 *     <th>GridConfiguration Method</th>
 *     <th>Default Value</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>{@link GridifyTest#disabled()}</td>
 *     <td><tt>false</tt></td>
 *     <td>
 *       If <tt>true</tt> then GridGain will be turned off and suite will run locally.
 *       This value can be overridden by setting {@link GridTestVmParameters#GRID_DISABLED} VM
 *       parameter to <tt>true</tt>. This parameter comes handy when you would like to
 *       turn off GridGain without changing the actual code.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@link GridifyTest#configPath()}</td>
 *     <td>{@link #DFLT_JUNIT_CONFIG DFLT_JUNIT_CONFIG}</td>
 *     <td>
 *       Optional path to GridGain Spring XML configuration file for running JUnit tests. This
 *       property can be overridden by setting {@link GridTestVmParameters#GRID_CONFIG} VM
 *       parameter. Note that the value can be either absolute value or relative to
 *       [GRIDGAIN_HOME] installation folder.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@link GridifyTest#routerClass()}</td>
 *     <td><tt>{@link GridTestRouterAdapter}</tt></td>
 *     <td>
 *       Optional test router class that implements {@link GridTestRouter} interface.
 *       If not provided, then tests will be routed in round-robin fashion using default
 *       {@link GridTestRouterAdapter}. The value of this parameter can be overridden by setting
 *       {@link GridTestVmParameters#GRID_TEST_ROUTER} VM parameter to the name of your
 *       own customer router class.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@link GridifyTest#timeout()}</td>
 *     <td><tt>0</tt> which means that tests will never timeout.</td>
 *     <td>
 *       Maximum timeout value in milliseconds after which test suite will return without
 *       waiting for the remaining tests to complete. This value can be overridden by setting
 *       {@link GridTestVmParameters#GRID_TEST_TIMEOUT} VM parameter to the timeout value
 *       for the tests.
 *     </td>
 *   </tr>
 * </table>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJunit4Suite extends GridJunit4SuiteRunner {
    /** Default GridGain configuration file for JUnits (value is <tt>config/junit/junit-spring.xml</tt>). */
    public static final String DFLT_JUNIT_CONFIG = "config/junit/junit-spring.xml";

    /** Default JUnit test router (value is {@link GridTestRouterAdapter GridTestRouterAdapter.class.getName()}). */
    public static final String DFLT_JUNIT_ROUTER = GridTestRouterAdapter.class.getName();

    /** Flag indicating whether grid was started in this suite. */
    private boolean selfStarted = false;

    /** Path to Spring XML configuration file. */
    private String cfgPath = System.getProperty(GRID_CONFIG.name()) == null ? DFLT_JUNIT_CONFIG :
        System.getProperty(GRID_CONFIG.name());

    /** Check if GridGain is disabled by checking {@link GridTestVmParameters#GRID_DISABLED} system property. */
    @SuppressWarnings({"UseOfArchaicSystemPropertyAccessors"})
    private boolean isDisabled = Boolean.getBoolean(GRID_DISABLED.name());

    /** JUnit router class name. */
    private String routerClsName = System.getProperty(GRID_TEST_ROUTER.name()) == null ? DFLT_JUNIT_ROUTER :
        System.getProperty(GRID_TEST_ROUTER.name());

    /** JUnit router class. */
    private Class<? extends GridTestRouter> routerCls = null;

    /** JUnit grid name. */
    private String gridName = null;

    /** JUnit test suite timeout. */
    @SuppressWarnings({"UseOfArchaicSystemPropertyAccessors"})
    private long timeout = Long.getLong(GRID_TEST_TIMEOUT.name()) == null ? 0 :
        Long.getLong(GRID_TEST_TIMEOUT.name());

    /** */
    private final ClassLoader clsLdr;

    /**
     * Creates distributed suite runner for given class.
     *
     * @param cls Class to create suite runner for.
     */
    public GridJunit4Suite(Class<?> cls) {
        super(cls);

        clsLdr = GridUtils.detectClassLoader(cls);
    }

    /**
     * Creates distributed suite runner for given class.
     *
     * @param cls Class to create suite runner for.
     * @param clsLdr Tests class loader.
     */
    public GridJunit4Suite(Class<?> cls, ClassLoader clsLdr) {
        super(cls);

        assert clsLdr != null : "ASSERTION [line=234, file=src/java/org/gridgain/grid/test/junit4/GridJunit4Suite.java]";

        this.clsLdr = clsLdr;
    }

    /**
     * Sets path to GridGain configuration file. By default
     * <tt>{GRIDGAIN_HOME}/config/junit/junit-spring.xml</tt> is used.
     *
     * @param cfgPath Path to GridGain configuration file.
     */
    public void setConfigurationPath(String cfgPath) {
        this.cfgPath = cfgPath;
    }

    /**
     * Gets path to GridGain configuration file. By default
     * <tt>{GRIDGAIN_HOME}/config/junit/junit-spring.xml</tt> is used.
     *
     * @return Path to GridGain configuration file.
     */
    public String getConfigurationPath() {
        return cfgPath;
    }

    /**
     * Disables GridGain. If set to <tt>true</tt> then this suite will execute locally
     * as if GridGain was not in a picture at all.
     *
     * @param disabled If set to <tt>true</tt> then this suite will execute locally
     *      as if GridGain was not in a picture at all.
     */
    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

    /**
     * Gets flag indicating whether GridGain should be enabled or not. If set to
     * <tt>true</tt> then this suite will execute locally as if GridGain was not
     * in a picture at all.
     *
     * @return Flag indicating whether GridGain should be enabled or not. If set to
     *      <tt>true</tt> then this suite will execute locally as if GridGain was not
     *      in a picture at all.
     */
    public boolean isDisabled() {
        return isDisabled;
    }

    /**
     * Sets name of class for routing JUnit tests. By default {@link #DFLT_JUNIT_ROUTER}
     * class name is used.
     *
     * @param routerClsName Junit test router class name.
     */
    public void setRouterClassName(String routerClsName) {
        this.routerClsName = routerClsName;
    }

    /**
     * Gets JUnit test router class name.
     *
     * @return JUnit test router class name.
     */
    public String getRouterClassName() {
        return routerClsName;
    }

    /**
     * Sets router class. By default {@link GridTestRouterAdapter} is used.
     *
     * @param routerCls Router class to use for test routing.
     */
    public void setRouterClass(Class<? extends GridTestRouter> routerCls) {
        this.routerCls = routerCls;
    }

    /**
     * Gets router class used for test routing.
     *
     * @return Router class used for test routing.
     */
    public Class<? extends GridTestRouter> getRouterClass() {
        return routerCls;
    }

    /**
     * Creates JUnit test router. Note that router must have a no-arg constructor.
     *
     * @return JUnit router instance.
     */
    @SuppressWarnings({"unchecked"})
    private GridTestRouter createRouter() {
        GridifyTest ann = getTestClass().getAnnotation(GridifyTest.class);

        if (ann != null) {
            Properties props = System.getProperties();

            // System property is given priority.
            if (props.containsKey(GRID_TEST_ROUTER.name()) == false) {
                routerCls = ann.routerClass();
            }
        }

        try {
            if (routerCls == null) {
                assert routerClsName != null : "ASSERTION [line=340, file=src/java/org/gridgain/grid/test/junit4/GridJunit4Suite.java]. " + "Neither outer class or router class name is specified.";

                routerCls = (Class<? extends GridTestRouter>)Class.forName(routerClsName);
            }
            else {
                routerClsName = routerCls.getName();
            }

            return routerCls.newInstance();
        }
        catch (ClassNotFoundException e) {
            throw (GridRuntimeException)new GridRuntimeException("Failed to initialize JUnit router: " + routerClsName, e).setData(351, "src/java/org/gridgain/grid/test/junit4/GridJunit4Suite.java");
        }
        catch (IllegalAccessException e) {
            throw (GridRuntimeException)new GridRuntimeException("Failed to initialize JUnit router: " + routerClsName, e).setData(354, "src/java/org/gridgain/grid/test/junit4/GridJunit4Suite.java");
        }
        catch (InstantiationException e) {
            throw (GridRuntimeException)new GridRuntimeException("Failed to initialize JUnit router: " + routerClsName, e).setData(357, "src/java/org/gridgain/grid/test/junit4/GridJunit4Suite.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final RunNotifier notifier) {
        GridifyTest ann = getTestClass().getAnnotation(GridifyTest.class);

        if (ann != null) {
            Properties props = System.getProperties();

            // System property is given priority.
            if (props.containsKey(GRID_DISABLED.name()) == false) {
                isDisabled = ann.disabled();
            }

            if (props.containsKey(GRID_TEST_TIMEOUT.name()) == false) {
                timeout = ann.timeout();
            }
        }

        if (isDisabled == false) {
            GridTestRouter router = createRouter();

            Grid grid = startGrid();

            try {
                // Start execution on Grid.
                for (final GridJunit4Runner child : getChildren()) {
                    grid.execute(
                        new GridJunit4Task(child.getTestClass(), clsLdr),
                        new GridJunit4Argument(router, child, isLocal(child)),
                        timeout,
                        new GridTaskListener() {
                            /**
                             * {@inheritDoc}
                             */
                            public void onFinished(GridTaskFuture<?> taskFuture) {
                                // Wait for results.
                                try {
                                    GridJunit4Runner res = (GridJunit4Runner)taskFuture.get();

                                    // Copy results to local test.
                                    child.copyResults(res);
                                }
                                catch (GridException e) {
                                    handleFail(child, e);
                                }
                            }
                        }
                    );
                }

                // Collect results and finish tests sequentially.
                for (GridJunit4Runner child : getChildren()) {
                    // Start test.
                    child.run(notifier);
                }
            }
            finally {
                stopGrid();
            }
        }
        else {
            try {
                new Suite(getTestClass(), new JUnit4Builder()).run(notifier);
            }
            catch (InitializationError e) {
                throw (GridRuntimeException)new GridRuntimeException("Failed to initialize suite: " + getTestClass(), e).setData(428, "src/java/org/gridgain/grid/test/junit4/GridJunit4Suite.java");
            }
        }
    }


    /**
     * Handles test fail.
     *
     * @param child Test.
     * @param e Exception that occurred during test.
     */
    private void handleFail(GridJunit4Runner child, GridException e) {
        // Since we got exception executing task, we cannot say which test failed.
        // So we mark all tests as failed.
        List<GridJunit4Result> failRes = new ArrayList<GridJunit4Result>();

        for (Description desc : child.getDescription().getChildren()) {
            failRes.add(new GridJunit4Result(desc.getDisplayName(), null, null, e));
        }

        child.setResult(failRes);
    }

    /**
     *
     * @return FIXDOC
     */
    private Grid startGrid() {
        Properties props = System.getProperties();

        gridName = props.getProperty(GRID_NAME.name());

        if (props.containsKey(GRID_NAME.name()) == false ||
            GridFactory.getState(gridName) != GridFactoryState.STARTED) {
            GridifyTest ann = getTestClass().getAnnotation(GridifyTest.class);

            if (ann != null) {
                // System property is given priority.
                if (props.containsKey(GRID_CONFIG.name()) == false) {
                    cfgPath = ann.configPath();
                }
            }

            selfStarted = true;

            // Set class loader for the spring.
            ClassLoader curClsLdr = Thread.currentThread().getContextClassLoader();

            // Add no-op logger to remove no-appender warning.
            Appender app = new NullAppender();

            Logger.getRootLogger().addAppender(app);

            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                Grid grid = GridFactory.start(cfgPath);

                gridName = grid.getName();

                System.setProperty(GRID_NAME.name(), grid.getName());

                return grid;
            }
            catch (GridException e) {
                throw (GridRuntimeException)new GridRuntimeException("Failed to start grid: " + gridName, e).setData(494, "src/java/org/gridgain/grid/test/junit4/GridJunit4Suite.java");
            }
            finally {
                Logger.getRootLogger().removeAppender(app);

                Thread.currentThread().setContextClassLoader(curClsLdr);
            }
        }

        return GridFactory.getGrid(gridName);
    }

    /**
     *
     */
    private void stopGrid() {
        // Only stop grid if it was started here.
        if (selfStarted == true) {
            GridFactory.stop(gridName, true);
        }
    }
}
