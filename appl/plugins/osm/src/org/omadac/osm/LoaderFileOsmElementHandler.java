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
package org.omadac.osm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import org.omadac.config.OmadacException;
import org.omadac.loader.LoaderFileWriter;
import org.omadac.osm.jaxb.Bounds;
import org.omadac.osm.jaxb.Node;
import org.omadac.osm.jaxb.NodeRef;
import org.omadac.osm.jaxb.Relation;
import org.omadac.osm.jaxb.RelationMember;
import org.omadac.osm.jaxb.Tag;
import org.omadac.osm.jaxb.Way;

public class LoaderFileOsmElementHandler extends AbstractOsmElementHandler
{
    private LoaderFileWriter nodesWriter;
    private LoaderFileWriter nodeTagsWriter;
    private LoaderFileWriter waysWriter;
    private LoaderFileWriter wayTagsWriter;
    private LoaderFileWriter wayNodesWriter;
    private LoaderFileWriter relationsWriter;
    private LoaderFileWriter relationTagsWriter;
    private LoaderFileWriter relationMembersWriter;

    private String outputDir;
    
    public LoaderFileOsmElementHandler(String outputDir)
    {
        this.outputDir = outputDir;
        nodesWriter = createWriter("nodes.txt");
        nodeTagsWriter = createWriter("node_tags.txt");
        waysWriter = createWriter("ways.txt");
        wayTagsWriter = createWriter("way_tags.txt");
        wayNodesWriter = createWriter("way_nodes.txt");
        relationsWriter = createWriter("relations.txt");
        relationTagsWriter = createWriter("relation_tags.txt");
        relationMembersWriter = createWriter("relation_members.txt");
    }
    
    private LoaderFileWriter createWriter(String fileName)
    {
        File file = new File(outputDir, fileName);
        try
        {
            LoaderFileWriter writer = new LoaderFileWriter(file);
            return writer;
        }
        catch (FileNotFoundException exc)
        {
            throw new OmadacException(exc);
        }
        catch (UnsupportedEncodingException exc)
        {
            throw new OmadacException(exc);
        }
    }
    
    public void close()
    {
        nodesWriter.close();
        nodeTagsWriter.close();
        waysWriter.close();
        wayTagsWriter.close();
        wayNodesWriter.close();
        relationsWriter.close();
        relationTagsWriter.close();
        relationMembersWriter.close();
    }

    @Override
    protected void handleNode(Node node)
    {
        Long id = node.getId();
        nodesWriter.writeColumn(id);
        nodesWriter.writeColumn(transform(node.getLat()));
        nodesWriter.writeColumn(transform(node.getLon()));
        nodesWriter.terminateRow();
        
        for (Tag tag : node.getTag())
        {
            nodeTagsWriter.writeColumn(id);
            nodeTagsWriter.writeColumn(tag.getK());
            nodeTagsWriter.writeColumn(tag.getV());
            nodeTagsWriter.terminateRow();
        }
    }

    private int transform(double d)
    {
        return (int) (d*1E7);
    }

    @Override
    protected void handleWay(Way way)
    {
        Long id = way.getId();
        waysWriter.writeColumn(id);
        waysWriter.terminateRow();

        for (Tag tag : way.getTag())
        {
            wayTagsWriter.writeColumn(id);
            wayTagsWriter.writeColumn(tag.getK());
            wayTagsWriter.writeColumn(tag.getV());
            wayTagsWriter.terminateRow();
        }
        
        int seqNum = 0;
        for (NodeRef node : way.getNd())
        {
            wayNodesWriter.writeColumn(id);
            wayNodesWriter.writeColumn(node.getRef());
            wayNodesWriter.writeColumn(seqNum);
            wayNodesWriter.terminateRow();
            seqNum++;
        }
    }

    @Override
    protected void handleBounds(Bounds bounds)
    {
        // nothing
    }

    @Override
    protected void handleRelation(Relation relation)
    {
        Long id = relation.getId();
        relationsWriter.writeColumn(id);
        relationsWriter.terminateRow();

        for (Tag tag : relation.getTag())
        {
            relationTagsWriter.writeColumn(id);
            relationTagsWriter.writeColumn(tag.getK());
            relationTagsWriter.writeColumn(tag.getV());
            relationTagsWriter.terminateRow();
        }
        
        int seqNum = 0;
        for (RelationMember member : relation.getMember())
        {
            relationMembersWriter.writeColumn(id);
            relationMembersWriter.writeColumn(member.getRef());
            relationMembersWriter.writeColumn(member.getRole());
            relationMembersWriter.writeColumn(typeToNumber(member.getType()));
            relationMembersWriter.writeColumn(seqNum);
            relationMembersWriter.terminateRow();
            seqNum++;
        }
    }

    private int typeToNumber(String type)
    {
        if ("node".equals(type))
            return 1;
        if ("way".equals(type))
            return 2;
        if ("relation".equals(type))
            return 3;
        throw new IllegalArgumentException(type);
    }
}
