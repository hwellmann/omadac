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

package org.gridgain.grid;

import org.apache.commons.jexl.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Implementation of {@link GridNodeFilter} based on
 * <a href="http://commons.apache.org/jexl/">Apache JEXL</a> expression
 * language. The expression will be compiled only once, at the creation
 * time, and all invocation will simply plugin the passed-in node into
 * the compiled expression.
 * <p>
 * Example below shows how JEXL node filter can be used to get grid nodes.
 * <pre name="code" class="java">
 * ...
 * GridJexlNodeFilter filter = new GridJexlNodeFilter(
 *     "node.metrics.availableProcessors > 1 && " +
 *     "node.metrics.averageCpuLoad < 0.5 && " +
 *     "node.attributes['os.name'] == 'Windows XP'"
 * );
 *
 * Collection&lt;GridNode&gt; nodes = grid.getNodes(filter);
 * ...
 * </pre>
 * <p>
 * Together with {@link org.gridgain.grid.spi.topology.nodefilter.GridNodeFilterTopologySpi}
 * <tt>GridJexlNodeFilter</tt> allows for a fairly simple way to provide complex SLA-based
 * task topology specifications. For example, expression below shows how the SPI can be
 * configured with <tt>GridJexlNodeFilter</tt> to include all Windows XP nodes
 * with more than one processor or core and that are not loaded over 50%
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *       ...
 *       &lt;property name="topologySpi"&gt;
 *           &lt;bean class="org.gridgain.grid.spi.topology.nodefilter.GridNodeFilterTopologySpi"&gt;
 *               &lt;property name="filter"&gt;
 *                    &lt;bean class="org.gridgain.grid.GridJexlNodeFilter"&gt;
 *                        &lt;property name="expression"&gt;
 *                            &lt;value&gt;
 *                                &lt;![CDATA[node.metrics.availableProcessors > 1 &&
 *                                node.metrics.averageCpuLoad < 0.5 &&
 *                                node.attributes['os.name'] == 'Windows XP']]&gt;
 *                            &lt;/value&gt;
 *                        &lt;/property&gt;
 *                    &lt;/bean&gt;
 *                &lt;/property&gt;
 *           &lt;/bean&gt;
 *       &lt;/property&gt;
 *       ...
 * &lt;/bean&gt;
 * </pre>
 *
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJexlNodeFilter implements GridNodeFilter {
    /** Boolean filter expression. */
    private String expr = null;

    /** JEXL expression object. */
    private Expression exprObj = null;

    /**
     * Creates filter.
     */
    public GridJexlNodeFilter() {
        // No-op.
    }

    /**
     * Creates filter.
     *
     * @param expr JEXL boolean expression for filter.
     * @throws GridRuntimeException If JEXL boolean expression failed.
     */
    public GridJexlNodeFilter(String expr) throws GridRuntimeException {
        assert expr != null : "ASSERTION [line=99, file=src/java/org/gridgain/grid/GridJexlNodeFilter.java]";

        this.expr = expr;

        try {
            exprObj = ExpressionFactory.createExpression(expr);
        }
        catch (Exception e) {
            throw (GridRuntimeException)new GridRuntimeException("Failed to parse expression: " + expr, e).setData(107, "src/java/org/gridgain/grid/GridJexlNodeFilter.java");
        }
    }

    /**
     * Gets expression.
     *
     * @return Expression.
     */
    public String getExpression() {
        return expr;
    }

    /**
     * Sets expression.
     *
     * @param expr JEXL boolean expression.
     * @throws GridRuntimeException If JEXL boolean expression failed.
     */
    public void setExpression(String expr) throws GridRuntimeException {
        assert expr != null : "ASSERTION [line=127, file=src/java/org/gridgain/grid/GridJexlNodeFilter.java]";

        this.expr = expr;

        try {
            exprObj = ExpressionFactory.createExpression(expr);
        }
        catch (Exception e) {
            throw (GridRuntimeException)new GridRuntimeException("Failed to parse expression: " + expr, e).setData(135, "src/java/org/gridgain/grid/GridJexlNodeFilter.java");
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public boolean accept(GridNode node) throws GridRuntimeException {
        assert node != null : "ASSERTION [line=144, file=src/java/org/gridgain/grid/GridJexlNodeFilter.java]";
        assert exprObj != null : "ASSERTION [line=145, file=src/java/org/gridgain/grid/GridJexlNodeFilter.java]";

        JexlContext ctx = JexlHelper.createContext();

        try {
            ctx.getVars().put("node", node);

            Object obj = exprObj.evaluate(ctx);

            if (obj instanceof Boolean) {
                return (Boolean)obj;
            }
        }
        catch (Exception e) {
            throw (GridRuntimeException)new GridRuntimeException("Failed to evaluate expression: " + expr, e).setData(159, "src/java/org/gridgain/grid/GridJexlNodeFilter.java");
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJexlNodeFilter.class, this);
    }
}
