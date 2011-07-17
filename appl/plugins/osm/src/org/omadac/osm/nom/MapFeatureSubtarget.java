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
package org.omadac.osm.nom;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.omadac.make.Target;
import org.omadac.make.util.NumberRange;
import org.omadac.nom.Feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

public class MapFeatureSubtarget extends Target
{
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(MapFeatureSubtarget.class);

    private NumberRange<Long> range;

    private EntityManager em;

    private List<Coordinate> coords;

    private int numFeatures;

    private GeometryFactory factory;

    public MapFeatureSubtarget(NumberRange<Long> range)
    {
        super(String.format("NomMapFeatures_%d_%d", range.getMinId(), range.getMaxId()));
        this.range = range;
    }

    @Override
    public void compile()
    {
        factory = new GeometryFactory();
        coords = new ArrayList<Coordinate>();
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
        query.setParameter(1, range.getMinId());
        query.setParameter(2, range.getMaxId());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        em.getTransaction().commit();
        
        long nodeId = -1;
        int latitude = 0;
        int longitude = 0;
        long wayId = -1;
        long lastWayId = -1;
        numFeatures = 0;
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
                    createFeature(lastWayId);
                    coords.clear();
                }
            }
            lastWayId = wayId;

            coords.add(new Coordinate(longitude, latitude));
        }
        createFeature(wayId);
        log.info(numFeatures + " features");
    }

    private void createFeature(long wayId)
    {
        Coordinate[] c = new Coordinate[coords.size()];
        coords.toArray(c);
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
        numFeatures++;
        Feature feature = new Feature(wayId, geom);
        feature.setSourceId(wayId);
        //em.persist(feature);

        if (numFeatures % 1000 == 0)
        {
            log.info("{} features", numFeatures);
        }
    }
}
