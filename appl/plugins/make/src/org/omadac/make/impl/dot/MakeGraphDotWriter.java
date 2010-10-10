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
package org.omadac.make.impl.dot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.omadac.make.Target;
import org.omadac.make.impl.MakeGraph;

import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;

/**
 * Outputs the make graph in DOT syntax to a given file.
 * @author hwellmann
 *
 */
public class MakeGraphDotWriter
{
    private MakeGraph makeGraph;    
    private String dotOutput;
    private VertexNameProvider<Target> vnp;
    
    /**
     * Creates a writer for a given graph and a given file.
     * @param graph     make graph
     * @param dotOutput DOT output file
     */
    public MakeGraphDotWriter(MakeGraph graph, String dotOutput)
    {
        this.makeGraph = graph;
        this.dotOutput = dotOutput;
        vnp = new VertexNameProvider<Target>()
        {
            @Override
            public String getVertexName(Target target)
            {
                return String.format("\"%s\"", target);
            }
        };
    }
    
    /**
     * Writes the graph to the file.
     */
    public void writeDotFile()
    {
        PrintWriter writer;
        try
        {
            writer = new PrintWriter(new File(dotOutput));
            writer.println("strict digraph G {");
            writer.println("rankdir=BT");
            writer.println("node [fontname=Helvetica, fontsize=10, style=filled, shape=box, " +
                "width=0.4, height=0.3, fillcolor=\"yellow\"]");
            for (Target target : makeGraph.vertexSet())
            {
                writer.println(vnp.getVertexName(target));
            }
            for (DefaultEdge edge : makeGraph.edgeSet())
            {
                String source = vnp.getVertexName(makeGraph.getEdgeSource(edge));
                String target = vnp.getVertexName(makeGraph.getEdgeTarget(edge));
                writer.println(String.format("%s -> %s", source, target));
            }
            writer.println("}");
            writer.close();
        }
        catch (FileNotFoundException exc)
        {
            throw new IllegalArgumentException(exc);
        }
    }
}
