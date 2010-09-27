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

package org.gridgain.grid;

import java.io.*;
import java.util.*;
import org.gridgain.grid.util.*;

/**
 * Convenience adapter for {@link GridJob} implementations. It provides the
 * following functionality:
 * <ul>
 * <li>
 *      Default implementation of {@link GridJob#cancel()} method and ability
 *      to check whether cancellation occurred.
 * </li>
 * <li>
 *      Ability to set and get a job argument via {@link #setArgument(Serializable)}
 *      and {@link #getArgument()} methods.
 * </li>
 * <li>
 *      Ability to add multiple arguments in order via {@link #addArgument(Serializable)}
 *      and {@link #getArgument()} methods.
 * </li>
 * </ul>
 * Here is an example of how <tt>GridJobAdapter</tt> can be used from task logic
 * to create jobs. The example creates job adapter as anonymous class, but you
 * are free to create a separate class for it. Note that all state passed within
 * <tt>GridJobAdapter</tt> must be {@link Serializable}.
 * <pre name="code" class="java">
 * public class TestGridTask extends GridTaskSplitAdapter&lt;String, Integer&gt; {
 *     // Used to imitate some logic for the
 *     // sake of this example
 *     private int multiplier = 3;
 *
 *     &#64;Override
 *     protected Collection&lt;? extends GridJob&gt; split(int gridSize, final String arg) throws GridException {
 *         List&lt;GridJobAdapter&lt;String&gt;&gt; jobs = new ArrayList&lt;GridJobAdapter&lt;String&gt;&gt;(gridSize);
 *
 *         for (int i = 0; i < gridSize; i++) {
 *             jobs.add(new GridJobAdapter&lt;String&gt;() {
 *                 // Job execution logic.
 *                 public Serializable execute() throws GridException {
 *                     return multiplier * arg.length();
 *                 }
 *             });
 *        }
 *
 *         return jobs;
 *     }
 *
 *     // Aggregate multiple job results into
 *     // one task result.
 *     public Integer reduce(List&lt;GridJobResult&gt; results) throws GridException {
 *         int sum = 0;
 *
 *         // For the sake of this example, let's sum all results.
 *         for (GridJobResult res : results) {
 *             sum += (Integer)res.getData();
 *         }
 *
 *         return sum;
 *     }
 * }
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <G> Type of the optional job argument.
 */
public abstract class GridJobAdapter<G extends Serializable> implements GridJob {
    /** Job argument. */
    private List<G> args = null;

    /** Cancellation flag. */
    private volatile boolean cancelled = false;

    /**
     * No-arg constructor.
     * <p>
     * <b>Note:</b> the job argument will be <tt>null</tt> which usually <i>is not</i> the intended behavior.
     * You can use {@link #setArgument(Serializable)} to  set job argument.
     */
    protected GridJobAdapter() {
        args = new ArrayList<G>(1);
    }

    /**
     * Creates job with specified arguments.
     *
     * @param args Job arguments.
     */
    protected GridJobAdapter(G... args) {
        if (args == null) {
            this.args = new ArrayList<G>(1);
        }
        else {
            this.args = new ArrayList<G>(args.length);

            this.args.addAll(Arrays.asList(args));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     * This method tests whether or not this job was cancelled. This method
     * is thread-safe and can be called without extra synchronization.
     * <p>
     * This method can be periodically called in {@link GridJob#execute()} method
     * implementation to check whether or not this job cancelled. Note that system
     * calls {@link #cancel()} method only as a hint and this is a responsibility of
     * the implementation of the job to properly cancel its execution.
     *
     * @return <tt>true</tt> if this job was cancelled, <tt>false</tt> otherwise.
     */
    protected final boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets an optional job argument at position <tt>0</tt>.
     *
     * @param arg Executable argument.
     */
    public void setArgument(G arg) {
        if (args.isEmpty() == true) {
            args.add(arg);
        }
        else {
            args.set(0, arg);
        }
    }

    /**
     * Adds an optional job argument.
     *
     * @param arg Job argument.
     */
    public void addArgument(G arg) {
        args.add(arg);
    }

    /**
     * Gets job argument at position 0 or <tt>null</tt> if no argument was previously set.
     *
     * @return Job argument.
     */
    public G getArgument() {
        return args.isEmpty() == true ? null : args.get(0);
    }

    /**
     * Gets argument at specified position.
     *
     * @param pos Position of the argument.
     * @return Argument at specified position.
     */
    public G getArgument(int pos) {
        GridArgumentCheck.checkRange(pos < args.size(), "position < args.size()");

        return args.get(pos);
    }

    /**
     * Gets ordered list of all job arguments set so far. Note that
     * the same list as used internally is returned, so modifications
     * to it will affect the state of this job adapter instance.
     *
     * @return Ordered list of all job arguments.
     */
    public List<G> getAllArguments() {
        return args;
    }
}
