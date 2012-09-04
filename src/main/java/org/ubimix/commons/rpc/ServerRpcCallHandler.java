/**
 * 
 */
package org.ubimix.commons.rpc;

import org.ubimix.commons.events.IEventManager;
import org.ubimix.commons.events.calls.CallListener;
import org.ubimix.commons.json.rpc.IRpcCallHandler;
import org.ubimix.commons.json.rpc.RpcError;
import org.ubimix.commons.json.rpc.RpcRequest;
import org.ubimix.commons.json.rpc.RpcResponse;

/**
 * @author kotelnikov
 */
public class ServerRpcCallHandler implements IRpcCallHandler {

    private IRpcCallBuilder fCallBuilder;

    private IEventManager fEventManager;

    /**
     * 
     */
    public ServerRpcCallHandler(
        IEventManager eventManager,
        IRpcCallBuilder callBuilder) {
        fEventManager = eventManager;
        fCallBuilder = callBuilder;
    }

    /**
     * @see org.ubimix.commons.json.rpc.IRpcCallHandler#handle(org.ubimix.commons.json.rpc.RpcRequest,
     *      org.ubimix.commons.json.rpc.IRpcCallHandler.IRpcCallback)
     */
    public void handle(RpcRequest request, final IRpcCallback callback) {
        RpcError error = null;
        try {
            RpcCall call = fCallBuilder.newRpcCall(request);
            if (call != null) {
                fEventManager.fireEvent(call, new CallListener<RpcCall>() {
                    @Override
                    protected void handleResponse(RpcCall event) {
                        RpcResponse response = event.getResponse();
                        callback.finish(response);
                    }
                });
            } else {
                error = RpcCall.newMethodNotFoundError();
            }
        } catch (Throwable t) {
            error = RpcCall.getError(t);
        }
        if (error != null) {
            RpcResponse response = new RpcResponse().<RpcResponse> setId(
                request.getId()).setError(error);
            callback.finish(response);
        }
    }

}
