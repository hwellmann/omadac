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

import org.gridgain.grid.Grid;
import org.gridgain.grid.GridException;
import org.gridgain.grid.GridJobAdapter;
import org.gridgain.grid.GridTaskSession;
import org.gridgain.grid.resources.GridInstanceResource;
import org.gridgain.grid.resources.GridTaskSessionResource;
import org.omadac.base.ExecutionContextImpl;
import org.omadac.config.jaxb.OmadacSettings;
import org.omadac.make.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a Target as a GridJob for GridGain.
 * 
 * @author hwellmann
 *
 */
public class TargetGridJobAdapter extends GridJobAdapter<Target>
{
    private static Logger log = LoggerFactory.getLogger(TargetGridJobAdapter.class);
    private static final long serialVersionUID = 1L;
    
    private Target target;
    
    @GridInstanceResource
    private Grid grid;
    
    @GridTaskSessionResource
    private GridTaskSession taskSession;

    public TargetGridJobAdapter(Target target)
    {
        this.target = target;
    }

    @Override
    public Serializable execute() throws GridException
    {
        ExecutionContextImpl ec = OmadacGridNode.getExecutionContext();
        GridConfigManager cm = (GridConfigManager) ec.getConfigManager();
        assert cm != null;
        cm.setGrid(grid);
        
        OmadacSettings settings = cm.getConfiguration(taskSession.getTaskNodeId());
        
        log.debug("configuration = {}", settings);
        target.setExecutionContext(ec);
        target.getAction().run();
        return target.getName();
    }

}
