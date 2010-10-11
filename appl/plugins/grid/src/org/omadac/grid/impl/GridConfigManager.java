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
package org.omadac.grid.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.gridgain.grid.Grid;
import org.gridgain.grid.GridNode;
import org.omadac.config.LocalConfigManager;
import org.omadac.config.OmadacException;
import org.omadac.config.jaxb.OmadacSettings;

public class GridConfigManager extends LocalConfigManager implements Serializable
{
    private static final long serialVersionUID = 1L;

    public static final String KEY_OMADAC_MASTER_CONFIG = "omadac.master.config";
    
    private Grid grid;
    private Map<UUID, OmadacSettings> configMap;
    
    public GridConfigManager()
    {
        this.configMap = new HashMap<UUID, OmadacSettings>();
    }
    
    
    public void setGrid(Grid grid)
    {
        this.grid = grid;
    }
    
    @Override
    public OmadacSettings getConfiguration(UUID uuid)
    {
        if (uuid.equals(grid.getLocalNode().getId()))
        {
            return getConfiguration();
        }
        
        GridNode node = grid.getNode(uuid);
        if (node == null)
        {
            throw new OmadacException("node " + uuid + " not found");
        }
        OmadacSettings configuration = configMap.get(uuid);
        if (configuration == null)
        {
            configuration = (OmadacSettings) node.getAttribute(KEY_OMADAC_MASTER_CONFIG);
            configMap.put(uuid, configuration);
        }
        return configuration;
    }
}
