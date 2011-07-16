package org.webreformatter.commons.rpc;

import java.util.Set;

import org.webreformatter.commons.events.EventListenerRegistry;
import org.webreformatter.commons.events.EventManager;
import org.webreformatter.commons.events.IEventManager;
import org.webreformatter.commons.events.calls.CallListener;
import org.webreformatter.commons.json.JsonObject;
import org.webreformatter.commons.json.rpc.IRpcCallHandler;
import org.webreformatter.commons.json.rpc.RpcError;
import org.webreformatter.commons.json.rpc.RpcRequest;
import org.webreformatter.commons.json.rpc.RpcResponse;

/**
 * This class translates RPC calls to {@link RpcEvent} events. It uses
 * {@link RpcEventListenerInterceptor} to translate method names to corresponding
 * event types. This class does the opposite operation with the
 * {@link RpcEventToCallTranslator}.
 * 
 * @author kotelnikov
 * @see RpcEventToCallTranslator
 */
public class RpcCallToEventTranslator implements IRpcCallHandler {

    private IEventManager fEventManager;

    private RpcEventListenerInterceptor fEventsRegistry;

    private RpcCallToEventTranslator(RpcEventListenerInterceptor methodRegistry) {
        fEventsRegistry = methodRegistry;
        EventListenerRegistry listenerRegistry = new EventListenerRegistry();
        listenerRegistry.addListenerInterceptor(methodRegistry);
        fEventManager = new EventManager(listenerRegistry);
    }

    public RpcCallToEventTranslator(
        RpcEventListenerInterceptor eventsRegistry,
        IEventManager eventManager) {
        fEventManager = eventManager;
        fEventsRegistry = eventsRegistry;
    }

    /**
     * This method creates and fires a new {@link RpcEvent} event each RPC call.
     * To translate method names to {@link RpcEvent} event types it uses an
     * internal {@link RpcEventListenerInterceptor} instance.
     * 
     * @see org.webreformatter.commons.json.rpc.IRpcCallHandler#handle(org.webreformatter.commons.json.rpc.RpcRequest,
     *      org.webreformatter.commons.json.rpc.IRpcCallHandler.IRpcCallback)
     */
    public void handle(RpcRequest request, final IRpcCallback callback) {
        String methodName = request.getMethod();
        Class<?> type = fEventsRegistry.getEventType(methodName);
        final RpcResponse rpcResponse = new RpcResponse()
            .setId(request.getId())
            .setVersion(request.getVersion());
        if (type == null) {
            rpcResponse.setError(
                RpcError.ERROR_METHOD_NOT_FOUND,
                "Method not found");
            callback.finish(rpcResponse);
        } else {
            try {
                RpcEvent event = (RpcEvent) type.newInstance();
                JsonObject params = request.getParamsAsObject();
                event.setRequest(params);
                fEventManager.fireEvent(event, new CallListener<RpcEvent>() {
                    @Override
                    protected void handleResponse(RpcEvent event) {
                        if (event.hasErrors()) {
                            Set<Throwable> errors = event.getErrors();
                            setErrors(
                                rpcResponse,
                                errors.toArray(new Throwable[errors.size()]));
                        } else {
                            JsonObject result = event.getResponse();
                            rpcResponse.setResult(result);
                        }
                        callback.finish(rpcResponse);
                    }
                });
            } catch (Throwable t) {
                setErrors(rpcResponse, t);
                callback.finish(rpcResponse);
            }
        }
    }

    private void setErrors(RpcResponse response, Throwable... errors) {
        RpcError error = new RpcError().setCode(RpcError.ERROR_INTERNAL_ERROR);
        error.setErrors(errors);
        response.setError(error);
    }
}