/**
 * 
 */
package org.webreformatter.commons.rpc;

import org.webreformatter.commons.events.calls.CallListener;
import org.webreformatter.commons.json.rpc.IRpcCallHandler;
import org.webreformatter.commons.json.rpc.IRpcCallHandler.IRpcCallback;
import org.webreformatter.commons.json.rpc.RpcRequest;
import org.webreformatter.commons.json.rpc.RpcResponse;

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
