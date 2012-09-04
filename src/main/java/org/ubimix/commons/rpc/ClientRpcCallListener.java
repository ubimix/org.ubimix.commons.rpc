/**
 * 
 */
package org.ubimix.commons.rpc;

import org.ubimix.commons.events.calls.CallListener;
import org.ubimix.commons.json.rpc.IRpcCallHandler;
import org.ubimix.commons.json.rpc.IRpcCallHandler.IRpcCallback;
import org.ubimix.commons.json.rpc.RpcRequest;
import org.ubimix.commons.json.rpc.RpcResponse;

/**
 * @author kotelnikov
 */
public class ClientRpcCallListener extends CallListener<RpcCall> {

    private IRpcCallHandler fRpcCallHandler;

    /**
     * 
     */
    public ClientRpcCallListener(IRpcCallHandler handler) {
        fRpcCallHandler = handler;
    }

    @Override
    protected void handleRequest(final RpcCall event) {
        if (event.hasResponse()) {
            return;
        }
        RpcRequest request = event.getRequest();
        fRpcCallHandler.handle(request, new IRpcCallback() {
            public void finish(RpcResponse response) {
                event.reply(response);
            }
        });
    }

}
