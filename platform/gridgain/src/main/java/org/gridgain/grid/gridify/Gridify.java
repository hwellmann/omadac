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

import java.io.*;
import java.lang.annotation.*;
import org.gridgain.grid.*;
import org.gridgain.grid.gridify.aop.spring.*;
import org.gridgain.grid.kernal.*;

/**
 * <img id="callout_img" src="{@docRoot}/img/callout_blue.gif"><span id="callout_blue">Start Here</span>&nbsp;
 * <tt>Gridify</tt> annotation is the main way to grid-enable existing code.
 * <p>
 * This annotation can be applied to any public method that needs to be grid-enabled,
 * static or non-static. When this annotation is applied to a method, the method
 * execution is considered to be grid-enabled. In general, the execution of this
 * method can be transfered to another node in the grid, potentially splitting it
 * into multiple subtasks to be executed in parallel on multiple grid nodes. But
 * from the caller perspective this method still looks and behaves like a local call.
 * This is achieved by utilizing AOP-based interception of the annotated code and
 * injecting all the necessary grid logic into the method execution.
 * <p>
 * By default, when neither {@link #taskClass()} or {@link #taskName()} are specified
 * a method with <tt>@Gridify</tt> annotation will be executed on randomly picked remote node.
 * <p>
 * Note that when using <tt>@Gridify</tt> annotation with default task (without
 * specifying explicit grid task), the state of the whole instance will be
 * serialized and sent out to remote node. Therefore the class must implement
 * {@link Serializable} interface. If you cannot make the class <tt>Serializable</tt>,
 * then you must implement custom grid task which will take care of proper state
 * initialization (see
 * <a href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/HelloWorld+-+Gridify+With+State">HelloWorld - Gridify With State</a>
 * example). In either case, GridGain must be able to serialize the state passed to remote node.
 * <p>
 * Refer to {@link GridTask} documentation for more information on how a task
 * can be split into multiple sub-jobs.
 * <p>
 * <h1 class="header">Java Example</h1>
 * Here is a simple example how to grid-enable a Java method. The method <tt>sayIt</tt>
 * with <tt>@Gridify</tt> annotation will be executed on remote node.
 * <pre name="code" class="java">
 * &#64;Gridify
 * public static void sayIt(String arg) {
 *    // Simply print out the argument.
 *    System.out.println(arg);
 * }
 * </pre>
 * Here is an example of how to grid-enable a Java method with custom task. The custom task
 * logic will provide a way to split and aggregate the result. The method <tt>sayIt</tt> will
 * be executed on remote node.
 * <pre name="code" class="java">
 * public class GridifyHelloWorldTaskExample {
 *     ...
 *     &#64;Gridify(taskClass = GridifyHelloWorldTask.class, timeout = 3000)
 *     public static integer sayIt(String arg) {
 *         // Simply print out the argument.
 *         System.out.println(">>> Printing '" + arg + "' on this node from grid-enabled method.");
 *
 *         return arg.length();
 *     }
 *     ...
 * }
 * </pre>
 * The custom task will actually take the String passed into <tt>sayIt(String)</tt> method,
 * split it into words and execute every word on different remote node.
 * <pre name="code" class="java">
 * public class GridifyHelloWorldTask extends GridifyTaskSplitAdapter&lt;Integer&gt; {
 *    &#64;Override
 *    protected Collection&lt;? extends GridJob&gt; split(int gridSize, GridifyArgument arg) throws GridException {
 *        String[] words = ((String)arg.getMethodParameters()[0]).split(" ");
 *
 *        List&lt;GridJobAdapter&lt;String&gt;&gt; jobs = new ArrayList&lt;GridJobAdapter&lt;String&gt;&gt;(words.length);
 *
 *        for (String word : words) {
 *            // Every job gets its own word as an argument.
 *            jobs.add(new GridJobAdapter&lt;String&gt;(word) {
 *                public Serializable execute() throws GridException {
 *                    // Execute gridified method.
 *                    // Note that since we are calling this method from within the grid job
 *                    // AOP-based grid enabling will not cross-cut it and method will just
 *                    // execute normally.
 *                    return GridifyHelloWorldTaskExample.sayIt(getArgument());
 *                }
 *            });
 *        }
 *
 *        return jobs;
 *    }
 *
 *    public Integer reduce(List&lt;GridJobResult&gt; results) throws GridException {
 *       int totalCharCnt = 0;
 *
 *        for (GridJobResult res : results) {
 *            // Every job returned a number of letters
 *            // for the phrase it was responsible for.
 *            Integer charCnt = res.getData();
 *
 *            totalCharCnt += charCnt;
 *        }
 *
 *        // Account for spaces. For simplicity we assume one space between words.
 *        totalCharCnt += results.size() - 1;
 *
 *        // Total number of characters in the phrase
 *        // passed into task execution.
 *        return totalCharCnt;
 *    }
 * }
 * </pre>
 * <p>
 * <h1 class="header">Jboss AOP</h1>
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
 *      The following JARs should be in a classpath:
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
 * <h1 class="header">AspectJ AOP</h1>
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
 * <p>
 * <h1 class="header">Spring AOP</h1>
 * Spring AOP framework is based on dynamic proxy implementation and doesn't require
 * any specific runtime parameters for online weaving. All weaving is on-demand and should
 * be performed by calling method {@link GridifySpringEnhancer#enhance(Object)} for the object
 * that has method with {@link Gridify} annotation.
 * <p>
 * Note that this method of weaving is rather inconvenient and AspectJ or JBoss AOP is
 * recommended over it. Spring AOP can be used in situation when code augmentation is
 * undesired and cannot be used. It also allows for very fine grained control of what gets
 * weaved.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Gridify {
    /**
     * Optional gridify task name. Note that either this name or {@link #taskClass()} must
     * be specified - but not both. If neither one is specified tasks' fully qualified name
     * will be used as a default name.
     */
    String taskName() default "";

    /**
     * Optional gridify task class. Note that either this name or {@link #taskName()} must
     * be specified - but not both. If neither one is specified tasks' fully qualified name
     * will be used as a default name.
     */
    Class<? extends GridTask<GridifyArgument, ?>> taskClass() default GridifyDefaultTask.class;

    /**
     * Optional gridify task execution timeout. Default is <tt>0</tt>
     * which indicates that task will not timeout.
     */
    long timeout() default 0;

    /**
     * Optional interceptor class.
     */
    Class<? extends GridifyInterceptor> interceptor() default GridifyInterceptor.class;

    /**
     * Name of the grid to use. By default, no-name default grid is used.
     * Refer to {@link GridFactory} for information about named grids.
     */
    String gridName() default "";
}
