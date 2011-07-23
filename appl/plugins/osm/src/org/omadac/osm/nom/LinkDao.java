package org.omadac.osm.nom;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.omadac.make.util.NumberRange;
import org.omadac.nom.NomJunction;
import org.omadac.nom.NomLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

public class LinkDao 
{
    private static Logger log = LoggerFactory.getLogger(LinkDao.class);

    private EntityManager em;

    public void setEntityManager(EntityManager em)
    {
        this.em = em;
    }

    
    
    public void loadJunctions(LinkSubtarget subtarget, Map<Long, NomJunction> junctionMap, Map<Coordinate, NomJunction> nodeMap)
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
        for (NomJunction junction : junctions)
        {
            junctionMap.put(junction.getSourceId(), junction);
            Coordinate coord = new Coordinate(junction.getX(), junction.getY());
            nodeMap.put(coord, junction);            
        }
    }

    public List<Object[]> loadWays(LinkSubtarget subtarget)
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

    public void saveFeatures(Collection<NomJunction> newJunctions, Collection<NomLink> links)
    {
        em.clear();
        for (NomJunction junction : newJunctions)
        {
            em.persist(junction);
        }
//        em.flush();
        for (NomLink link : links)
        {
//            long refNodeId = link.getReferenceNode().getId();
//            assert refNodeId != 0;
//            NomJunction refNode = em.find(NomJunction.class, refNodeId);
//            assert refNode != null : link.toString();
//            
//            long nonRefNodeId = link.getNonReferenceNode().getId();
//            assert nonRefNodeId != 0;
//            NomJunction nonRefNode = em.find(NomJunction.class, nonRefNodeId);
//            assert nonRefNode != null : link.toString();
//            link.setReferenceNode(refNode);
//            link.setNonReferenceNode(nonRefNode);
        }
        for (NomLink link : links)
        {
            //long refNodeId = link.getReferenceNode().getId();
            log.debug("creating link {}", link);
            em.persist(link);
        }
        log.info("done");
    }
    
    //public NomJunction findNode(NomJunction )
}
