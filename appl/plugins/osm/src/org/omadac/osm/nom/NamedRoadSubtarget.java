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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.omadac.make.Target;
import org.omadac.make.util.NumberRange;
import org.omadac.nom.AdminRegion;
import org.omadac.nom.NamedRoad;
import org.omadac.nom.NamedRoadLink;
import org.omadac.nom.HouseNumberRange;
import org.omadac.nom.NomLink;
import org.omadac.nom.RoadName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamedRoadSubtarget extends Target
{
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(NamedRoadSubtarget.class);

    // FIXME
    private static final long HAMBURG_ORDER8 = 3;

    private NumberRange<Long> range;

    private EntityManager em;

    private String lastStreetName;

    private int numResults;
    
    private NamedRoadHandle lastRoad;

    private NamedRoad namedRoad;

    private AdminRegion order8Region;

    private HouseNumberRange addressRange;
    
    private Map<Long, RoadName> roadNameMap;

    private List<Object[]> linksAndNames;
    
    private List<NamedRoad> namedRoads;

    public NamedRoadSubtarget(NumberRange<Long> range)
    {
        super(String.format("NomNamedRoads_%d_%d", range.getMinId(), range.getMaxId()));
        this.range = range;
    }

    @Override
    public void compile()    
    {
        init();
        loadLinksAndNames();
        createNamedRoads();
        saveNamedRoads();
    }
    
    private void init()
    {
        roadNameMap = new HashMap<Long, RoadName>();
        namedRoads = new ArrayList<NamedRoad>();
        
        em = getCurrentEntityManager();
        addressRange = em.find(HouseNumberRange.class, 1);
    }

    @SuppressWarnings("unchecked")
    public void loadLinksAndNames()
    {
        numResults = 0;
        
        String jpql = "select r from RoadName r where r.id between :minId and :maxId";
        em = getCurrentEntityManager();
        TypedQuery<RoadName> q = em.createQuery(jpql, RoadName.class);
        q.setParameter("minId", range.getMinId());
        q.setParameter("maxId", range.getMaxId());
        List<RoadName> roadNames = q.getResultList();
        

        String sql = "select f.feature_id, wt.v as name, rn.road_name_id::bigint "
                + "from osm.way_tags wt " 
                + "join nom.feature f "
                + "on wt.id = f.source_id "
                + "join nom.link l "
                + "on f.feature_id = l.feature_id " 
                + "join nom.road_name rn "
                + "on wt.v = rn.name "
                + "where wt.k = 'name' " 
                + "and rn.road_name_id between ?1 and ?2 "
                + "order by rn.road_name_id";
        
        Query query = em.createNativeQuery(sql);
        query.setParameter(1, range.getMinId());
        query.setParameter(2, range.getMaxId());

        linksAndNames = query.getResultList();
        em.getTransaction().commit();
        
        for (RoadName name : roadNames)
        {
            roadNameMap.put(name.getId(), name);
        }
    }
    
    private void createNamedRoads()
    {
        for (Object[] result : linksAndNames)
        {
            Long linkId = (Long) result[0];
            String roadName = (String) result[1];
            Long roadNameId = (Long) result[2];
            handleRoadLink(linkId, roadName, roadNameId);
            numResults++;
        }

        log.info(numResults + " links");
    }

    private void handleRoadLink(Long linkId, String streetName, long roadNameId)
    {
        log.debug("link = {}, name = {}, id = {}", new Object[] {linkId, streetName, roadNameId});

        if (!streetName.equals(lastStreetName))
        {
            lastStreetName = streetName;
        }

        NamedRoadHandle currentRoad = new NamedRoadHandle(HAMBURG_ORDER8, null, roadNameId);

        if (!currentRoad.equals(lastRoad))
        {
            namedRoad = new NamedRoad();
            namedRoads.add(namedRoad);
            namedRoad.setLinks(new HashSet<NamedRoadLink>());
            RoadName roadName = roadNameMap.get(roadNameId);
            namedRoad.setRoadName(roadName);
            lastRoad = currentRoad;
        }

        NamedRoadLink NomLink = new NamedRoadLink();
        NomLink.setId(linkId.intValue());
        NomLink.setLink(new NomLink(linkId));
        NomLink.setLeftRangeId(addressRange.getId());
        NomLink.setRightRangeId(addressRange.getId());
        NomLink.setNamedRoad(namedRoad);

        namedRoad.getLinks().add(NomLink);
    }

    private void saveNamedRoads()
    {
        em.getTransaction().begin();
        order8Region = em.find(AdminRegion.class, HAMBURG_ORDER8);
        for (NamedRoad road : namedRoads)
        {
            road.setOrder8(order8Region);
            em.persist(road);
        }
        em.getTransaction().commit();
    }
}
