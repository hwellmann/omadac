/*
 *    Omadac - The Open Map Database Compiler
 *    http://omadac.org
 * 
 *    (C) 2010, Harald Wellmann and Contributors
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.omadac.make;

import java.util.Collection;

/**
 * A Make Engine compiles a collection of targets taking care of their dependencies. Each
 * first-level target may have a number of prerequisites. The make engine ensures that all
 * prerequisites of a given target exist before updating the given target. It delegates the
 * actions for updating targets to a job manager. The actions are executed asynchronously, and
 * possibly in parallel.
 * <p>
 * A goal is an end result to be produced by the make enigne. The make engine stops running
 * when all goals are up-to-date. Goals are modelled as prerequisites of an implicit default
 * target. Thus, the make enigne terminates when the default target is up to date.
 * <p>
 * A forced target is required to be updated even when its prerequisites are up to date.
 * <p>
 * The dependency graph of the first-level targets can be output in DOT format, to be
 * visualized by the Graphviz toolset.
 * 
 * @author hwellmann
 *
 */
public interface MakeEngine extends ActionListener
{
    /** 
     * Sets the job manager to be used by the make engine.
     * @param jobManager job manager for executing target actions 
     */
    void setJobManager(JobManager jobManager);
    
    /**
     * Sets a file name for outputting the DOT dependency graph. If no file name is set, the
     * graph will not be generated
     * @param fileName   file name for dot graph
     */
    void setDotOutput(String fileName);
    
    /**
     * Adds a dependency between two targets
     * @param target        a target with a prerequisite
     * @param prerequisite  a prerequisite of the first target
     */
    void addDependency(Target target, Target prerequisite);
    
    /**
     * Adds a goal to the make engine. If no goal is set, the make engine will not create or
     * update any targets. The make engine terminates when all goals are up to date.
     * @param target
     */
    void addGoal(Target target);
    
    /**
     * Setting a target as forced will cause the target (and all its transitive dependents) to
     * be updated.
     * @param target
     */
    void addForcedTarget(Target target);
    
    /**
     * Starts the make engine. Preconditions:
     * <ul>
     * <li>A job manager is set.</li>
     * <li>All required goals and dependencies have been set.</li>
     * </ul>
     * Target dependencies may not be changed by the user after invoking this method.
     * Subtargets of complex targets are the only targets which will be added by the make
     * engine during the execution of this method.
     */
    void make();

    /**
     * Returns the prerequisites of the given target.
     * @param target
     * @return prerequisite targets
     */
    Collection<Target> getPrerequisites(Target target);

    /**
     * Returns the dependents of the given target.
     * @param target
     * @return dependent targets
     */
    Collection<Target> getDependents(Target target);

    /**
     * Returns all first-level targets.
     * @return first-level targets.
     */
    Collection<Target> getTargets();
    
    Step<? extends Target> findStep(Target target);
}
