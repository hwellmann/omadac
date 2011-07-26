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
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.omadac.make.ComplexStep;
import org.omadac.make.util.NumberRange;
import org.omadac.nom.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

public class MapFeatureStep implements ComplexStep<MapFeatureComplexTarget, MapFeatureSubtarget> 
{
    private static Logger log = LoggerFactory.getLogger(MapFeatureStep.class);
    private static final int NUM_LINKS = 5000;

    private EntityManager em;

    private GeometryFactory factory = new GeometryFactory();

    public MapFeatureStep()
    {
    }

    public void setEntityManager(EntityManager em)
    {
        this.em = em;
    }

    @Override
    public List<MapFeatureSubtarget> split(MapFeatureComplexTarget target)
    {
        List<MapFeatureSubtarget> subtargets = new ArrayList<MapFeatureSubtarget>();
        List<NumberRange<Long>> ranges = getRanges(NUM_LINKS);
        for (NumberRange<Long> range : ranges)
        {
            MapFeatureSubtarget subtarget = new MapFeatureSubtarget(range);
            subtargets.add(subtarget);
        }
        
        return subtargets;
    }

    @Override
    public void merge(MapFeatureComplexTarget target)
    {
        em.createNativeQuery("ALTER TABLE nom.feature " +
                "ADD CONSTRAINT pk_feature " +
                "PRIMARY KEY(feature_id)").executeUpdate();
    }

    @Override
    public void compile(MapFeatureSubtarget target)
    {        
        factory = new GeometryFactory();
        target.coords = new ArrayList<Coordinate>();
        String sql = "select w.id as way_id, wn.sequence_id as seq_num, "
                + "n.id as node_id, n.latitude, n.longitude "
                + "from osm.ways w  " 
                + "left join nom.link l "
                + "on w.id = l.feature_id " 
                + "join osm.way_nodes wn "
                + "on w.id = wn.id " 
                + "join osm.nodes n "
                + "on wn.node_id = n.id " 
                + "where w.id between ?1 and ?2 " 
                + "and l.feature_id is null "
                + "order by w.id, seq_num";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, target.getRange().getMinId());
        query.setParameter(2, target.getRange().getMaxId());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        long nodeId = -1;
        int latitude = 0;
        int longitude = 0;
        long wayId = -1;
        long lastWayId = -1;
        target.numFeatures = 0;
        for (Object[] result : results)
        {
            wayId = (Long) result[0];
            long seqNum = (Long) result[1];
            nodeId = (Long) result[2];
            latitude = (Integer) result[3];
            longitude = (Integer) result[4];
            log.trace("{} {} {} {} {}", new Object[] { wayId, seqNum, nodeId, latitude, longitude });

            if (seqNum == 0)
            {
                if (lastWayId != -1)
                {
                    createFeature(target, lastWayId);
                    target.coords.clear();
                }
            }
            lastWayId = wayId;

            target.coords.add(new Coordinate(longitude, latitude));
        }
        createFeature(target, wayId);
        log.info("{} features", target.numFeatures );
    }

    @Override
    public void clean(MapFeatureSubtarget target)
    {
    }

    @Override
    public void cleanAll(MapFeatureComplexTarget target)
    {
        String sql = "delete from nom.feature where discriminator = 'F'";
        em.createNativeQuery(sql).executeUpdate();
    }

    private List<NumberRange<Long>> getRanges(int rangeSize)
    {
        String sql = "select w.id from osm.ways w  "
            + "left join nom.link l "
            + "on w.id = l.feature_id " 
            + "where l.feature_id is null "
            + "order by w.id"; 

        em.clear();
        Query query = em.createNativeQuery(sql);
        
        @SuppressWarnings("unchecked")
        List<Long> ids = query.getResultList();
        
        List<NumberRange<Long>> ranges = NumberRange.split(ids, rangeSize);
        return ranges;
    }
    
    private void createFeature(MapFeatureSubtarget target, long wayId)
    {
        Coordinate[] c = new Coordinate[target.coords.size()];
        target.coords.toArray(c);
        Geometry geom;

        // Is the way closed?
        if (c[0].equals2D(c[c.length - 1]))
        {
            if (c.length < 4)
            {
                log.error("closed way {} has only {} points", wayId, c.length);
                return;
            }
            LinearRing ring = factory.createLinearRing(c);
            geom = factory.createPolygon(ring, null);
        }
        else
        {
            geom = factory.createLineString(c);
        }
        if (!geom.isValid())
        {
            log.error("way {} has invalid geometry", wayId);
            return;
        }
        target.numFeatures++;

        // use a dummy feature type and an autogenerated ID
        Feature feature = new Feature(0, wayId, geom);
        em.persist(feature);

        if (target.numFeatures % 1000 == 0)
        {
            log.info("{} features", target.numFeatures);
        }
    }
}
