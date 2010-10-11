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
package org.omadac.nom;


public enum NomFeatureType
{ 
    UNDEFINED(0, 0),

    AREA_ORDER_0(3, 2),
    AREA_ORDER_1(4, 2),
    AREA_ORDER_2(5, 2),
    AREA_ORDER_3(5, 2),
    AREA_ORDER_4(5, 2),
    AREA_ORDER_5(5, 2),
    AREA_ORDER_6(6, 2),
    AREA_ORDER_7(6, 2),
    AREA_ORDER_8(6, 2),
    AREA_ORDER_9(6, 2),
    AREA_ORDER_10(6, 2),


    AREA_OCEAN(1, 2),  
    AREA_LAKE(10, 2),
    AREA_RIVER(8, 2),
    AREA_CANAL(9, 2),
    AREA_BAY(11, 2),

    AREA_BEACH(0, 2),
    AREA_ISLAND(2, 2),

    AREA_AIRPORT(19, 2),
    AREA_AIRPORT_RUNWAY(20, 2),
    AREA_AIRPORT_TAXIWAY(20, 2),
    AREA_AIRPORT_APRON(20, 2),
    AREA_TRAFFIC(31, 2),  

       
    AREA_ALLOTMENTS(0, 2),
    AREA_CLIFF(0, 2),
    AREA_HEATH(0, 2),
    AREA_MARSH(0, 2),
    AREA_VINEYARD(0, 2),
    AREA_SCRUB(12, 2),
    AREA_WOODLAND(12, 2),
    AREA_NATIONAL_PARK(13, 2),  
    AREA_MEADOW(16, 2),
    AREA_PARK(16, 2),
    AREA_PLAYGROUND(16, 2),
    AREA_GOLF_COURSE(23, 2),
    
    
    AREA_MILITARY_BASE(18, 2),

    AREA_FARMLAND(7, 2),
    AREA_BROWNFIELD(7, 2),
    AREA_GREENFIELD(7, 2),
    AREA_CONSTRUCTION(7, 2),
    AREA_BUILT_UP(7, 2),

    AREA_INDUSTRIAL(25, 2),
    AREA_RAILYARD(20, 2),
    AREA_SEAPORT(11, 2),
    AREA_POWER_STATION(11, 2),
    
    
    AREA_PEDESTRIAN_ZONE(30, 2),
    AREA_SHOPPING_CENTRE(24, 2),
    AREA_UNIVERSITY_COLLEGE(26, 2),
    AREA_HOSPITAL(27, 2),
    AREA_CEMETERY(28, 2),
    AREA_SPORTS_COMPLEX(29, 2),
    
    AREA_PARKING_LOT(21, 2),
    AREA_PARKING_GARAGE(22, 2),
    
    
    AREA_BUILDING(45, 2),
    AREA_BUSINESS_BUILDING(32, 2),
    AREA_CINEMA_BUILDING(32, 2),
    AREA_CONVENTION_EXHIBITION_BUILDING(33, 2),
    AREA_MUSEUM_BUILDING(34, 2),
    AREA_THEATRE_BUILDING(34, 2),
    AREA_CONCERT_HALL_BUILDING(34, 2),
    AREA_ARTS_CENTRE_BUILDING(34, 2),
    AREA_EDUCATION_BUILDING(35, 2),
    AREA_KINDERGARTEN_BUILDING(35, 2),
    AREA_EMERGENCY_SERVICE_BUILDING(36, 2),
    AREA_GOVERNMENT_BUILDING(37, 2),
    AREA_HISTORICAL_BUILDING(38, 2),
    AREA_MEDICAL_BUILDING(39, 2),
    AREA_PARK_LEISURE_BUILDING(40, 2),  
    AREA_RESIDENTIAL_BUILDING(41, 2),  
    AREA_RETAIL_BUILDING(42, 2),
    AREA_SPORTS_BUILDING(43, 2),
    AREA_RAILWAY_STATION_BUILDING(45, 2),
            
    AREA_MEMORIAL(2, 2),
    
