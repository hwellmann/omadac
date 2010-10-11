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
import org.omadac.nom.NomFeatureType;
import org.omadac.nom.Poi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class PointFeatureSubtarget extends Target
{
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(PointFeatureSubtarget.class);

    private NumberRange<Long> range;

    private EntityManager em;

    private int numFeatures;

    private GeometryFactory factory;
    
    private Map<String, String> tagMap;
    

    public PointFeatureSubtarget(NumberRange<Long> range)
    {
        super(String.format("NomPointFeatures_%d_%d", range.getMinId(), range
                .getMaxId()));
        this.range = range;
    }

    @Override
    public void compile()
    {
        List<Object[]> results = readNodes();        
        processResults(results);
    }

    private List<Object[]> readNodes()
    {
        String sql = "select n.id, n.longitude, n.latitude, nt.k, nt.v " 
            + "from osm.nodes n "
            + "join osm.node_tags nt "
            + "on n.id = nt.id "
            + "where n.id between ?1 and ?2 "
            + "order by n.id";

        em = getCurrentEntityManager();
        Query query = em.createNativeQuery(sql);
        query.setParameter(1, range.getMinId());
        query.setParameter(2, range.getMaxId());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        em.getTransaction().commit();
        return results;
    }

    private void processResults(List<Object[]> results)
    {
        factory = new GeometryFactory();
        tagMap = new HashMap<String, String>();
        Long nodeId = null;
        int x = 0;
        int y = 0;
        String key;
        String value;
        Long lastNodeId = null;
        numFeatures = 0;
        em = getCurrentEntityManager();
        for (Object[] result : results)
        {
            nodeId = (Long) result[0];
            key = (String) result[3];
            value = (String) result[4];

            // need to compare objects with equals(), not ==
            if (! nodeId.equals(lastNodeId))
            {
                if (lastNodeId != null)
                {
                    createFeature(lastNodeId, x, y, tagMap);
                    tagMap.clear();
                }
                x = (Integer) result[1];
                y = (Integer) result[2];
                lastNodeId = nodeId;
            }
            tagMap.put(key, value);

            log.trace("{} {} {} {} {}", new Object[] { nodeId, x, y, key, value });
        }
        createFeature(nodeId, x, y, tagMap);
        log.info(numFeatures + " features");
        em.getTransaction().commit();
    }

    private void createFeature(long nodeId, int x, int y, Map<String, String> tags)
    {
        NomFeatureType type = determineFeatureType(tags);        
        if (type == NomFeatureType.UNDEFINED)
        {
            log.warn("node {}: cannot match tags {}", nodeId, tags);
        }
        else
        {
            Coordinate c = new Coordinate(x, y);
            Point point = factory.createPoint(c);
            Poi poi = new Poi(type.getValue(), nodeId, point);
            log.debug("creating POI {}", nodeId);
            em.persist(poi);
            numFeatures++;
        }
    }

    
    private NomFeatureType determineFeatureType(Map<String, String> tags)
    {
        String amenity = tags.get("amenity");
        if (amenity != null)
        {
            if (amenity.equals("atm"))
                return NomFeatureType.POI_ATM;
            
            if (amenity.equals("bank"))
                return NomFeatureType.POI_BANK;
            
            if (amenity.equals("bar"))
                return NomFeatureType.POI_COCKTAIL_LOUNGE;
            
            if (amenity.equals("bench"))
                return NomFeatureType.POI_BENCH;
            
            if (amenity.equals("bicycle_rental"))
                return NomFeatureType.POI_BICYCLE_RENTAL;
            
            if (amenity.equals("cafe"))
                return NomFeatureType.POI_COFFEE_SHOP;

            if (amenity.equals("car_sharing"))
                return NomFeatureType.POI_CAR_SHARING;

            if (amenity.equals("doctors"))
                return NomFeatureType.POI_PHYSICIAN;

            // FIXME
            if (amenity.equals("driving_school"))
                return NomFeatureType.UNDEFINED;

            if (amenity.equals("fast_food"))
                return NomFeatureType.POI_FAST_FOOD;

            if (amenity.equals("fire_station"))
                return NomFeatureType.POI_FIRE_DEPARTMENT;

            if (amenity.equals("fountain"))
                return NomFeatureType.POI_FOUNTAIN;

            if (amenity.equals("fuel"))
                return NomFeatureType.POI_PETROL_STATION;

            if (amenity.equals("kindergarten"))
                return NomFeatureType.POI_KINDERGARTEN;

            if (amenity.equals("library"))
                return NomFeatureType.POI_LIBRARY;

            if (amenity.equals("nightclub"))
                return NomFeatureType.POI_NIGHT_CLUB;

            if (amenity.equals("parking"))
                return NomFeatureType.POI_PARKING_LOT;

            if (amenity.equals("pharmacy"))
                return NomFeatureType.POI_PHARMACY;

            if (amenity.equals("place_of_worship"))
                return NomFeatureType.POI_PLACE_OF_WORSHIP;

            if (amenity.equals("police"))
                return NomFeatureType.POI_POLICE_STATION;

            if (amenity.equals("post_box"))
                return NomFeatureType.POI_POST_BOX;

            if (amenity.equals("post_office"))
                return NomFeatureType.POI_POST_OFFICE;
            
            if (amenity.equals("pub"))
                return NomFeatureType.POI_BAR_OR_PUB;
            
            // FIXME
            if (amenity.equals("public_building"))
                return NomFeatureType.POI_GOVERNMENT_OFFICES;
            
            if (amenity.equals("recycling"))
                return NomFeatureType.POI_RECYCLING_BINS;
            
            if (amenity.equals("restaurant"))
                return NomFeatureType.POI_RESTAURANT;
            
            if (amenity.equals("school"))
                return NomFeatureType.POI_SCHOOL;
            
            if (amenity.equals("taxi"))
                return NomFeatureType.POI_TAXI;
            
            if (amenity.equals("telephone"))
                return NomFeatureType.POI_TELEPHONE_SERVICE;
            
            if (amenity.equals("theatre"))
                return NomFeatureType.POI_THEATER;
            
            if (amenity.equals("toilets"))
                return NomFeatureType.POI_TOILETS;
            
            // FIXME
            if (amenity.equals("vending_machine"))
                return NomFeatureType.UNDEFINED;
            
            if (amenity.equals("veterinary"))
                return NomFeatureType.POI_VETERINARIAN_SERVICE;
            
            if (amenity.equals("youth_centre"))
                return NomFeatureType.POI_YOUTH_CLUB;            
            
        }
        
        String leisure = tags.get("leisure");
        if (leisure != null)
        {
            if (leisure.equals("fitness_centre"))
                return NomFeatureType.POI_HEALTH_CLUB;
            
            if (leisure.equals("pitch"))
                return NomFeatureType.POI_SPORTS_COMPLEX;
            
            // FIXME
            if (leisure.equals("playground"))
                return NomFeatureType.UNDEFINED;
            
            if (leisure.equals("sports_centre"))
                return NomFeatureType.POI_SPORTS_COMPLEX;
            
        }
        
        String parking = tags.get("parking");
        if (parking != null)
        {
            if (parking.equals("multi-storey"))
                return NomFeatureType.POI_PARKING_GARAGE;
            
            if (parking.equals("surface"))
                return NomFeatureType.POI_PARKING_LOT;
            
            if (parking.equals("underground"))
                return NomFeatureType.POI_PARKING_GARAGE;
            
        }
        
        String railway = tags.get("railway");
        if (railway != null)
        {
            if (railway.equals("station"))
                return NomFeatureType.POI_RAIL_STATION;
        }
        
        String shop = tags.get("shop");
        if (shop != null)
        {
            if (shop.equals("bakery"))
                return NomFeatureType.POI_BAKERY;

            if (shop.equals("beverage"))
                return NomFeatureType.POI_WINE_AND_LIQUOR;

            if (shop.equals("bicycle"))
                return NomFeatureType.POI_BICYCLE_DEALER;

            if (shop.equals("butcher"))
                return NomFeatureType.POI_BICYCLE_DEALER;
            
            if (shop.equals("car"))
                return NomFeatureType.POI_AUTO_DEALER;

            if (shop.equals("car_repair"))
                return NomFeatureType.POI_AUTO_SERVICE;

            // FIXME
            if (shop.equals("chemist"))
                return NomFeatureType.UNDEFINED;

            if (shop.equals("computer"))
                return NomFeatureType.POI_COMPUTER_AND_SOFTWARE;
            
            // FIXME
            if (shop.equals("drugstore"))
                return NomFeatureType.UNDEFINED;

            if (shop.equals("dry_cleaning"))
                return NomFeatureType.POI_CLEANING_AND_LAUNDRY;

            if (shop.equals("electronics"))
                return NomFeatureType.POI_CONSUMER_ELECTRONICS_STORE;

            // FIXME
            if (shop.equals("fish"))
                return NomFeatureType.POI_GROCERY_STORE;

            if (shop.equals("florist"))
                return NomFeatureType.POI_FLOWERS;

            if (shop.equals("furniture"))
                return NomFeatureType.POI_FURNITURE_STORE;

            if (shop.equals("hairdresser"))
                return NomFeatureType.POI_HAIR_AND_BEAUTY;

            // FIXME
            if (shop.equals("ice_cream"))
                return NomFeatureType.POI_COFFEE_SHOP;

            if (shop.equals("kiosk"))
                return NomFeatureType.POI_CONVENIENCE_STORE;

            // FIXME
            if (shop.equals("newsagent"))
                return NomFeatureType.UNDEFINED;

            if (shop.equals("optician"))
                return NomFeatureType.POI_OPTICAL;

            // FIXME
            if (shop.startsWith("papers"))
                return NomFeatureType.UNDEFINED;

            if (shop.startsWith("photo"))
                return NomFeatureType.POI_PHOTOGRAPHY;

            // FIXME
            if (shop.startsWith("shoe_repair"))
                return NomFeatureType.POI_SHOE_STORE;

            if (shop.startsWith("shoes"))
                return NomFeatureType.POI_SHOE_STORE;

            if (shop.startsWith("supermarket"))
                return NomFeatureType.POI_GROCERY_STORE;

            // FIXME
            if (shop.startsWith("toys"))
                return NomFeatureType.POI_SHOPPING;

            if (shop.startsWith("wine"))
                return NomFeatureType.POI_WINERY;

        }
        return NomFeatureType.UNDEFINED;
    }    
}
