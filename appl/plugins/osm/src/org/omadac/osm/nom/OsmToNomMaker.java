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

import org.omadac.base.OmadacMaker;

public class OsmToNomMaker extends OmadacMaker
{

    @Override
    protected void defineDependencies()
    {
        addDependency("NomFeatureTypes", "NomSchema");
        addDependency("NomJunctions", "NomFeatureTypes");
        addDependency("NomLinks", "NomJunctions");
//        addDependency("NomMapFeatures", "NomLinks");
//        addDependency("NomMapFeatureClassifier", "NomMapFeatures");
//        addDependency("NomPointFeatures", "NomMapFeatureClassifier");
//        
//        addDependency("NomAdminRegion", "NomSchema");
//        
//        addDependency("NomRoadNames", 
//                      "NomAdminRegion", "NomPointFeatures");        
//
//        addDependency("NomNamedRoads", "NomRoadNames");
    }

}
