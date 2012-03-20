package org.webreformatter.commons.rpc;

import org.webreformatter.commons.events.EventListenerRegistry;
import org.webreformatter.commons.events.EventManager;
import org.webreformatter.commons.events.IEventListenerRegistry;
import org.webreformatter.commons.events.IEventManager;
import org.webreformatter.commons.json.rpc.IRpcCallHandler;
import org.webreformatter.commons.json.rpc.RpcRequest;
import org.webreformatter.commons.json.rpc.RpcResponse;

/**
 * @author kotelnikov
 */
public class RpcCallTestUtil {

    protected IEventManager fClientEventManager;

    protected IEventManager fServerEventManager;

    protected IEventListenerRegistry fServerListenerRegistry = new EventListenerRegistry();

    public RpcCallTestUtil() {
        fServerEventManager = newServerEventManager(fServerListenerRegistry);
        IRpcCallBuilder callBuilder = new RpcCallBuilder(
            fServerListenerRegistry);
        final ServereRpcCallHandler serverHandler = new ServereRpcCallHandler(
            fServerEventManager,
            callBuilder);

        fClientEventManager = newClientEventManager();
        IRpcCallHandler messageSender = new IRpcCallHandler() {
            public void handle(
                RpcRequest clientRequest,
                final IRpcCallback clientCallback) {
                String requestStr = clientRequest.toString();
                // TODO: Transfer this serialized message from the client to
                // the server
                RpcRequest serverRequest = RpcRequest.FACTORY
                    .newValue(requestStr);
                serverHandler.handle(serverRequest, new IRpcCallback() {
                    public void finish(RpcResponse serverResponse) {
                        String responseStr = serverResponse.toString();
                        // TODO: Transfer this response from the server to the
                        // client
                        RpcResponse clientResponse = RpcResponse.FACTORY
                            .newValue(responseStr);
                        clientCallback.finish(clientResponse);
                    }
                });
            }
        };
        ClientRpcCallListener clientListener = new ClientRpcCallListener(
            messageSender);
        fClientEventManager.addListener(RpcCall.class, clientListener);
    }

    public IEventManager getClientEventManager() {
        return fClientEventManager;
    }

    public IEventManager getServerEventManager() {
        return fServerEventManager;
    }

    public IEventListenerRegistry getServerListenerRegistry() {
        return fServerListenerRegistry;
    }

    protected IEventManager newClientEventManager() {
        return new EventManager();
    }

    protected IEventManager newServerEventManager(
        IEventListenerRegistry registry) {
        return new EventManager(registry);
    }

}