    LINE_RAILWAY(46, 1),
    LINE_LIGHT_RAILWAY(46, 1),
    LINE_TRAM(46, 1),
    LINE_BUS(46, 1),
    LINE_CYCLE_TRACK(46, 1),
    LINE_CYCLE_LANE(46, 1),
    LINE_BORDER_ORDER_0(48, 1),  
    LINE_BORDER_ORDER_1(48, 1),  
    LINE_BORDER_ORDER_2(48, 1),  
    LINE_BORDER_ORDER_3(48, 1),  
    LINE_BORDER_ORDER_4(48, 1),  
    LINE_BORDER_ORDER_5(48, 1),  
    LINE_BORDER_ORDER_6(48, 1),  
    LINE_BORDER_ORDER_7(48, 1),  
    LINE_BORDER_ORDER_8(48, 1),  
    LINE_BORDER_ORDER_9(48, 1),  
    LINE_BORDER_ORDER_10(48, 1),  
    LINE_RIVER(52, 1),
    LINE_CANAL(53, 1),
    LINE_LAKE(54, 1),
    LINE_PIER(52, 1),
    LINE_ROAD(51, 1),
    LINE_ADDRESS_INTERPOLATION(1, 1),
    LINE_BARRIER(1, 1),
    LINE_DAM(1, 1),
    LINE_POWER(1, 1),


    POINT_JUNCTION(59, 0),
    
    POI_DENTIST(60, 0),
    POI_HAIR_AND_BEAUTY(60, 0),
    POI_HEALTH_CARE_SERVICE(60, 0),
    POI_HOSPITAL(60, 0),
    POI_MEDICAL_SERVICE(60, 0),
    POI_OPTICAL(60, 0),
    POI_PHARMACY(60, 0),
    POI_PHYSICIAN(60, 0),
    POI_RETIREMENT_NURSING_HOME(60, 0),
    POI_SOCIAL_SERVICE(60, 0),
    
    
    POI_AUTOMOBILE_CLUB(60, 0),
    POI_AUTO_DEALER(60, 0),
    POI_AUTO_DEALER_USED_CARS(60, 0),
    POI_AUTO_PARTS(60, 0),
    POI_AUTO_SERVICE(60, 0),
    POI_CAR_RENTAL(60, 0),
    POI_CAR_SHARING(60, 0),
    POI_CAR_WASH(60, 0),
    POI_MOTORCYCLE_DEALERSHIP(60, 0),
    POI_PARKING_GARAGE(60, 0),
    POI_PARKING_LOT(60, 0),
    POI_PARK_AND_RIDE(60, 0),
    POI_PETROL_STATION(60, 0),
    POI_REPAIR_SERVICE(60, 0),
    POI_TAXI(60, 0),
    POI_TRUCK_STOP(60, 0),
    
    POI_BICYCLE_DEALER(60, 0),
    POI_BICYCLE_RENTAL(60, 0),
    
    POI_AIRPORT(60, 0),
    POI_BUS_STATION(60, 0),
    POI_COMMUTER_RAIL_STATION(60, 0),
    POI_FERRY_TERMINAL(60, 0),
    POI_MARINA(60, 0),
    POI_PUBLIC_SPORTS_AIRPORT(60, 0),
    POI_RAIL_STATION(60, 0),

    POI_CHURCH(60, 0),
    POI_MOSQUE(60, 0),
    POI_PLACE_OF_WORSHIP(60, 0),
    POI_SYNAGOGUE(60, 0),

    POI_CITY_HALL(60, 0),
    POI_COMMUNITY_CENTRE(60, 0),
    POI_COUNTY_COUNCIL(60, 0),
    POI_COURT_HOUSE(60, 0),
    POI_EMBASSY(60, 0),
    POI_FIRE_DEPARTMENT(60, 0),
    POI_GOVERNMENT_OFFICES(60, 0),
    POI_POLICE_SERVICE(60, 0),
    POI_POLICE_STATION(60, 0),
    POI_POST_AGENCY(60, 0),
    POI_POST_BOX(60, 0),
    POI_POST_OFFICE(60, 0),
    POI_RECYCLING_BINS(60, 0),
    POI_RESIDENTIAL_AREA_BUILDING(60, 0),
    POI_YOUTH_CLUB(60, 0),
    
    POI_BAR_OR_PUB(60, 0),
    POI_CASINO(60, 0),
    POI_COCKTAIL_LOUNGE(60, 0),
    POI_COFFEE_SHOP(60, 0),  
    POI_FAST_FOOD(60, 0),
    POI_NIGHTLIFE(60, 0),
    POI_NIGHT_CLUB(60, 0),
    POI_RESTAURANT(60, 0),
    
    POI_CINEMA(60, 0),
    POI_PERFORMING_ARTS(60, 0),
    POI_THEATER(60, 0),

    POI_HIGHER_EDUCATION(60, 0),
    POI_KINDERGARTEN(60, 0),
    POI_SCHOOL(60, 0),
    
