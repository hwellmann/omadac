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

package org.gridgain.grid.kernal.managers.deployment;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import static org.gridgain.grid.GridDiscoveryEventType.*;
import static org.gridgain.grid.kernal.GridTopic.*;

/**
 * Communication helper class. Provides request and response sending methods.
 * It uses communication manager as a way of sending and receiving requests.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridToStringExclude
class GridDeploymentCommunication {
    /** */
    private final GridLogger log;

    /** */
    private final GridManagerRegistry mgrReg;

    /** */
    private GridMessageListener peerListener = null;

    /**
     * Creates new instance of deployment communication.
     *
     * @param mgrReg Manager registry.
     * @param log Logger.
     */
    GridDeploymentCommunication(GridManagerRegistry mgrReg, GridLogger log) {
        assert log != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentCommunication.java]";

        this.mgrReg = mgrReg;
        this.log = log.getLogger(getClass());
    }

    /**
     * Starts deployment communication.
     */
    void start() {
        peerListener = new GridMessageListener() {
            /**
             * {@inheritDoc}
             */
            public void onMessage(UUID nodeId, Serializable msg) {
                assert nodeId != null : "ASSERTION [line=75, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentCommunication.java]";
                assert msg != null : "ASSERTION [line=76, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentCommunication.java]";

                GridDeploymentRequest req = (GridDeploymentRequest)msg;

                if (req.isUndeploy() == true) {
                    processUndeployRequest(nodeId, req);
                }
                else {
                    processResourceRequest(nodeId, req);
                }
            }

            /**
             *
             * @param nodeId Sender node ID.
             * @param req Undeploy request.
             */
            private void processUndeployRequest(UUID nodeId, GridDeploymentRequest req) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Received undeploy request [nodeId=" + nodeId + ", req=" + req + ']');
                }

                mgrReg.getDeploymentManager().undeployTask(nodeId, req.getResourceName());
            }

            /**
             * Handles classes/resources requests.
             *
             * @param nodeId Originating node id.
             * @param req Request.
             */
            private void processResourceRequest(UUID nodeId, GridDeploymentRequest req) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Received peer class/resources loading request [node=" + nodeId + ", req=" + req + ']');
                }

                String errMsg;

                GridDeploymentResponse res = new GridDeploymentResponse();

                ClassLoader ldr = mgrReg.getDeploymentManager().getClassLoader(req.getClassLoaderId());

                // Null class loader means unsuccess here.
                if (ldr != null) {
                    InputStream in = ldr.getResourceAsStream(req.getResourceName());

                    if (in == null) {
                        errMsg = "Requested resource not found: " + req.getResourceName();

                        // Java requests the same class with BeanInfo postfix during
                        // introspection automatically. Usually nobody uses this kind
                        // of classes. Thus we print it out with DEBUG level.
                        // Also we print it with DEBUG level because of the
                        // frameworks which ask some classes just in case - for
                        // example to identify whether certain framework is available.
                        // Remote node will throw an exception if needs.
                        if (log.isDebugEnabled() == true) {
                            log.debug(errMsg);
                        }

                        res.setSuccess(false);
                        res.setErrorMessage(errMsg);
                    }
                    else {
                        try {
                            GridByteArrayList bytes = new GridByteArrayList(1024);

                            bytes.readAll(in);

                            res.setSuccess(true);
                            res.setByteSource(bytes);
                        }
                        catch (IOException e) {
                            errMsg = "Failed to read resource due to IO failure: " + req.getResourceName();

                            log.error(errMsg, e);

                            res.setErrorMessage(errMsg);
                            res.setSuccess(false);
                        }
                        finally {
                            GridUtils.close(in, log);
                        }
                    }
                }
                else {
                    errMsg = "Failed to find local deployment for peer request: " + req;

                    log.warning(errMsg);

                    res.setSuccess(false);
                    res.setErrorMessage(errMsg);
                }

                sendResponse(nodeId, req.getResponseTopic(), res);
            }

            /**
             * @param nodeId Destination node ID.
             * @param topic Response topic.
             * @param res Response.
             */
            private void sendResponse(UUID nodeId, String topic, GridDeploymentResponse res) {
                GridNode node = mgrReg.getDiscoveryManager().getNode(nodeId);

                if (node != null) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Sending peer class loading response [node=" + node.getId() + ", res=" + res + ']');
                    }

                    try {
                        mgrReg.getCommunicationManager().sendMessage(node, topic, res,
                            GridCommunicationThreadPolicy.NEW_THREAD);
                    }
                    catch (GridException e) {
                        log.error("Failed to send response to node:"  + nodeId, e);
                    }
                }
                else {
                    log.error("Failed to send response (node does not exist): " + nodeId);
                }
            }
        };

        mgrReg.getCommunicationManager().addMessageListener(CLASSLOAD_TOPIC.topic(), peerListener);
    }

    /**
     * Stops deployment communication.
     */
    void stop() {
        mgrReg.getCommunicationManager().removeMessageListener(CLASSLOAD_TOPIC.topic(), peerListener);
    }

    /**
     *
     * @param rsrcName Resource to undeploy.
     * @throws GridException If request could not be sent.
     */
    void sendUndeployRequest(String rsrcName) throws GridException {
        GridDeploymentRequest req = new GridDeploymentRequest(null, rsrcName, true);

        Collection<GridNode> rmtNodes = mgrReg.getDiscoveryManager().getRemoteNodes();

        if (rmtNodes.isEmpty() == false) {
            mgrReg.getCommunicationManager().sendMessage(
                rmtNodes,
                CLASSLOAD_TOPIC.topic(),
                req,
                GridCommunicationThreadPolicy.NEW_THREAD
            );
        }
    }

    /**
     * Sends request to the remote node and wait for response. If there is
     * no response until endTime returns null.
     *
     * @param rsrcName Resource name.
     * @param clsLdrId Class loader ID.
     * @param dstNode Remote node request should be sent to.
     * @param endTime Time in milliseconds when request is decided to
     *      be obsolete.
     * @return Either response value or <tt>null</tt> if timeout occurred.
     * @throws GridException Thrown if there is no connection with remote node.
     */
    GridDeploymentResponse sendResourceRequest(String rsrcName, UUID clsLdrId, final GridNode dstNode, long endTime)
        throws GridException {
        assert rsrcName != null : "ASSERTION [line=244, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentCommunication.java]";
        assert dstNode != null : "ASSERTION [line=245, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentCommunication.java]";
        assert clsLdrId != null : "ASSERTION [line=246, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentCommunication.java]";

        String resTopic = CLASSLOAD_TOPIC.topic(UUID.randomUUID());

        GridDeploymentRequest req = new GridDeploymentRequest(clsLdrId, rsrcName, false);

        req.setResponseTopic(resTopic);

        final Object queryMux = new Object();

        final GridMutable<GridDeploymentResponse> res = new GridMutable<GridDeploymentResponse>();

        GridDiscoveryListener nodeListener = new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                if ((type == LEFT || type == FAILED) && node.getId().equals(dstNode.getId()) == true) {
                    GridDeploymentResponse fake = new GridDeploymentResponse();

                    String errMsg = "Originating task node left grid (resource will not be peer-loaded)"
                        + "[taskNodeId=" + dstNode.getId() + ']';

                    log.warning(errMsg);

                    fake.setSuccess(false);
                    fake.setErrorMessage(errMsg);

                    // We put fake result here to interrupt waiting peer-to-peer thread
                    // because originating node has left grid.
                    synchronized (queryMux) {
                        res.setValue(fake);

                        queryMux.notifyAll();
                    }
                }
            }
        };

        GridMessageListener resListener = new GridMessageListener() {
            /**
             * {@inheritDoc}
             */
            public void onMessage(UUID nodeId, Serializable msg) {
                assert nodeId != null : "ASSERTION [line=290, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentCommunication.java]";
                assert msg != null : "ASSERTION [line=291, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentCommunication.java]";

                synchronized (queryMux) {
                    if (msg instanceof GridDeploymentResponse == false) {
                        log.error("Received unknown peer class loading response [node=" + nodeId + ", msg=" + msg +
                            ']');
                    }
                    else {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Received peer loading response [node=" + nodeId + ", res=" + msg + ']');
                        }

                        res.setValue((GridDeploymentResponse)msg);
                    }

                    queryMux.notifyAll();
                }
            }
        };

        try {
            mgrReg.getCommunicationManager().addMessageListener(resTopic, resListener);

            // The destination node has potentially left grid here but in this case
            // Communication manager will throw the exception while sending message.
            mgrReg.getDiscoveryManager().addDiscoveryListener(nodeListener);

            if (log.isDebugEnabled() == true) {
                log.debug("Sending peer class loading request [node=" + dstNode.getId() + ", req=" + req + ']');
            }

            long start = System.currentTimeMillis();

            mgrReg.getCommunicationManager().sendMessage(dstNode, CLASSLOAD_TOPIC.topic(), req,
                GridCommunicationThreadPolicy.NEW_THREAD);

            synchronized (queryMux) {
                try {
                    long delta = endTime - start;

                    if (log.isDebugEnabled() == true) {
                        log.debug("Waiting for peer response from node [time=" + delta + "ms, node=" + dstNode.getId() +
                            ']');
                    }

                    while (res.getValue() == null && delta > 0) {
                        queryMux.wait(delta);

                        delta = endTime - System.currentTimeMillis();
                    }
                }
                catch (InterruptedException e) {
                    // Interrupt again to get it in the users code.
                    Thread.currentThread().interrupt();

                    throw (GridException)new GridException("Got interrupted while waiting for response from node: " + dstNode.getId(),
                        e).setData(346, "src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentCommunication.java");
                }
            }

            if (res.getValue() == null) {
                log.error("Failed to receive peer response from node within " + (System.currentTimeMillis() - start) +
                    " ms: " + dstNode.getId());
            }
            else if (log.isDebugEnabled() == true) {
                log.debug("Received peer response from node: " + dstNode.getId());
            }

            return res.getValue();
        }
        finally {
            mgrReg.getDiscoveryManager().removeDiscoveryListener(nodeListener);
            mgrReg.getCommunicationManager().removeMessageListener(resTopic, resListener);
        }
    }
}
