/*
 * GRIDGAIN - OPEN CLOUD PLATFORM.
 * COPYRIGHT (C) 2005-2008 GRIDGAIN SYSTEMS. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE AS PUBLISHED BY THE FREE SOFTWARE FOUNDATION; EITHER
 * VERSION 2.1 OF THE LICENSE, OR (AT YOUR OPTION) ANY LATER
 * VERSION.
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  SEE THE
 * GNU LESSER GENERAL PUBLIC LICENSE FOR MORE DETAILS.
 *
 * YOU SHOULD HAVE RECEIVED A COPY OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE ALONG WITH THIS LIBRARY; IF NOT, WRITE TO THE FREE
 * SOFTWARE FOUNDATION, INC., 51 FRANKLIN ST, FIFTH FLOOR, BOSTON, MA
 * 02110-1301 USA
 */

package org.gridgain.grid.spi.checkpoint.database;


import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean that provides general administrative and configuration information
 * about database checkpoint SPI.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides information about database checkpoint SPI.")
public interface GridDatabaseCheckpointSpiMBean extends GridSpiManagementMBean {
    /**
     * Gets number of retries in case of DB failure.
     *
     * @return Number of retries.
     */
    @GridMBeanDescription("Number of retries.")
    public int getNumberOfRetries();

    /**
     * Gets data source description.
     *
     * @return Description for data source.
     */
    @GridMBeanDescription("Description for data source.")
    public String getDataSourceInfo();

    /**
     * Gets checkpoint database user name.
     *
     * @return User name for checkpoint database.
     */
    @GridMBeanDescription("User name for checkpoint database.")
    public String getUser();

    /**
     * Gets checkpoint database password.
     *
     * @return Password for checkpoint database.
     */
    @GridMBeanDescription("Password for checkpoint database.")
    public String getPassword();

    /**
     * Gets checkpoint table name.
     *
     * @return Checkpoint table name.
     */
    @GridMBeanDescription("Checkpoint table name.")
    public String getCheckpointTableName();

    /**
     * Gets key field name for checkpoint table.
     *
     * @return Key field name for checkpoint table.
     */
    @GridMBeanDescription("Key field name for checkpoint table.")
    public String getKeyFieldName();

    /**
     * Gets key field type for checkpoint table.
     *
     * @return Key field type for checkpoint table.
     */
    @GridMBeanDescription("Key field type for checkpoint table.")
    public String getKeyFieldType();

    /**
     * Gets value field name for checkpoint table.
     *
     * @return Value field name for checkpoint table.
     */
    @GridMBeanDescription("Value field name for checkpoint table.")
    public String getValueFieldName();

    /**
     * Gets value field type for checkpoint table.
     *
     * @return Value field type for checkpoint table.
     */
    @GridMBeanDescription("Value field type for checkpoint table.")
    public String getValueFieldType();

    /**
     * Gets expiration date field name for checkpoint table.
     *
     * @return Create date field name for checkpoint table.
     */
    @GridMBeanDescription("Expiration date field name for checkpoint table.")
    public String getExpireDateFieldName();

    /**
     * Gets expiration date field type for checkpoint table.
     *
     * @return Expiration date field type for checkpoint table.
     */
    @GridMBeanDescription("Expiration date field type for checkpoint table.")
    public String getExpireDateFieldType();
}
