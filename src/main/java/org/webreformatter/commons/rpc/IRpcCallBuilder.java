package org.webreformatter.commons.rpc;

import org.webreformatter.commons.json.rpc.RpcRequest;

/**
 * Instances of this type are used to create new RpcCall objects using the
 * information defined in the request (like RPC method names).
 * 
 * @author kotelnikov
 */
public interface IRpcCallBuilder {

    /**
     * Creates and returns a {@link RpcCall} instance corresponding to the
     * given request object.
     * 
     * @param request the request object
     * @return a newly created {@link RpcCall} instance corresponding to the
     *         given request
     * @throws Exception
     */
    RpcCall newRpcCall(RpcRequest request) throws Exception;

}