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

import java.sql.*;
import java.text.*;
import javax.sql.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.checkpoint.*;
import org.gridgain.grid.util.*;

/**
 * This class defines database checkpoint SPI implementation. All checkpoints are
 * stored in the database table and available from all nodes in the grid. Note that every
 * node must have access to the database. The reason of having it is because a job state
 * can be saved on one node and loaded on another (e.g., if a job gets
 * preempted on a different node after node failure).
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has following mandatory configuration parameters.
 * <ul>
 * <li>DataSource (see {@link #setDataSource(DataSource)}).</li>
 * </ul>
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 * <li>Checkpoint table name (see {@link #setCheckpointTableName(String)}).</li>
 * <li>Checkpoint key field name (see {@link #setKeyFieldName(String)}). </li>
 * <li>Checkpoint key field type (see {@link #setKeyFieldType(String)}). </li>
 * <li>Checkpoint value field name (see {@link #setValueFieldName(String)}).</li>
 * <li>Checkpoint value field type (see {@link #setValueFieldType(String)}).</li>
 * <li>Checkpoint expiration date field name (see {@link #setExpireDateFieldName(String)}).</li>
 * <li>Checkpoint expiration date field type (see {@link #setExpireDateFieldType(String)}).</li>
 * <li>Number of retries in case of any failure (see {@link #setNumberOfRetries(int)}).</li>
 * <li>User name (see {@link #setUser(String)}).</li>
 * <li>Password (see {@link #setPassword(String)}).</li>
 * </ul>
 * <h2 class="header">Apache DBCP</h2>
 * <a href="http://commons.apache.org/dbcp/">Apache DBCP</a> project provides various wrappers
 * for data sources and connection pools. You can use these wrappers as Spring beans to configure
 * this SPI from Spring configuration file. Refer to <tt>Apache DBCP</tt> project for more information.
 * <p>
 * <h2 class="header">Java Example</h2>
 * <tt>GridDatabaseCheckpointSpi</tt> can be configured as follows:
 * <pre name="code" class="java">
 * GridDatabaseCheckpointSpi checkpointSpi = new GridDatabaseCheckpointSpi();
 *
 * javax.sql.DataSource ds = ... // Set datasource.
 *
 * // Set database checkpoint SPI parameters.
 * checkpointSpi.setDataSource(ds);
 * checkpointSpi.setUser("test");
 * checkpointSpi.setPassword("test");
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default checkpoint SPI.
 * cfg.setCheckpointSpi(checkpointSpi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 *
 * <h2 class="header">Spring Example</h2>
 * <tt>GridDatabaseCheckpointSpi</tt> can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *     ...
 *     &lt;property name="checkpointSpi"&gt;
 *         &lt;bean class="org.gridgain.grid.spi.checkpoint.database.GridDatabaseCheckpointSpi"&gt;
 *             &lt;property name="dataSource"&gt;&lt;ref bean="anyPoolledDataSourceBean" /&gt;&lt;/property&gt;
 *             &lt;property name="checkpointTableName" value="GRID_CHECKPOINTS" /&gt;
 *             &lt;property name="user" value="test" /&gt;
 *             &lt;property name="password" value="test" /&gt;
 *         &lt;/bean&gt;
 *     &lt;/property&gt;
 *     ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@SuppressWarnings({"JDBCResourceOpenedButNotSafelyClosed", "JDBCExecuteWithNonConstantString"})
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridDatabaseCheckpointSpi extends GridSpiAdapter
    implements GridCheckpointSpi, GridDatabaseCheckpointSpiMBean {
    /** Default number of retries in case of errors (value is <tt>2</tt>). */
    public static final int DFLT_NUMBER_OF_RETRIES = 2;

    /** Default expiration date field type (value is <tt>DATETIME</tt>). */
    public static final String DFLT_EXPIRE_DATE_FIELD_TYPE = "DATETIME";

    /** Default expiration date field name (value is <tt>EXPIRE_DATE</tt>). */
    public static final String DFLT_EXPIRE_DATE_FIELD_NAME = "EXPIRE_DATE";

    /** Default checkpoint value field type (value is <tt>BLOB</tt>). */
    public static final String DFLT_VALUE_FIELD_TYPE = "BLOB";

    /** Default checkpoint value field name (value is <tt>VALUE</tt>). */
    public static final String DFLT_VALUE_FIELD_NAME = "VALUE";

    /** Default checkpoint key field type (value is <tt>VARCHAR(256)</tt>). */
    public static final String DFLT_KEY_FIELD_TYPE = "VARCHAR(256)";

    /** Default checkpoint key field name (value is <tt>NAME</tt>). */
    public static final String DFLT_KEY_FIELD_NAME = "NAME";

    /** Default checkpoint table name (value is <tt>CHECKPOINTS</tt>). */
    public static final String DFLT_CHECKPOINT_TABLE_NAME = "CHECKPOINTS";

    /** Non-expirable timeout. */
    private static final long NON_EXPIRABLE_TIMEOUT = 0;

    /**
     * Template arguments:<br />
     * <tt>0</tt> - checkpoint table name;<br />
     * <tt>1</tt> - key field name;<br />
     * <tt>2</tt> - key field type;<br />
     * <tt>3</tt> - value field name;<br />
     * <tt>4</tt> - value field type;<br />
     * <tt>5</tt> - create date field name;<br />
     * <tt>6</tt> - create date field type.<br />
     */
    private static final String CREATE_CHECKPOINT_TABLE_SQL_TMPL = "CREATE TABLE {0} ({1} {2} PRIMARY KEY, {3} {4}" +
        ", {5} {6} NULL)";

    /** */
    private static final String CHECK_CHECKPOINT_TABLE_EXISTS_SQL_TMPL = "SELECT 0 FROM {0} WHERE 0 <> 0";

    /** */
    private static final String CHECK_CHECKPOINT_EXISTS_SQL_TMPL = "SELECT 0 FROM {0} WHERE {1} = ?";

    /** */
    private static final String UPDATE_CHECKPOINT_SQL_TMPL = "UPDATE {0} SET {1} = ?, {2} = ? WHERE {3} = ?";

    /** */
    private static final String INSERT_CHECKPOINT_SQL_TMPL = "INSERT INTO {0} ({1}, {2}, {3}) VALUES (?, ?, ?)";

    /** */
    private static final String DELETE_CHECKPOINT_SQL_TMPL = "DELETE FROM {0} WHERE {1} = ? AND ({2} IS NULL" +
        " OR {2} >= ?)";

    /** */
    private static final String SELECT_CHECKPOINT_SQL_TMPL = "SELECT {0} FROM {1} WHERE {2} = ? AND ({3} IS NULL" +
        " OR {3} >= ?)";

    /** */
    private static final String DELETE_EXPIRED_CHECKPOINTS_SQL_TMPL = "DELETE FROM {0} WHERE {1} IS NOT NULL" +
        " AND {1} > ?";

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    private DataSource dataSource = null;

    /** */
    private String user = null;

    /** */
    private String password = null;

    /** */
    private int retryNum = DFLT_NUMBER_OF_RETRIES;

    /** */
    private String cpTableName = DFLT_CHECKPOINT_TABLE_NAME;

    /** */
    private String keyFieldName = DFLT_KEY_FIELD_NAME;

    /** */
    private String keyFieldType = DFLT_KEY_FIELD_TYPE;

    /** */
    private String valFieldName = DFLT_VALUE_FIELD_NAME;

    /** */
    private String valFieldType = DFLT_VALUE_FIELD_TYPE;

    /** */
    private String expireDateFieldName = DFLT_EXPIRE_DATE_FIELD_NAME;

    /** */
    private String expireDateFieldType = DFLT_EXPIRE_DATE_FIELD_TYPE;

    /** */
    private String createCpTableSql = null;

    /** */
    private String checkCpTableExistsSql = null;

    /** */
    private String checkCpExistsSql = null;

    /** */
    private String updateCpSql = null;

    /** */
    private String insertCpSql = null;

    /** */
    private String deleteCpSql = null;

    /** */
    private String selectCpSql = null;

    /** */
    private String deleteExpiredCpsSql = null;

    /**
     * {@inheritDoc}
     */
    public int getNumberOfRetries() {
        return retryNum;
    }

    /**
     * {@inheritDoc}
     */
    public String getDataSourceInfo() {
        return dataSource.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getUser() {
        return user;
    }

    /**
     * {@inheritDoc}
     */
    public String getPassword() {
        return password;
    }

    /**
     * {@inheritDoc}
     */
    public String getCheckpointTableName() {
        return cpTableName;
    }

    /**
     * {@inheritDoc}
     */
    public String getKeyFieldName() {
        return keyFieldName;
    }

    /**
     * {@inheritDoc}
     */
    public String getKeyFieldType() {
        return keyFieldType;
    }

    /**
     * {@inheritDoc}
     */
    public String getValueFieldName() {
        return valFieldName;
    }

    /**
     * {@inheritDoc}
     */
    public String getValueFieldType() {
        return valFieldType;
    }

    /**
     * {@inheritDoc}
     */
    public String getExpireDateFieldName() {
        return expireDateFieldName;
    }

    /**
     * {@inheritDoc}
     */
    public String getExpireDateFieldType() {
        return expireDateFieldType;
    }

    /**
     * Sets DataSource to use for database access. This parameter is mandatory and must be
     * provided for this SPI to be able to start.
     * <p>
     * <a href="http://commons.apache.org/dbcp/">Apache DBCP</a> project provides various wrappers
     * for data sources and connection pools. You can use these wrappers as Spring beans to configure
     * this SPI from Spring configuration file. Refer to <tt>Apache DBCP</tt> project for more information.
     *
     * @param dataSource DataSource object to set.
     */
    @GridSpiConfiguration(optional = false)
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Sets number of retries in case of any database errors. By default
     * the value is {@link #DFLT_NUMBER_OF_RETRIES}.
     *
     * @param retryNum Number of retries in case of any database errors.
     */
    @GridSpiConfiguration(optional = true)
    public void setNumberOfRetries(int retryNum) {
        this.retryNum = retryNum;
    }

    /**
     * Sets checkpoint database user name. Note that authentication will be
     * performed only if both, <tt>user</tt> and <tt>password</tt> are set.
     *
     * @param user Checkpoint database user name to set.
     * @see #setPassword(String)
     */
    @GridSpiConfiguration(optional = true)
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Sets checkpoint database password. Note that authentication will be
     * performed only if both, <tt>user</tt> and <tt>password</tt> are set.
     *
     * @param password Checkpoint database password to set.
     * @see #setUser(String)
     */
    @GridSpiConfiguration(optional = true)
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets checkpoint table name. By default {@link #DFLT_CHECKPOINT_TABLE_NAME}
     * is used.
     *
     * @param cpTableName Checkpoint table name to set.
     */
    @GridSpiConfiguration(optional = true)
    public void setCheckpointTableName(String cpTableName) {
        this.cpTableName = cpTableName;
    }

    /**
     * Sets checkpoint key field name. By default,
     * {@link #DFLT_KEY_FIELD_NAME} is used. Note that you may also want to
     * change key field type (see {@link #setKeyFieldType(String)}).
     *
     * @param keyFieldName Checkpoint key field name to set.
     */
    @GridSpiConfiguration(optional = true)
    public void setKeyFieldName(String keyFieldName) {
        this.keyFieldName = keyFieldName;
    }

    /**
     * Sets checkpoint key field type. The field should have
     * corresponding SQL string type (<tt>VARCHAR</tt>, for example).
     * By default {@link #DFLT_EXPIRE_DATE_FIELD_TYPE} is used.
     *
     * @param keyFieldType Checkpoint key field type to set.
     */
    @GridSpiConfiguration(optional = true)
    public void setKeyFieldType(String keyFieldType) {
        this.keyFieldType = keyFieldType;
    }

    /**
     * Sets checkpoint value field name. By default {@link #DFLT_VALUE_FIELD_NAME}
     * is used. Note that you may also want to change the value type
     * (see {@link #setValueFieldType(String)}).
     *
     * @param valFieldName Checkpoint value field name to set.
     */
    @GridSpiConfiguration(optional = true)
    public void setValueFieldName(String valFieldName) {
        this.valFieldName = valFieldName;
    }

    /**
     * Sets checkpoint value field type. Note, that the field should have corresponding
     * SQL <tt>BLOB</tt> type, and the default value of {@link #DFLT_VALUE_FIELD_TYPE}, which is
     * <tt>BLOB</tt>, won't work for all databases. For example, if using <tt>HSQL DB</tt>,
     * then the type should be <tt>longvarbinary</tt>.
     *
     * @param valFieldType Checkpoint value field type to set.
     */
    @GridSpiConfiguration(optional = true)
    public void setValueFieldType(String valFieldType) {
        this.valFieldType = valFieldType;
    }

    /**
     * Sets checkpoint expiration date field name. By default
     * {@link #DFLT_EXPIRE_DATE_FIELD_NAME} is used. Note that you may also
     * want to change the expiration date field type
     * (see {@link #setExpireDateFieldType(String)}).
     *
     * @param expireDateFieldName Checkpoint expiration date field name to set.
     */
    @GridSpiConfiguration(optional = true)
    public void setExpireDateFieldName(String expireDateFieldName) {
        this.expireDateFieldName = expireDateFieldName;
    }

    /**
     * Sets checkpoint expiration date field type. By default
     * {@link #DFLT_EXPIRE_DATE_FIELD_TYPE} is used. The field should have
     * corresponding SQL <tt>DATETIME</tt> type.
     *
     * @param expireDateFieldType Checkpoint expiration date field type to set.
     */
    @GridSpiConfiguration(optional = true)
    public void setExpireDateFieldType(String expireDateFieldType) {
        this.expireDateFieldType = expireDateFieldType;
    }

    /**
     *{@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        assertParameter(dataSource != null, "dataSource != null");
        assertParameter(cpTableName.length() > 0, "checkpointTableName.length() > 0");
        assertParameter(keyFieldName.length() > 0, "keyFieldName.length() > 0");
        assertParameter(keyFieldType.length() > 0, "keyFieldType.length() > 0");
        assertParameter(valFieldName.length() > 0, "valueFieldName.length() > 0");
        assertParameter(valFieldType.length() > 0, "valueFieldType.length() > 0");
        assertParameter(expireDateFieldName.length() > 0, "expireDateFieldName.length() > 0");
        assertParameter(expireDateFieldType.length() > 0, "expireDateFieldType.length() > 0");

        // Fill SQL template strings.
        createCpTableSql = MessageFormat.format(CREATE_CHECKPOINT_TABLE_SQL_TMPL, cpTableName,
            keyFieldName, keyFieldType, valFieldName, valFieldType, expireDateFieldName, expireDateFieldType);

        checkCpTableExistsSql = MessageFormat.format(CHECK_CHECKPOINT_TABLE_EXISTS_SQL_TMPL,
            cpTableName);

        checkCpExistsSql = MessageFormat.format(CHECK_CHECKPOINT_EXISTS_SQL_TMPL, cpTableName,
            keyFieldName);

        updateCpSql = MessageFormat.format(UPDATE_CHECKPOINT_SQL_TMPL, cpTableName, valFieldName,
            expireDateFieldName, keyFieldName);

        insertCpSql = MessageFormat.format(INSERT_CHECKPOINT_SQL_TMPL, cpTableName, keyFieldName,
            valFieldName, expireDateFieldName);

        deleteCpSql = MessageFormat.format(DELETE_CHECKPOINT_SQL_TMPL, cpTableName, keyFieldName,
            expireDateFieldName);

        selectCpSql = MessageFormat.format(SELECT_CHECKPOINT_SQL_TMPL, valFieldName, cpTableName,
            keyFieldName, expireDateFieldName);

        deleteExpiredCpsSql = MessageFormat.format(DELETE_EXPIRED_CHECKPOINTS_SQL_TMPL, cpTableName,
            expireDateFieldName);

        Connection conn = null;

        try {
            conn = getConnection();

            // Check checkpoint table exists.
            int errCnt = 0;

            while (true) {
                try {
                    if (isCheckpointTableExists(conn) == false) {
                        createCheckpointTable(conn);
                    }

                    conn.commit();

                    break;
                }
                catch (SQLException e) {
                    GridUtils.rollbackConnection(conn, log);

                    if(++errCnt >= retryNum) {
                        throw (GridSpiException)new GridSpiException("Failed to create checkpoint table [checkpointTableName=" +
                            cpTableName + ']', e).setData(518, "src/java/org/gridgain/grid/spi/checkpoint/database/GridDatabaseCheckpointSpi.java");
                    }

                    if (log.isDebugEnabled() == true) {
                        log.debug("Failed to create checkpoint table as it may already exist (will try again) " +
                          "[checkpointTableName=" + cpTableName + ']');
                    }
                }
            }
        }
        catch (SQLException e) {
            throw (GridSpiException)new GridSpiException("Failed to start database checkpoint SPI [checkpointTableName=" +
                cpTableName + ']', e).setData(530, "src/java/org/gridgain/grid/spi/checkpoint/database/GridDatabaseCheckpointSpi.java");
        }
        finally {
            GridUtils.close(conn, log);
        }

        // Ack ok start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        if (dataSource == null) {
            // SPI was not started. Do nothing.
            return;
        }

        Connection conn = null;

        try {
            conn = getConnection();

            removeExpiredCheckpoints(conn);

            conn.commit();
        }
        catch (SQLException e) {
            GridUtils.rollbackConnection(conn, log);

            throw (GridSpiException)new GridSpiException("Failed to remove expired checkpoints [checkpointTableName=" +
                cpTableName + ']', e).setData(564, "src/java/org/gridgain/grid/spi/checkpoint/database/GridDatabaseCheckpointSpi.java");
        }
        finally {
            GridUtils.close(conn, log);
        }

        // Ack ok stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] loadCheckpoint(String key) throws GridSpiException {
        Connection conn = null;

        PreparedStatement st = null;

        ResultSet rs = null;

        try {
            conn = getConnection();

            st = conn.prepareStatement(selectCpSql);

            st.setString(1, key);

            st.setTime(2, new Time(System.currentTimeMillis()));

            rs = st.executeQuery();

            if (rs.next() == false) {
                return null;
            }

            byte[] res = rs.getBytes(1);

            return res;
        }
        catch (SQLException e) {
            throw (GridSpiException)new GridSpiException("Failed to load checkpoint [checkpointTableName=" + cpTableName +
                ", keyFieldName=" + keyFieldName + ", key=" + key + ']', e).setData(607, "src/java/org/gridgain/grid/spi/checkpoint/database/GridDatabaseCheckpointSpi.java");
        }
        finally {
            GridUtils.close(rs, log);
            GridUtils.close(st, log);
            GridUtils.close(conn, log);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeCheckpoint(String key) {
        boolean hasRemoved = false;

        Connection conn = null;

        PreparedStatement st = null;

        try {
            conn = getConnection();

            st = conn.prepareStatement(deleteCpSql);

            st.setString(1, key);

            st.setTime(2, new Time(System.currentTimeMillis()));

            if (st.executeUpdate() > 0) {
                hasRemoved = true;
            }

            conn.commit();
        }
        catch (SQLException e) {
            GridUtils.rollbackConnection(conn, log);

            log.error("Failed to remove checkpoint [checkpointTableName=" + cpTableName + ", keyFieldName=" +
                keyFieldName + ", key=" + key + ']', e);
        }
        finally {
            GridUtils.close(st, log);
            GridUtils.close(conn, log);
        }

        return hasRemoved;
    }

    /**
     * {@inheritDoc}
     */
    public void saveCheckpoint(String key, byte[] state, long timeout) throws GridSpiException {
        Time expiredTime = null;

        if (timeout != NON_EXPIRABLE_TIMEOUT) {
            expiredTime = new Time(System.currentTimeMillis() + timeout);
        }

        Connection conn = null;

        try {
            conn = getConnection();

            int errCnt = 0;

            while (true) {
                if (errCnt >= retryNum) {
                    throw (GridSpiException)new GridSpiException("Failed to save checkpoint after pre-configured number of " +
                        "retries [checkpointTableName=" + cpTableName + ", keyFieldName=" + keyFieldName +
                        ", key=" + key + ", retres=" + retryNum + ']').setData(675, "src/java/org/gridgain/grid/spi/checkpoint/database/GridDatabaseCheckpointSpi.java");
                }

                try {
                    if (isCheckpointExists(conn, key) == false) {
                        if (createCheckpoint(conn, key, state, expiredTime) == 0) {
                            ++errCnt;

                            if (log.isDebugEnabled()) {
                                log.debug("Failed to create checkpoint (will try again) [checkpointTableName=" +
                                    cpTableName + ", keyFieldName=" + keyFieldName + ", key=" + key + ']');
                            }

                            continue;
                        }
                    }
                    else {
                        if (updateCheckpoint(conn, key, state, expiredTime) == 0) {
                            ++errCnt;

                            if (log.isDebugEnabled()) {
                                log.debug("Failed to update checkpoint as it may be deleted (will try create) [" +
                                    "checkpointTableName=" + cpTableName + ", keyFieldName=" + keyFieldName +
                                    ", key=" + key + ']');
                            }

                            continue;
                        }
                    }

                    conn.commit();

                    break;
                }
                catch (SQLException e) {
                    GridUtils.rollbackConnection(conn, log);

                    if(++errCnt >= retryNum) {
                        throw (GridSpiException)new GridSpiException("Failed to save checkpoint [checkpointTableName=" +
                            cpTableName + ", keyFieldName=" + keyFieldName + ", key=" + key + ']', e).setData(715, "src/java/org/gridgain/grid/spi/checkpoint/database/GridDatabaseCheckpointSpi.java");
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Failed to save checkpoint (will try again) [checkpointTableName=" +
                            cpTableName + ", keyFieldName=" + keyFieldName + ", key=" + key + ']');
                    }
                }
            }
        }
        catch (SQLException e) {
            throw (GridSpiException)new GridSpiException("Failed to save checkpoint [checkpointTableName=" + cpTableName +
                ", keyFieldName=" + keyFieldName + ", key=" + key + ']', e).setData(727, "src/java/org/gridgain/grid/spi/checkpoint/database/GridDatabaseCheckpointSpi.java");
        }
        finally {
            GridUtils.close(conn, log);
        }
    }

    /**
     * Checks specified checkpoint existing.
     *
     * @param conn Active database connection.
     * @param key Checkpoint key.
     * @return <tt>true</tt> if specified checkpoint exists in the checkpoint table.
     * @throws SQLException Thrown in case of any errors.
     */
    private boolean isCheckpointExists(Connection conn, String key) throws SQLException {
        PreparedStatement st = null;

        ResultSet rs = null;

        try {
            st = conn.prepareStatement(checkCpExistsSql);

            st.setString(1, key);

            rs = st.executeQuery();

            return (rs.next() == true);
        }
        finally {
            GridUtils.close(rs, log);
            GridUtils.close(st, log);
        }
    }

    /**
     * Creates checkpoint.
     *
     * @param conn Active database connection.
     * @param key Checkpoint key.
     * @param state Checkpoint data.
     * @param expireTime FIXDOC
     * @return FIXDOC
     * @throws SQLException Thrown in case of any errors.
     */
    private int createCheckpoint(Connection conn, String key, byte[] state, Time expireTime) throws SQLException {
        PreparedStatement st = null;

        try {
            st = conn.prepareStatement(insertCpSql);

            st.setString(1, key);
            st.setBytes(2, state);
            st.setTime(3, expireTime);

            return st.executeUpdate();
        }
        finally {
            GridUtils.close(st, log);
        }
    }

    /**
     * Updates checkpoint data.
     *
     * @param conn Active database connection.
     * @param key Checkpoint key.
     * @param state Checkpoint data.
     * @param expiredTime FIXDOC
     * @return FIXDOC
     * @throws SQLException Thrown in case of any errors.
     */
    private int updateCheckpoint(Connection conn, String key, byte[] state, Time expiredTime) throws SQLException {
        PreparedStatement st = null;

        try {
            st = conn.prepareStatement(updateCpSql);

            st.setBytes(1, state);
            st.setTime(2, expiredTime);
            st.setString(3, key);

            return st.executeUpdate();
        }
        finally {
            GridUtils.close(st, log);
        }
    }

    /**
     * Get connection from the datasource.
     *
     * @return JDBC connection.
     * @throws SQLException Thrown in case of any errors.
     */
    private Connection getConnection() throws SQLException {
        Connection conn = null;

        if (user != null && password != null) {
            conn = dataSource.getConnection(user, password);
        }
        else {
            conn = dataSource.getConnection();
        }

        conn.setAutoCommit(false);

        return conn;
    }

    /**
     * This method accomplishes RDBMS-independent table exists check.
     *
     * @param conn Active database connection.
     * @return <tt>true</tt> if specified table exists, <tt>false</tt> otherwise.
     */
    private boolean isCheckpointTableExists(Connection conn) {
        Statement st = null;

        ResultSet rs = null;

        try {
            st = conn.createStatement();

            rs = st.executeQuery(checkCpTableExistsSql);

            return true; // if table does exist, no rows will ever be returned
        }
        catch (SQLException ignored) {
            return false; // if table does not exist, an exception will be thrown
        }
        finally {
            GridUtils.close(rs, log);
            GridUtils.close(st, log);
        }
    }

    /**
     * Creates checkpoint table.
     *
     * @param conn Active database connection.
     * @throws SQLException Thrown in case of any errors.
     */
    private void createCheckpointTable(Connection conn) throws SQLException {
        Statement st = null;

        try {
            st = conn.createStatement();

            st.executeUpdate(createCpTableSql);

            if (log.isInfoEnabled() == true) {
                log.info("Successfully created checkpoint table [checkpointTableName=" + cpTableName + ']');
            }
        }
        finally {
            GridUtils.close(st, log);
        }
    }

    /**
     * Removes expired checkpoints from the checkpoint table.
     *
     * @param conn Active database connection.
     * @return Number of removed expired checkpoints.
     * @throws SQLException Thrown in case of any errors.
     */
    private int removeExpiredCheckpoints(Connection conn) throws SQLException {
        int deletedRows = 0;

        PreparedStatement st = null;

        try {
            st = conn.prepareStatement(deleteExpiredCpsSql);

            st.setTime(1, new Time(System.currentTimeMillis()));

            deletedRows = st.executeUpdate();
        }
        finally {
            GridUtils.close(st, log);
        }

        if (log.isInfoEnabled() == true) {
            log.info("Successfully removed expired checkpoints [checkpointTableName=" + cpTableName + ']');
        }

        return deletedRows;
    }
}
