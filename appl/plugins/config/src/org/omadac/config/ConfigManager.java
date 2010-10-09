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
package org.omadac.config;

import java.util.UUID;

import org.omadac.config.jaxb.OmadacSettings;

/**
 * A ConfigManager returns the Omadac runtime configuration. In a multi-master scenario with 
 * n worker threads receiving jobs from m master nodes, the configuration is not globally unique.
 * A worker node may depend on some configuration for its local node and on configuration defined
 * by the master owning the job to be processed by the worker.
 * @author hwellmann
 *
 */
public interface ConfigManager
{
    /** Returns the configuration for the local node. */
    OmadacSettings getConfiguration();
    
    /** Returns the configuration for the (local or remote) master indicated by the given uuid. */
    OmadacSettings getConfiguration(UUID uuid);
}
