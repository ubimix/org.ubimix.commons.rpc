package org.webreformatter.commons.rpc;

import org.webreformatter.commons.events.EventListenerRegistry;
import org.webreformatter.commons.events.EventManager;
import org.webreformatter.commons.events.IEventListenerRegistry;
import org.webreformatter.commons.events.IEventManager;
import org.webreformatter.commons.json.rpc.IRpcCallHandler;
import org.webreformatter.commons.json.rpc.RpcRequest;
import org.webreformatter.commons.json.rpc.RpcResponse;

/**
 * This is an test utility class creating and sinking "client" and "server"-side
 * event managers. It serializes/deserializes RPC request and response objects
 * circulating between the client and server event managers.
 * 
 * @author kotelnikov
 */
public class RpcCallTestUtil {

    /**
     * A client-side event manager. It serializes all {@link RpcCall} events and
     * sends them to the "server". Responses from the server are deseriliazed
     * and dispatched to the callers.
     */
    protected IEventManager fClientEventManager;

    /**
     * The server-side event manager. It receives messages from the client,
     * deserializes them, creates new {@link RpcCall} instances and send them to
     * registered handlers. Responses and serialized and sent back to the
     * client.
     */
    protected IEventManager fServerEventManager;

    /**
     * This is a listener registry used by the server-side event manager.
     */
    protected IEventListenerRegistry fServerListenerRegistry = new EventListenerRegistry();

    /**
     * The main constructor of the class. It initializes all internal fields (it
     * sinks the client-server communication channel).
     */
    public RpcCallTestUtil() {
        /*
         * A call builder used to re-create {@link RpcCall} instances on the
         * server side.
         */
        RpcCallBuilder callBuilder = new RpcCallBuilder(fServerListenerRegistry);
        final ServereRpcCallHandler serverHandler = new ServereRpcCallHandler(
            fServerEventManager,
            callBuilder);
        fClientEventManager = newClientEventManager();
        /*
         * This sender serializes/deserializes requests and responses
         * circulating between the client and the server.
         */
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

        /*
         * This client event listener is responsible for dispatching of requests
         * to the server. It is used to sink client event managers returned by
         * the {@link #getClientEventManager()} method.
         */
        ClientRpcCallListener clientListener = new ClientRpcCallListener(
            messageSender);
        fClientEventManager.addListener(RpcCall.class, clientListener);
    }

    /**
     * Returns the client-side event manager. All {@link RpcCall} events fired
     * using this event manager are dispatched to the server and handled by the
     * server-sided event manager (see {@link #getServerEventManager()}).
     * Responses from the server are deserialized and returned to this event
     * manager.
     * 
     * @return the client-side event manager.
     */
    public IEventManager getClientEventManager() {
        return fClientEventManager;
    }

    /**
     * Returns the server-side event manager. All handlers for the
     * {@link RpcCall} events should be registered with this event manager. All
     * calls from the client are dispatched to this event manager. Responses are
     * returned back to the client event manager.
     * 
     * @return the server-side event manager
     */
    public IEventManager getServerEventManager() {
        return fServerEventManager;
    }

    /**
     * Returns a listener registry used by the server-side event manager.
     * 
     * @return the listener registry
     */
    public IEventListenerRegistry getServerListenerRegistry() {
        return fServerListenerRegistry;
    }

    /**
     * Returns a newly created client-side event managers.
     * 
     * @return a newly created client-side event managers
     */
    protected IEventManager newClientEventManager() {
        return new EventManager();
    }

    /**
     * Returns a newly created server-side instance of the {@link IEventManager}
     * ; it uses the specified event listener registry.
     * 
     * @param registry the registry for the event listeners used to create a
     *        server-side event manager
     * @return a newly created server-side instance of the {@link IEventManager}
     *         ; it uses the specified event listener registry.
     */
    protected IEventManager newServerEventManager(
        IEventListenerRegistry registry) {
        return new EventManager(registry);
    }

}