/**
 * 
 */
package org.webreformatter.commons.rpc;

import org.webreformatter.commons.events.IEventManager;
import org.webreformatter.commons.events.calls.CallListener;
import org.webreformatter.commons.json.JsonObject;
import org.webreformatter.commons.json.rpc.IRpcCallHandler;
import org.webreformatter.commons.json.rpc.IRpcCallHandler.IRpcCallback;
import org.webreformatter.commons.json.rpc.RpcError;
import org.webreformatter.commons.json.rpc.RpcRequest;
import org.webreformatter.commons.json.rpc.RpcResponse;

/**
 * This class translates {@link RpcEvent} events to RPC calls. Internally it
 * uses a {@link IRpcCallHandler} object to perform RPC calls. Instances of this
 * type are used on the client side. They are registered as listeners in the
 * central {@link IEventManager} object. It allows transparently translate
 * client side event calls in RPC calls. This class does the opposite operation
 * with the {@link RpcCallToEventTranslator}.
 * 
 * @author kotelnikov
 * @see RpcCallToEventTranslator
 */

public class RpcEventToCallTranslator extends CallListener<RpcEvent> {

    private static String fBase = "call-" + System.currentTimeMillis() + "-";

    private int fCallCounter;

    private IRpcCallHandler fHandler;

    private IRpcMethodProvider fMethodProvider;

    /**
     * 
     */
    public RpcEventToCallTranslator(
        IRpcCallHandler handler,
        IRpcMethodProvider methodProvider) {
        fHandler = handler;
        fMethodProvider = methodProvider;
    }

    private Throwable getException(RpcError error) {
        return new RuntimeException("Error "
            + error.getCode()
            + ". "
            + error.getMessage());
    }

    @Override
    protected void handleRequest(final RpcEvent event) {
        JsonObject params = event.getRequest();
        Class<? extends RpcEvent> type = event.getClass();
        String methodName = fMethodProvider.getMethodName(type);
        RpcRequest request = new RpcRequest()
            .setId(newCallId())
            .setMethod(methodName)
            .setParams(params);
        fHandler.handle(request, new IRpcCallback() {
            public void finish(RpcResponse response) {
                RpcError error = response.getError();
                if (error != null) {
                    Throwable t = getException(error);
                    event.setResponse(t);
                } else {
                    JsonObject result = response.getResultObject();
                    event.setResponse(result);
                }
            }

        });
    }

    protected String newCallId() {
        return fBase + fCallCounter++;
    }

    public void registerWith(IEventManager eventManager) {
        eventManager.addListener(RpcEvent.class, this);
    }
}
