/*
 *    Omadac - The Open Map Database Compiler
 *    http://omadac.org
 * 
 *    (C) 2011, Harald Wellmann and Contributors
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
package org.omadac.osm.nom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.omadac.geom.CoordinateListSequence;
import org.omadac.geom.LineNormalizer;
import org.omadac.jpa.JpaUtil;
import org.omadac.jpa.MetadataInspector;
import org.omadac.make.ComplexStep;
import org.omadac.make.util.NumberRange;
import org.omadac.nom.NomFeatureType;
import org.omadac.nom.NomJunction;
import org.omadac.nom.NomLink;
import org.omadac.nom.RoadAttributes;
import org.omadac.osm.model.OsmNode;
import org.omadac.osm.model.OsmWay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class LinkStep implements ComplexStep<LinkComplexTarget, LinkSubtarget> 
{
    private static Logger log = LoggerFactory.getLogger(LinkStep.class);

    private static final int NUM_LINKS = 1000;

    private EntityManager em;

    private Map<String, RoadAttributes> highwayTypeMap;

    private List<RoadAttributes> roadAttr;

    private GeometryFactory factory;

    private LineNormalizer normalizer;

    private LinkDao linkDao;
    

    public LinkStep()
    {
        factory = new GeometryFactory();
        normalizer = new LineNormalizer();
        roadAttr = new ArrayList<RoadAttributes>();
        highwayTypeMap = new HashMap<String, RoadAttributes>();
        createRoadAttributes();
    }

    public void setEntityManager(EntityManager em)
    {
        this.em = em;
    }

    public void setLinkDao(LinkDao linkDao)
    {
        this.linkDao = linkDao;
    }
    
    @Override
    public List<LinkSubtarget> split(LinkComplexTarget target)
    {
        persistRoadAttributes();
        
        List<LinkSubtarget> subtargets = new ArrayList<LinkSubtarget>();
        List<NumberRange<Long>> ranges = getRanges(NUM_LINKS);
        for (NumberRange<Long> range : ranges)
        {
            LinkSubtarget subtarget = new LinkSubtarget(range);
            subtarget.setStep(this);
            subtargets.add(subtarget);
        }
        
        return subtargets;
    }

    @Override
    public void merge(LinkComplexTarget target)
    {
        em.clear();
        em.createNativeQuery(
            "alter table nom.link "
            + "add constraint pk_link "
            + "primary key (feature_id)").executeUpdate();
    }

    @Override
    public void compile(LinkSubtarget target)
    {
        
        List<Object[]> results = linkDao.loadWays(target);
        linkDao.loadJunctions(target, target.junctionMap, target.nodeMap);
        
        for (Object[] result : results)
        {
            OsmWay way = (OsmWay) result[0];
            String tagValue = (String) result[1];
            createLink(target, way, tagValue);
        }        
        
        linkDao.saveFeatures(target.newJunctions, target.links);
        log.info("done");
    }

    @Override
    public void clean(LinkSubtarget target)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cleanAll(LinkComplexTarget target)
    {
        MetadataInspector inspector = JpaUtil.getMetadataInspector(em);
        inspector.cleanTable("nom", "link");        

        String sql = "delete from nom.feature where discriminator = 'L'";
        em.createNativeQuery(sql).executeUpdate();
    }

    private void createRoadAttributes()
    {
        for (int i = 0; i < 8; i++)
        {
            RoadAttributes attr = new RoadAttributes();
            attr.setId(i+1);
            attr.setFunctionalClass(i);
            attr.setTravelDirection('B');
            roadAttr.add(attr);
        }

        highwayTypeMap.put("bridlepath", roadAttr.get(7));
        highwayTypeMap.put("bridleway", roadAttr.get(7));
        highwayTypeMap.put("construction", roadAttr.get(7));
        highwayTypeMap.put("cycleway", roadAttr.get(7));
        highwayTypeMap.put("footway", roadAttr.get(7));
        highwayTypeMap.put("living_street", roadAttr.get(4));
        highwayTypeMap.put("minor", roadAttr.get(4));
        highwayTypeMap.put("motorway", roadAttr.get(0));
        highwayTypeMap.put("motorway_link", roadAttr.get(0));
        highwayTypeMap.put("path", roadAttr.get(7));
        highwayTypeMap.put("pedestrian", roadAttr.get(7));
        highwayTypeMap.put("platform", roadAttr.get(7));
        highwayTypeMap.put("primary", roadAttr.get(1));
        highwayTypeMap.put("primary_link", roadAttr.get(1));
        highwayTypeMap.put("residential", roadAttr.get(4));
        highwayTypeMap.put("road", roadAttr.get(5));
        highwayTypeMap.put("secondary", roadAttr.get(2));
        highwayTypeMap.put("service", roadAttr.get(7));
        highwayTypeMap.put("steps", roadAttr.get(7));
        highwayTypeMap.put("tertiary", roadAttr.get(3));
        highwayTypeMap.put("track", roadAttr.get(6));
        highwayTypeMap.put("trunk", roadAttr.get(2));
        highwayTypeMap.put("trunk_link", roadAttr.get(2));
        highwayTypeMap.put("unclassified", roadAttr.get(7));
        highwayTypeMap.put("unsurfaced", roadAttr.get(7));
    }    
    
    private void persistRoadAttributes() {
        for (RoadAttributes ra : roadAttr) {
            em.persist(ra);
        }
    }

    private List<NumberRange<Long>> getRanges(int rangeSize)
    {
        Query query = em
                .createNativeQuery("select distinct id from osm.way_tags wt "
                        + "where wt.k = 'highway' order by id");
        
        @SuppressWarnings("unchecked")
        List<Long> ids = query.getResultList();
        
        List<NumberRange<Long>> ranges = NumberRange.split(ids, rangeSize);
        return ranges;
    }
    
    
    private void createLink(LinkSubtarget target, OsmWay way, String highwayType)
    {
        int seqNum = 0;
        for (OsmNode node : way.getNodes())
        {
            NomJunction junction = target.junctionMap.get(node.getId());
            assert !((seqNum == 0) || (seqNum == way.getNodes().size()-1)) 
                || junction != null;

            Coordinate coord = new Coordinate(node.getLongitude(), node.getLatitude());
            target.nodeMap.put(coord, junction);
            if (seqNum++ == 0)
            {
                target.coords = new ArrayList<Coordinate>();
                target.coords.add(coord);
            }
            else
            {
                if (junction == null)
                {
                    target.coords.add(coord);
                }
                else
                {
                    target.coords.add(coord);
                    createLinkPart(target, way.getId(), highwayType);
                    target.coords = new ArrayList<Coordinate>();
                    target.coords.add(coord);
                }
            }
        }
    }
    
    private void createLinkPart(LinkSubtarget target, long wayId, String highwayType)
    {
        if (target.coords.size() == 1)
        {
            log.error("skipping way {} with only one node", wayId);
            return;
        }

        if (target.coords.size() == 2 && target.coords.get(0).equals(target.coords.get(1)))
        {
            log.error("skipping way {} collapsed to a point at {}", wayId,
                    target.coords.get(0));
            return;
        }

        LineString line = factory.createLineString(new CoordinateListSequence(
                target.coords));

        List<LineString> parts = normalizer.normalize(line);
        if (parts.size() > 1)
        {
            log.info("way {} split into {} parts", wayId, parts.size());
        }

        for (LineString part : parts)
        {
            assert part.getNumPoints() > 1 : "wayId = " + wayId;

            int length = LinkLengthCalculator.computeLinkLength(part);
            Coordinate fromCoord = part.getCoordinateN(0);
            Coordinate toCoord = part.getCoordinateN(part.getNumPoints() - 1);
            NomJunction fromNode = findOrCreateJunction(target, fromCoord);
            NomJunction toNode = findOrCreateJunction(target, toCoord);
            assert fromNode != null;
            assert toNode != null;
            
            assert !fromNode.equals(toNode);

            NomLink link = new NomLink();
//            link.setAttr(highwayTypeMap.get(highwayType));
//            if (link.getAttr() == null)
//            {
//                log.error("unknown highway type {}", highwayType);
//                link.setAttr(highwayTypeMap.get("unclassified"));
//            }
            link.setLength(length);

            link.setFeatureType(NomFeatureType.LINE_ROAD.getValue());            
            link.setSourceId(wayId);
            //link.setGeometry(part);
            link.getJunctions().add(fromNode);
            link.getJunctions().add(toNode);
            target.links.add(link);
        }
    }

    private NomJunction findOrCreateJunction(LinkSubtarget target, Coordinate coord)
    {
        NomJunction junction = target.nodeMap.get(coord);
        if (junction == null)
        {
            int x = (int) coord.x;
            int y = (int) coord.y;
            junction = new NomJunction(x, y);
            target.newJunctions.add(junction);
        }
        return junction;        
    }
}
