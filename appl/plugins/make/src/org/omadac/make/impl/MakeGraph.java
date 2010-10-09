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
package org.omadac.make.impl;

import org.omadac.make.Target;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Directed graph representation of targets and dependencies. The targets are the graph nodes.
 * The edges indicate the dependencies, pointing from a target to a prerequisite. The graph
 * must be acyclic. The root node is the default target.
 * 
 * @author hwellmann
 *
 */
public class MakeGraph extends DefaultDirectedGraph<Target, DefaultEdge>
{
    private static final long serialVersionUID = 1L;

    public MakeGraph()
    {
        super(DefaultEdge.class);
    }
}
