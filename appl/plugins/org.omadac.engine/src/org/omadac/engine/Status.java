package org.omadac.engine;

/**
 * Status of a target.
 * @author hwellmann
 *
 */
public enum Status
{
    /** The status is unknown. It may be stored in the database but is not loaded yet. */
    UNKNOWN,
    
    /** The target does not exist. */
    MISSING,
    
    /** The target is being created. */
    CREATING,
    
    /** The target is being updated. */
    UPDATING,
    
    /** 
     * The target has been created or updated, but the new status has not yet been
     * persisted.
     */
    COMPLETED,
    
    /**
     * The target is up to date.
     */
    UPTODATE,
    
    /**
     * The target is outdated, i.e. at least one of its prerequisites is not up to date.
     */
    OUTDATED,
    
    /**
     * The target is incomplete. Some of its subtargets were updated in a previous run of
     * the make engine, but the compilation was suspended or interrupted.
     */
    INCOMPLETE,
    
    /**
     * An update of this target was forced by the user.
     */
    FORCED,
    
    /**
     * An error has occurred while updating this target.
     */
    ERROR
}