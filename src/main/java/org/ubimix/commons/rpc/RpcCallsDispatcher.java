package org.ubimix.commons.rpc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.ubimix.commons.events.IEventListener;
import org.ubimix.commons.events.IEventManager;
import org.ubimix.commons.events.calls.CallListener;
import org.ubimix.commons.json.JsonObject;
import org.ubimix.commons.json.rpc.RpcError;
import org.ubimix.commons.json.rpc.RpcObject;
import org.ubimix.commons.json.rpc.RpcRequest;
import org.ubimix.commons.json.rpc.RpcResponse;

/**
 * This class is used to send and implement remote RPC calls using
 * {@link IEventManager}. It dispatches calls to and from remote peers for
 * execution. If an event was not handled locally then it will be sent to the
 * remote peer.
 * <p>
 * Example of usage:
 * </p>
 * 
 * <pre>
 *  // An event manager executing real operations.
 *  IEventManager eventManager = new EventManager();
 *  
 *  // This block contains the initialization process 
 *  // for the communication layer  
 *  {  
 *    // This object is responsible for communication with the peer. 
 *    // It is used to send to and receive JSON objects from 
 *    // the remote peer. For each message received from outside the 
 *    // RpcMessenger#onMessage(String) method should be called.
 *    IRpcMessenger connector = new RpcMessenger() {
 *      @Override
 *      protected void sendMessage(String msg) {
 *        ... // Send this serialized object to the peer.
 *      }
 *    };
 *    // Create a new dispatcher. 
 *    RpcCallsDispatcher dispatcher = new RpcCallsDispatcher();
 *    // Initialize the dispatcher. It will register all required listeners
 *    // to provide transparent communication between peers.
 *    dispatcher.init(eventManager, connector);
 *  }
 *  
 *  // Now we can use the IEventManager as usually:
 *  // - add event listeners
 *  // - fire events
 *  // If there are local event listeners, then calls will be handled locally
 *  // Otherwise they will be serialized and sent to the peer to be handled 
 *  // there.
 *  
 *  public static class SayHello extends RpcCall {
 *  }
 *  // This listener will handle all *remote* calls for the "sayHello" method.
 *  eventManager.addListener(SayHello.class, new CallListener&lt;SayHello>() {
 *      public void handleRequest(SayHello event) {
 *          JsonObject result = new JsonObject();
 *          result.setValue("msg", "Hello!");
 *          event.reply(result);
 *      }
 *  });
 *  
 *  // Remote calls: 
 *  public static class SayRemoteHello extends RpcCall {
 *  }
 *  String requestId = "123";
 *  JsonObject params = new JsonObject();
 *  params.setValue("msg", "I am here!");
 *  SayRemoteHello event = new SayRemoteHello(requestId, params);
 *  eventManager.fireEvent(event, new CallListener&lt;SayRemoteHello>() {
 *      public void handleResponse(SayRemoteHello event) {
 *          // This method will be called when the peer reply something.
 *          System.out.println(event.getResultObject());
 *      }
 *  });
 *  
 * </pre>
 * 
 * @author kotelnikov
 */
public class RpcCallsDispatcher {

    /**
     * @author kotelnikov
     */
    public interface IRpcMessenger {

        public interface IMessageListener {
            void onMessage(RpcObject message);
        }

        void postMessage(JsonObject message);

        void setMessageListener(IMessageListener listener);
    }

    /**
     * @author kotelnikov
     */
    public static abstract class RpcMessenger implements IRpcMessenger {

        private IMessageListener fListener;

        /**
         * This method should be called when a new serialized JSON object is
         * received.
         * 
         * @param msg a serialized JSON message
         */
        public void onMessage(String msg) {
            JsonObject json = JsonObject.FACTORY.newValue(msg);
            RpcObject value = RpcObject.toRpcObject(json);
            fListener.onMessage(value);
        }

        /**
         * @see org.ubimix.commons.rpc.IMessageSender#postMessage(java.lang.Object)
         */
        public void postMessage(JsonObject message) {
            String str = message.toString();
            sendMessage(str);
        }

        /**
         * Sends a serialized JSON object.
         * 
         * @param msg a serialized JSON object to send
         */
        protected abstract void sendMessage(String msg);

        public void setMessageListener(IRpcMessenger.IMessageListener listener) {
            fListener = listener;
        }

    }

    /**
     * This map is used to keep all non-responded calls.
     */
    private Map<String, RpcCall> fCalls = new HashMap<String, RpcCall>();

    private Class<? extends RpcCall> fCallType;

