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
/**
 * 
 */
package org.omadac.nom;


/**
 * Attributes of a road link.
 *
 * @author hwellmann
 * 
 */
public class RoadAttributes
{
    /**
     * Travel direction from node 0 to node 1. The opposite direction is prohibited.
     */
    public static final char FROM_ZERO_NODE = 'F';

    /**
     * Travel direction from node 1 to node 0. The opposite direction is prohibited.
     */
    public static final char TO_ZERO_NODE = 'T';
    
    /**
     * The link may be travelled in both directions.
     */
    public static final char BOTHWAYS = 'B';
    
    private long id;

    private boolean bridge;

    private boolean tunnel;

    private int functionalClass;

    private int speedClass;

    private int maxSpeed;

    private char travelDirection;

    private boolean paved;

    private boolean privateRoad;

    private boolean tollway;

    private boolean ferry;

    private boolean carTrain;

    private boolean multiLink;

    private boolean buses;

    private boolean cars;

    private boolean deliveries;

    private boolean emergency;
    
    private boolean pedestrians;

    private boolean taxis;

    private boolean trucks;

    private boolean throughTraffic;

    private boolean controlledAccess;
    
    private boolean parking;
    
    private boolean ramp;
    
    private boolean roundabout;
    
    private boolean service;

    private boolean square;

    public RoadAttributes()
    {
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public boolean getBridge()
    {
        return bridge;
    }

    public void setBridge(boolean bridge)
    {
        this.bridge = bridge;
    }

    public boolean getTunnel()
    {
        return tunnel;
    }

    public void setTunnel(boolean tunnel)
    {
        this.tunnel = tunnel;
    }

    public int getFunctionalClass()
    {
        return functionalClass;
    }

    public void setFunctionalClass(int functionalClass)
    {
        this.functionalClass = functionalClass;
    }

    public int getSpeedClass()
    {
        return speedClass;
    }

    public void setSpeedClass(int speedClass)
    {
        this.speedClass = speedClass;
    }

    public int getMaxSpeed()
    {
        return maxSpeed;
    }

    public void setMaxSpeed(int maxSpeed)
    {
        this.maxSpeed = maxSpeed;
    }

    public char getTravelDirection()
    {
        return travelDirection;
    }

    public void setTravelDirection(char travelDirection)
    {
        this.travelDirection = travelDirection;
    }

    public boolean getPaved()
    {
        return paved;
    }

    public void setPaved(boolean paved)
    {
        this.paved = paved;
    }

    public boolean getPrivateRoad()
    {
        return privateRoad;
    }

    public void setPrivateRoad(boolean privateRoad)
    {
        this.privateRoad = privateRoad;
    }

    public boolean getTollway()
    {
        return tollway;
    }

    public void setTollway(boolean tollway)
    {
        this.tollway = tollway;
    }

    public boolean getFerry()
    {
        return ferry;
    }

    public void setFerry(boolean ferry)
    {
        this.ferry = ferry;
    }

    public boolean getCarTrains()
    {
        return carTrain;
    }

    public void setCarTrain(boolean carTrain)
    {
        this.carTrain = carTrain;
    }

    public boolean getMultiLink()
    {
        return multiLink;
    }

    public void setMultiLink(boolean multiLink)
    {
        this.multiLink = multiLink;
    }

    public boolean getBuses()
    {
        return buses;
    }

    public void setBuses(boolean buses)
    {
        this.buses = buses;
    }

    public boolean getCars()
    {
        return cars;
    }

    public void setCars(boolean cars)
    {
        this.cars = cars;
    }

    public boolean getDeliveries()
    {
        return deliveries;
    }

    public void setDeliveries(boolean deliveries)
    {
        this.deliveries = deliveries;
    }

    public boolean getEmergency()
    {
        return emergency;
    }

    public void setEmergency(boolean emergency)
    {
        this.emergency = emergency;
    }

    public boolean getPedestrians()
    {
        return pedestrians;
    }

    public void setPedestrians(boolean pedestrians)
    {
        this.pedestrians = pedestrians;
    }

    public boolean getTaxis()
    {
        return taxis;
    }

    public void setTaxis(boolean taxis)
    {
        this.taxis = taxis;
    }

    public boolean getTrucks()
    {
        return trucks;
    }

    public void setTrucks(boolean trucks)
    {
        this.trucks = trucks;
    }

    public boolean getThroughTraffic()
    {
        return throughTraffic;
    }

    public void setThroughTraffic(boolean throughTraffic)
    {
        this.throughTraffic = throughTraffic;
    }

    public boolean getControlledAccess()
    {
        return controlledAccess;
    }

    public void setControlledAccess(boolean controlledAccess)
    {
        this.controlledAccess = controlledAccess;
    }

    public boolean getParking()
    {
        return parking;
    }

    public void setParking(boolean parking)
    {
        this.parking = parking;
    }

    public boolean getRamp()
    {
        return ramp;
    }

    public void setRamp(boolean ramp)
    {
        this.ramp = ramp;
    }

    public boolean getRoundabout()
    {
        return roundabout;
    }

    public void setRoundabout(boolean roundabout)
    {
        this.roundabout = roundabout;
    }

    public boolean getService()
    {
        return service;
    }

    public void setService(boolean service)
    {
        this.service = service;
    }

    public boolean getSquare()
    {
        return square;
    }

    public void setSquare(boolean square)
    {
        this.square = square;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RoadAttributes other = (RoadAttributes) obj;
        if (id != other.id)
            return false;
        return true;
    }
    
    public void reverse()
    {
        if (getTravelDirection() == FROM_ZERO_NODE)
        {
            setTravelDirection(TO_ZERO_NODE);
        }
        else if (getTravelDirection() == TO_ZERO_NODE)
        {
            setTravelDirection(FROM_ZERO_NODE);
        }
    }
}
