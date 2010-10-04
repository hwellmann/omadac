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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.omadac.make.Target;
import org.omadac.make.util.NumberRange;
import org.omadac.nom.Feature;
import org.omadac.nom.NomFeatureType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapFeatureClassifierSubtarget extends Target
{
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(MapFeatureClassifierSubtarget.class);

    private NumberRange<Long> range;

    private EntityManager em;

    private int numFeatures;

    private Map<String, String> tagMap;
    

    public MapFeatureClassifierSubtarget(NumberRange<Long> range)
    {
        super(String.format("NomMapFeatureClassifier_%d_%d", range.getMinId(), range
                .getMaxId()));
        this.range = range;
    }

    @Override
    public void compile()
    {
        this.tagMap = new HashMap<String, String>();
        String sql = "select w.id as way_id, wt.k, wt.v " 
                + "from osm.ways w  "
                + "left join nom.link l "
                + "on w.id = l.feature_id " 
                + "left join osm.way_tags wt "
                + "on w.id = wt.id " 
                + "where l.feature_id is null "
                + "and w.id between ?1 and ?2 "
                + "order by w.id, wt.k    ";

        em = getCurrentEntityManager();
        Query query = em.createNativeQuery(sql);
        query.setParameter(1, range.getMinId());
        query.setParameter(2, range.getMaxId());

        @SuppressWarnings("unchecked")
        List<Object[]> resultList = query.getResultList();
        em.getTransaction().commit();
        
        long lastWayId = -1;
        long wayId = -1;
        String key = null;
        String value = null;
        numFeatures = 0;
        
        em = getCurrentEntityManager();
        for (Object[] results : resultList)
        {
            wayId = (Long) results[0];
            key = (String) results[1];
            value = (String) results[2];
            log.trace("{} {} {}", new Object[] { wayId, key, value });
            if (key == null)
            {
                log.error("way {} has no tags", wayId);
                updateFeatureType(lastWayId, tagMap);
                lastWayId = -1;
                continue;
            }

            if (wayId != lastWayId && lastWayId != -1)
            {
                updateFeatureType(lastWayId, tagMap);
            }
            tagMap.put(key, value);

            lastWayId = wayId;
        }
        updateFeatureType(wayId, tagMap);
        log.info(numFeatures + " features");
        em.getTransaction().commit();
    }
    
    private void updateFeatureType(long wayId, Map<String, String> tags)
    {
        numFeatures++;
        NomFeatureType featureType = determineFeatureType(tags);
        if (featureType == NomFeatureType.UNDEFINED)
        {
            log.warn("way {}: cannot match tags {}", wayId, tags);
        }
        
        Feature feature = em.find(Feature.class, wayId);
        if (feature == null)
        {
            log.warn("feature {} has no geometry; skipping tags", wayId);
        }
        else
        {
            feature.setFeatureType(featureType.getValue());
            em.merge(feature);            
        }
        tags.clear();
        
        if (numFeatures % 250 == 0)
        {
            log.info("{} features", numFeatures);
        }
    }

    private NomFeatureType determineFeatureType(Map<String, String> tags)
    {
        String aeroway = tags.get("aeroway");
        if (aeroway != null)
        {
            if (aeroway.equals("aerodrome"))
                return NomFeatureType.AREA_AIRPORT;

            if (aeroway.equals("apron"))
                return NomFeatureType.AREA_AIRPORT_APRON;

            if (aeroway.equals("runway"))
                return NomFeatureType.AREA_AIRPORT_RUNWAY;

            if (aeroway.equals("taxiway"))
                return NomFeatureType.AREA_AIRPORT_TAXIWAY;
        }
        
        String amenity = tags.get("amenity");
        if (amenity != null)
        {
            if (amenity.equals("arts_centre"))
                return NomFeatureType.AREA_ARTS_CENTRE_BUILDING;
            
            if (amenity.equals("cinema"))
                return NomFeatureType.AREA_CINEMA_BUILDING;
            
            if (amenity.equals("courthouse"))
                return NomFeatureType.AREA_GOVERNMENT_BUILDING;
            
            // FIXME
            if (amenity.equals("fuel"))
                return NomFeatureType.AREA_BUILDING;
            
            if (amenity.equals("grave_yard"))
                return NomFeatureType.AREA_CEMETERY;
            
            if (amenity.equals("hospital"))
                return NomFeatureType.AREA_HOSPITAL;
            
            if (amenity.equals("kindergarten"))
                return NomFeatureType.AREA_KINDERGARTEN_BUILDING;
            
            if (amenity.equals("library"))
                return NomFeatureType.AREA_GOVERNMENT_BUILDING;
            
            if (amenity.equals("parking"))
                return NomFeatureType.AREA_PARKING_LOT;
            
            if (amenity.equals("prison"))
                return NomFeatureType.AREA_GOVERNMENT_BUILDING;
            
            if (amenity.equals("public_building"))
                return NomFeatureType.AREA_GOVERNMENT_BUILDING;
            
            if (amenity.equals("public_library"))
                return NomFeatureType.AREA_GOVERNMENT_BUILDING;
            
            if (amenity.equals("school"))
                return NomFeatureType.AREA_GOVERNMENT_BUILDING;
            
            if (amenity.equals("theatre"))
                return NomFeatureType.AREA_THEATRE_BUILDING;
            
            if (amenity.equals("townhall"))
                return NomFeatureType.AREA_GOVERNMENT_BUILDING;
            
            if (amenity.equals("university"))
                return NomFeatureType.AREA_EDUCATION_BUILDING;
            
            return NomFeatureType.AREA_BUILDING;
            
        }
        
        String barrier = tags.get("barrier");
        if (barrier != null)
        {
            return NomFeatureType.LINE_BARRIER;
        }
        
        String boundary = tags.get("boundary");
        if (boundary != null)
        {
            if (boundary.equals("administrative"))
            {
                String level = tags.get("admin_level");
                {
                    if (level != null)
                    {
                        if (level.equals("7"))
                            return NomFeatureType.LINE_BORDER_ORDER_7;
                    }
                }
                return NomFeatureType.LINE_BORDER_ORDER_8;
            }
        }
        
        String building = tags.get("building");
        if (building != null)
        {
            if (building.equals("residential"))
                return NomFeatureType.AREA_RESIDENTIAL_BUILDING;
            
            return NomFeatureType.AREA_BUILDING;
        }
        
        String historic = tags.get("historic");
        if (historic != null)
        {
            if (historic.equals("memorial"))
            {
                return NomFeatureType.AREA_MEMORIAL;
            }
        }
        
        String island = tags.get("island");
        if (island != null)
        {
            return NomFeatureType.AREA_ISLAND;
        }
        
        String landuse = tags.get("landuse");
        if (landuse != null)
        {
            if (landuse.equals("allotments"))
                return NomFeatureType.AREA_ALLOTMENTS;
            
            if (landuse.equals("basin"))
                return NomFeatureType.AREA_LAKE;
            
            if (landuse.equals("brownfield"))
                return NomFeatureType.AREA_BROWNFIELD;
            
            if (landuse.equals("cemetery"))
                return NomFeatureType.AREA_CEMETERY;
            
            if (landuse.equals("commercial"))
                // FIXME
                return NomFeatureType.AREA_BUSINESS_BUILDING;

            if (landuse.equals("construction"))
                return NomFeatureType.AREA_CONSTRUCTION;
            
            if (landuse.equals("farm"))
                return NomFeatureType.AREA_FARMLAND;
            
            if (landuse.equals("farmland"))
                return NomFeatureType.AREA_FARMLAND;
            
            if (landuse.equals("farmyard"))
                // FIXME
                return NomFeatureType.AREA_BUSINESS_BUILDING;
            
            if (landuse.equals("forest"))
                // FIXME
                return NomFeatureType.AREA_WOODLAND;

            if (landuse.equals("grass"))
                // FIXME
                return NomFeatureType.AREA_MEADOW;

            if (landuse.equals("greenfield"))
                return NomFeatureType.AREA_GREENFIELD;

            if (landuse.equals("industrial"))
                return NomFeatureType.AREA_INDUSTRIAL;
            
            if (landuse.equals("meadow"))
                return NomFeatureType.AREA_MEADOW;
            
            if (landuse.equals("military"))
                return NomFeatureType.AREA_MILITARY_BASE;
            
            if (landuse.equals("plaza"))
                return NomFeatureType.AREA_TRAFFIC;
            
            if (landuse.equals("quarry"))
                return NomFeatureType.AREA_INDUSTRIAL;
            
            if (landuse.equals("railway"))
                return NomFeatureType.AREA_INDUSTRIAL;
            
            if (landuse.equals("recreation_ground"))
                // FIXME
                return NomFeatureType.AREA_PARK;
            
            if (landuse.equals("reservoir"))
                return NomFeatureType.AREA_LAKE;
            
            if (landuse.equals("residential"))
                return NomFeatureType.AREA_BUILT_UP;
            
            if (landuse.equals("retail"))
                // FIXME
                return NomFeatureType.AREA_BUSINESS_BUILDING;            

            if (landuse.equals("village_green"))
                // FIXME
                return NomFeatureType.AREA_PARK;            

            if (landuse.equals("vineyard"))
                return NomFeatureType.AREA_VINEYARD;            
        }
        
        String leisure = tags.get("leisure");
        if (leisure != null)
        {
            if (leisure.equals("common"))
                return NomFeatureType.AREA_PARK;

            if (leisure.equals("fishing"))
                return NomFeatureType.AREA_SPORTS_COMPLEX;

            if (leisure.equals("garden"))
                return NomFeatureType.AREA_PARK;

            if (leisure.equals("golf_course"))
                return NomFeatureType.AREA_GOLF_COURSE;

            if (leisure.equals("marina"))
                return NomFeatureType.AREA_GOLF_COURSE;

            if (leisure.equals("miniature_golf"))
                return NomFeatureType.AREA_SPORTS_COMPLEX;

            if (leisure.equals("nature_reserve"))
                return NomFeatureType.AREA_NATIONAL_PARK;

            if (leisure.equals("park"))
                return NomFeatureType.AREA_PARK;

            if (leisure.equals("pitch"))
                return NomFeatureType.AREA_SPORTS_COMPLEX;

            if (leisure.equals("playground"))
                return NomFeatureType.AREA_PLAYGROUND;

            if (leisure.equals("sports_centre"))
                return NomFeatureType.AREA_SPORTS_COMPLEX;

            if (leisure.equals("stadium"))
                return NomFeatureType.AREA_SPORTS_COMPLEX;

            if (leisure.equals("track"))
                return NomFeatureType.AREA_SPORTS_COMPLEX;

            if (leisure.equals("water_park"))
                return NomFeatureType.AREA_SPORTS_COMPLEX;

        }
        
        String manMade = tags.get("man_made");
        if (manMade != null)
        {
            if (manMade.equals("pier"))
                return NomFeatureType.LINE_PIER;
            
            if (manMade.equals("wastewater_plant"))
                return NomFeatureType.AREA_INDUSTRIAL;

            if (manMade.equals("works"))
                return NomFeatureType.AREA_INDUSTRIAL;
        }
                
        String military = tags.get("military");
        if (military != null)
        {
            return NomFeatureType.AREA_MILITARY_BASE;
        }
                
        String natural = tags.get("natural");
        if (natural != null)
        {
            if (natural.equals("beach"))
                return NomFeatureType.AREA_BEACH;
            
            if (natural.equals("cave_entrance"))
                // FIXME
                return NomFeatureType.UNDEFINED;

            if (natural.equals("cliff"))
                return NomFeatureType.AREA_CLIFF;

            if (natural.equals("fell"))
                // FIXME
                return NomFeatureType.UNDEFINED;

            if (natural.equals("forest"))
                return NomFeatureType.AREA_WOODLAND;

            if (natural.equals("glacier"))
                // FIXME
                return NomFeatureType.UNDEFINED;

            if (natural.equals("heath"))
                // FIXME
                return NomFeatureType.AREA_HEATH;

            if (natural.equals("marsh"))
                // FIXME
                return NomFeatureType.AREA_MARSH;

            if (natural.equals("meadow"))
                // FIXME
                return NomFeatureType.AREA_MEADOW;

            if (natural.equals("mud"))
                // FIXME
                return NomFeatureType.UNDEFINED;

            if (natural.equals("scree"))
                // FIXME
                return NomFeatureType.UNDEFINED;

            if (natural.equals("scrub"))
                return NomFeatureType.AREA_SCRUB;

            if (natural.equals("water"))
                return NomFeatureType.AREA_LAKE;

            if (natural.equals("wetland"))
            {
                if ("marsh".equals(tags.get("wetland")))
                    return NomFeatureType.AREA_MARSH;
            }

            if (natural.equals("wood"))
                return NomFeatureType.AREA_WOODLAND;
        }
        
        String place = tags.get("place");
        if (place != null)
        {
            if (place.equals("island"))
                return NomFeatureType.AREA_ISLAND;
        }
        
        String power = tags.get("power");
        if (power != null)
        {
            if (power.equals("line"))
                return NomFeatureType.LINE_POWER;

            if (power.equals("sub_station"))
                return NomFeatureType.AREA_POWER_STATION;
        }
        
        String route = tags.get("route");
        if (route != null)
        {
            if (route.equals("ferry"))
                return NomFeatureType.LINE_ROAD;
        }
        
        String railway = tags.get("railway");
        if (railway != null)
        {
            return NomFeatureType.LINE_RAILWAY;
        }
        
        String shop = tags.get("shop");
        if (shop != null)
        {
            return NomFeatureType.AREA_BUSINESS_BUILDING;
        }
        
        String sport = tags.get("sport");
        if (sport != null)
        {
            return NomFeatureType.AREA_SPORTS_COMPLEX;
        }
        
        String tourism = tags.get("tourism");
        if (tourism != null)
        {
            if (tourism.equals("museum"))
                return NomFeatureType.AREA_MUSEUM_BUILDING;
        }
        
        String waterway = tags.get("waterway");
        if (waterway != null)
        {
            if (waterway.equals("bank"))
                return NomFeatureType.AREA_SEAPORT;

            if (waterway.equals("canal"))
                return NomFeatureType.LINE_CANAL;

            if (waterway.equals("dam"))
                return NomFeatureType.LINE_DAM;

            if (waterway.equals("dock"))
                return NomFeatureType.AREA_BAY;

            if (waterway.equals("drain"))
                return NomFeatureType.LINE_CANAL;

            if (waterway.equals("quay"))
                return NomFeatureType.AREA_SEAPORT;

            if (waterway.equals("river"))
                return NomFeatureType.LINE_RIVER;

            if (waterway.equals("riverbank"))
                return NomFeatureType.AREA_RIVER;

            if (waterway.equals("stream"))
                return NomFeatureType.LINE_RIVER;
        }
        
        
        String interpolation = tags.get("addr:interpolation");
        if (interpolation != null)
        {
            return NomFeatureType.LINE_ADDRESS_INTERPOLATION;
        }

        return NomFeatureType.UNDEFINED;
    }
}
