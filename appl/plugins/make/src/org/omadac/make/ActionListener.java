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
package org.omadac.make;

/**
 * Listener interface for Actions.
 * @author hwellmann
 *
 */
public interface ActionListener
{
    /**
     * Called when an action completes normally.
     * @param action  the action completed
     */
    void onCompleted(Action action);
    
    /**
     * Called when an action has an error.
     * @param action   the action with error
     */
    void onError(Action action);
}