    private IEventManager fEventManager;

    private String fIdBase = "id-" + (new Date().getTime()) + "-";

    private int fIdCounter = 0;

    private IRpcMessenger.IMessageListener fMessageListener = new IRpcMessenger.IMessageListener() {
        public void onMessage(RpcObject message) {
            RpcObject obj = RpcObject.toRpcObject(message);
            if (obj instanceof RpcResponse) {
                RpcResponse resp = (RpcResponse) obj;
                handleExternalResponses(resp);
            } else {
                RpcRequest request = (obj instanceof RpcRequest)
                    ? (RpcRequest) obj
                    : new RpcRequest().<RpcRequest> setId(newRequestId());
                handleExternalCall(request);
            }
        }
    };

    private IRpcCallBuilder fRpcCallBuilder;

    private IEventListener<RpcCall> fRpcCallListener = new CallListener<RpcCall>() {
        @Override
        protected void handleRequest(RpcCall event) {
            if (event.hasResponse()) {
                return;
            }
            if (event.isRemote()) {
                /*
                 * This is a remote call. If there is no reply for this call was
                 * provided then we need to generate an error response.
                 */
                event.reply(RpcCall.newMethodNotFoundError());
            } else {
                /*
                 * This is a local call. It should be serialized and sent to the
                 * peer.
                 */
                RpcRequest request = event.getRequest();
                String id = request.getIdAsString();
                if (id != null) {
                    saveEvent(id, event);
                }
                fRpcMessenger.postMessage(request);
            }
        }
    };

    private IRpcMessenger fRpcMessenger;

    public RpcCallsDispatcher() {
    }

    protected RpcCall createEvent(RpcRequest request) throws Exception {
        RpcCall call = fRpcCallBuilder.newRpcCall(request);
        return call;
    }

    public void done() {
        fEventManager.removeListener(fCallType, fRpcCallListener);
    }

    /**
     * Returns an event waiting for response corresponding to the specified
     * request identifier.
     * 
     * @param requestId an id of the request
     * @return an event waiting for the response
     */
    protected RpcCall getEvent(String requestId) {
        synchronized (fCalls) {
            return fCalls.remove(requestId);
        }
    }

    /**
     * This method handles external requests.
     * 
     * @param request the request to handle
     */
    private void handleExternalCall(RpcRequest request) {
        Object requestId = request.getId();
        try {
            RpcCall event = createEvent(request);
            if (event == null) {
                if (requestId != null) {
                    RpcError error = RpcCall.newMethodNotFoundError();
                    RpcResponse response = new RpcResponse()
                        .<RpcResponse> setId(requestId)
                        .setError(error);
                    fRpcMessenger.postMessage(response);
                }
            } else {
                event.setRemote(true);
                fEventManager.fireEvent(event, new CallListener<RpcCall>() {
                    @Override
                    protected void handleResponse(RpcCall event) {
                        RpcResponse response = event.getResponse();
                        fRpcMessenger.postMessage(response);
                    }
                });
            }
        } catch (Throwable t) {
            RpcError error = RpcCall.getError(t);
            RpcResponse response = new RpcResponse().<RpcResponse> setId(
                requestId).setError(error);
            fRpcMessenger.postMessage(response);
        }
    }

    /**
     * Handles an external response message.
     * 
     * @param resp the external response to dispatch
     */
    private void handleExternalResponses(RpcResponse resp) {
        String id = resp.getIdAsString();
        RpcCall event = getEvent(id);
        if (event != null) {
            event.reply(resp);
        }
    }

    public void init(
        Class<? extends RpcCall> callType,
        IRpcCallBuilder callBuilder,
        IEventManager manager,
        IRpcMessenger messenger) {
        fCallType = callType;
        fRpcCallBuilder = callBuilder;
        fEventManager = manager;
        fRpcMessenger = messenger;
        fEventManager.addListener(fCallType, fRpcCallListener);
        fRpcMessenger.setMessageListener(fMessageListener);
    }

    public void init(IEventManager manager, IRpcMessenger messenger) {
        RpcCallBuilder builder = new RpcCallBuilder(manager);
        init(RpcCall.class, builder, manager, messenger);
    }

    public synchronized String newRequestId() {
        return fIdBase + (fIdCounter++);
    }

    /**
     * Saves an event corresponding to the specified request id.
     * 
     * @param requestId the unique identifier of the call
     * @param event the event to save
     */
    protected void saveEvent(String requestId, RpcCall event) {
        synchronized (fCalls) {
            fCalls.put(requestId, event);
        }
    }

}