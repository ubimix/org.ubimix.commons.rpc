/**
 * 
 */
package org.webreformatter.commons.rpc;

import org.webreformatter.commons.events.IEventManager;
import org.webreformatter.commons.events.calls.CallListener;
import org.webreformatter.commons.json.rpc.IRpcCallHandler;
import org.webreformatter.commons.json.rpc.RpcError;
import org.webreformatter.commons.json.rpc.RpcRequest;
import org.webreformatter.commons.json.rpc.RpcResponse;

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
     * @see org.webreformatter.commons.json.rpc.IRpcCallHandler#handle(org.webreformatter.commons.json.rpc.RpcRequest,
     *      org.webreformatter.commons.json.rpc.IRpcCallHandler.IRpcCallback)
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
