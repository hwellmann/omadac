package org.omadac.osm.nom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.omadac.geom.CoordinateListSequence;
import org.omadac.geom.LineNormalizer;
import org.omadac.jpa.JpaUtil;
import org.omadac.jpa.MetadataInspector;
import org.omadac.make.Step;
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

public class LinkStep implements Step<LinkComplexTarget, LinkSubtarget> 
{
    private static Logger log = LoggerFactory.getLogger(LinkStep.class);

    private static final int NUM_LINKS = 1000;

    private EntityManager em;

    private List<Coordinate> coords;

    private Map<String, RoadAttributes> highwayTypeMap;

    private List<RoadAttributes> roadAttr;

    private GeometryFactory factory;

    private LineNormalizer normalizer;

    private List<NomLink> links;

    private Map<Coordinate, NomJunction> nodeMap;

    private Set<NomJunction> newJunctions;

    private Map<Long, NomJunction> junctionMap;
    

    public void setEntityManager(EntityManager em)
    {
        this.em = em;
    }

    private void init()
    {
        coords = new ArrayList<Coordinate>();
        factory = new GeometryFactory();
        normalizer = new LineNormalizer();
        roadAttr = new ArrayList<RoadAttributes>();
        highwayTypeMap = new HashMap<String, RoadAttributes>();
        links = new ArrayList<NomLink>();
        nodeMap = new HashMap<Coordinate, NomJunction>();
        newJunctions = new HashSet<NomJunction>();
        createRoadAttributes();
    }

    
    
    @Override
    public List<LinkSubtarget> split(LinkComplexTarget target)
    {
        createRoadAttributes();
        
        List<LinkSubtarget> subtargets = new ArrayList<LinkSubtarget>();
        List<NumberRange<Long>> ranges = getRanges(NUM_LINKS);
        for (NumberRange<Long> range : ranges)
        {
            LinkSubtarget subtarget = new LinkSubtarget(range);
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
        init();
        
        List<Object[]> results = loadWays(target);
        loadJunctions(target);
        
        for (Object[] result : results)
        {
            OsmWay way = (OsmWay) result[0];
            String tagValue = (String) result[1];
            createLink(way, tagValue);
        }        
        
        saveFeatures();
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
        JpaUtil.commit();

        String sql = "delete from nom.feature where discriminator = 'L'";
        em.createNativeQuery(sql).executeUpdate();
        em.getTransaction().commit();        
    }

    private void createRoadAttributes()
    {
        for (int i = 0; i < 8; i++)
        {
            RoadAttributes attr = new RoadAttributes();
            attr.setId(i+1);
            attr.setFunctionalClass(i);
            attr.setTravelDirection('B');
            em.persist(attr);
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

    private List<NumberRange<Long>> getRanges(int rangeSize)
    {
        Query query = em
                .createNativeQuery("select distinct id from osm.way_tags wt "
                        + "where wt.k = 'highway' order by id");
        
        @SuppressWarnings("unchecked")
        List<Long> ids = query.getResultList();
        
        em.getTransaction().commit();
        List<NumberRange<Long>> ranges = NumberRange.split(ids, rangeSize);
        return ranges;
    }
    
    
    private void loadJunctions(LinkSubtarget subtarget)
    {
        String jpql;
        Query query;
        NumberRange<Long> range = subtarget.getRange();
        jpql = "select j from NomJunction j, OsmWay w join w.nodes as wn join w.tags as wt "
                + "where key(wt) = 'highway' and j.sourceId = wn.id and w.id between :minId and :maxId";
        query = em.createQuery(jpql);
        query.setParameter("minId", range.getMinId());
        query.setParameter("maxId", range.getMaxId());

        @SuppressWarnings("unchecked")
        List<NomJunction> junctions = query.getResultList();
        junctionMap = new HashMap<Long, NomJunction>();
        for (NomJunction junction : junctions)
        {
            junctionMap.put(junction.getSourceId(), junction);
            Coordinate coord = new Coordinate(junction.getX(), junction.getY());
            nodeMap.put(coord, junction);            
        }
    }

    private List<Object[]> loadWays(LinkSubtarget subtarget)
    {
        NumberRange<Long> range = subtarget.getRange();
        String jpql = "select w, wt from OsmWay w join fetch w.nodes join w.tags as wt "
                + "where key(wt) = 'highway' and w.id between :minId and :maxId order by w.id";

        Query query = em.createQuery(jpql);

        query.setParameter("minId", range.getMinId());
        query.setParameter("maxId", range.getMaxId());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results;
    }

    private void saveFeatures()
    {
        for (NomJunction junction : newJunctions)
        {
            em.persist(junction);
        }

        for (NomLink link : links)
        {
            em.persist(link);
        }
    }

    private void createLink(OsmWay way, String highwayType)
    {
        int seqNum = 0;
        for (OsmNode node : way.getNodes())
        {
            NomJunction junction = junctionMap.get(node.getId());
            assert !((seqNum == 0) || (seqNum == way.getNodes().size()-1)) 
                || junction != null;

            Coordinate coord = new Coordinate(node.getLongitude(), node.getLatitude());
            nodeMap.put(coord, junction);
            if (seqNum++ == 0)
            {
                coords = new ArrayList<Coordinate>();
                coords.add(coord);
            }
            else
            {
                if (junction == null)
                {
                    coords.add(coord);
                }
                else
                {
                    coords.add(coord);
                    createLinkPart(way.getId(), highwayType);
                    coords = new ArrayList<Coordinate>();
                    coords.add(coord);
                }
            }
        }
    }
    
    private void createLinkPart(long wayId, String highwayType)
    {
        if (coords.size() == 1)
        {
            log.error("skipping way {} with only one node", wayId);
            return;
        }

        if (coords.size() == 2 && coords.get(0).equals(coords.get(1)))
        {
            log.error("skipping way {} collapsed to a point at {}", wayId,
                    coords.get(0));
            return;
        }

        LineString line = factory.createLineString(new CoordinateListSequence(
                coords));

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
            NomJunction fromNode = findOrCreateJunction(fromCoord);
            NomJunction toNode = findOrCreateJunction(toCoord);
            assert fromNode != null;
            assert toNode != null;
            assert !fromNode.equals(toNode);

            NomLink link = new NomLink();
            link.setAttr(highwayTypeMap.get(highwayType));
            if (link.getAttr() == null)
            {
                log.error("unknown highway type {}", highwayType);
                link.setAttr(highwayTypeMap.get("unclassified"));
            }
            link.setLength(length);

            link.setFeatureType(NomFeatureType.LINE_ROAD.getValue());            
            link.setSourceId(wayId);
            link.setGeometry(part);
            link.getJunctions().add(fromNode);
            link.getJunctions().add(toNode);
            links.add(link);
        }
    }

    private NomJunction findOrCreateJunction(Coordinate coord)
    {
        NomJunction junction = nodeMap.get(coord);
        if (junction == null)
        {
            int x = (int) coord.x;
            int y = (int) coord.y;
            junction = new NomJunction(x, y);
            newJunctions.add(junction);
        }
        return junction;        
    }
   
    
}