    POI_AMUSEMENT_PARK(60, 0),
    POI_CONVENTION_EXHIBITION_CENTRE(60, 0),
    POI_HISTORICAL_MONUMENT(60, 0),
    POI_LIBRARY(60, 0),
    POI_MUSEUM(60, 0),
    POI_TOURIST_ATTRACTION(60, 0),
    
    POI_BAKERY(60, 0),
    POI_CONVENIENCE_STORE(60, 0),
    POI_DEPARTMENT_STORE(60, 0),
    POI_DISCOUNT_STORE(60, 0),
    POI_GROCERY_STORE(60, 0),
    POI_SHOPPING(60, 0),
    
    POI_BOOKSTORE(60, 0),
    POI_CLEANING_AND_LAUNDRY(60, 0),
    POI_CLOTHING_STORE(60, 0),
    POI_COMPUTER_AND_SOFTWARE(60, 0),
    POI_CONSUMER_ELECTRONICS_STORE(60, 0),
    POI_FLOOR_AND_CARPET(60, 0),
    POI_FLOWERS(60, 0),
    POI_FURNITURE_STORE(60, 0),
    POI_GARDEN_CENTER(60, 0),
    POI_GIFT_ANTIQUE_AND_ART(60, 0),
    POI_GLASS_AND_WINDOW(60, 0),
    POI_HARDWARE_STORE(60, 0),
    POI_HOME_IMPROVEMENT_STORE(60, 0),
    POI_JEWELRY(60, 0),
    POI_MENS_APPAREL(60, 0),
    POI_MOVER(60, 0),
    POI_OFFICE_SUPPLY_AND_SERVICES_STORE(60, 0),
    POI_PAINT(60, 0),
    POI_PHOTOGRAPHY(60, 0),
    POI_RECORD_CD_AND_VIDEO(60, 0),
    POI_SHOE_STORE(60, 0),
    POI_SPORTING_GOODS_STORE(60, 0),
    POI_STORAGE(60, 0),
    POI_TAILOR_AND_ALTERATION(60, 0),
    POI_TELEPHONE_SERVICE(60, 0),
    POI_VIDEO_AND_GAME_RENTAL(60, 0),
    POI_WINERY(60, 0),
    POI_WINE_AND_LIQUOR(60, 0),
    POI_WOMENS_APPAREL(60, 0),

    POI_BOWLING_CENTRE(60, 0),
    POI_GOLF_COURSE(60, 0),
    POI_HEALTH_CLUB(60, 0),
    POI_ICE_SKATING_RINK(60, 0),
    POI_RACE_TRACK(60, 0),
    POI_RECREATION_CENTER(60, 0),
    POI_SPORTS_ACTIVITIES(60, 0),
    POI_SPORTS_COMPLEX(60, 0),

    POI_ATTORNEY(60, 0),
    POI_FUNERAL_DIRECTOR(60, 0),
    POI_VETERINARIAN_SERVICE(60, 0),

    POI_BUSINESS_FACILITY(60, 0),
    
    POI_ATM(60, 0),
    POI_BANK(60, 0),
    POI_CURRENCY_EXCHANGE(60, 0),
    POI_MONEY_TRANSFERRING_SERVICE(60, 0),

    POI_CAMPING(60, 0),
    POI_GUEST_HOUSE(60, 0),
    POI_HOTEL(60, 0),
    POI_REST_AREA(60, 0),
    POI_SKI_RESORT(60, 0),
    POI_TOILETS(60, 0),
    POI_TOURIST_INFORMATION(60, 0),

    POI_BENCH(60, 0),
    POI_FOUNTAIN(60, 0),
    POI_PARK_RECREATION_AREA(60, 0),

    POI_BORDER_CROSSING(60, 0),
    POI_CEMETERY(60, 0),
    POI_INDUSTRIAL_ZONE(60, 0),
    
    
    POI_CITY_CENTRE(60, 0),
    POI_HAMLET(60, 0);
    
    
    private int seqNum;
    
    private NomFeatureType(int seqNum, int dimension)
    {
        this.seqNum = seqNum;
    }
    
    public int getSeqNum()
    {
        return seqNum;
    }
    
    public int getValue()
    {
        return ordinal();
    }
    
    public int getDimension()
    {
        if (toString().startsWith("AREA_"))
            return 2; 
        else if (toString().startsWith("LINE_"))
            return 1;
        else
            return 0;
    }
    
    public String getDescription()
    {
        return toString();
    }
}
