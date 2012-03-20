package org.webreformatter.commons.rpc;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.webreformatter.commons.events.EventListenerInterceptor;
import org.webreformatter.commons.events.IEventListener;
import org.webreformatter.commons.events.IEventListenerInterceptor;
import org.webreformatter.commons.events.IEventListenerRegistry;
import org.webreformatter.commons.json.rpc.RpcRequest;

/**
 * @author kotelnikov
 */
public class RpcCallBuilder implements IRpcCallBuilder {

    private Map<String, Class<?>> fCallTypes = new HashMap<String, Class<?>>();

    private IEventListenerInterceptor fListenerInterceptor = new EventListenerInterceptor() {
        @SuppressWarnings("unchecked")
        private <E> Class<? extends RpcCall> castEventType(
            final Class<E> eventType) {
            Class<? extends RpcCall> type = (Class<? extends RpcCall>) eventType;
            return type;
        }

        /**
         * @see org.webreformatter.commons.events.IEventListenerInterceptor#onAddListener(java.lang.Class,
         *      org.webreformatter.commons.events.IEventListener)
         */
        @Override
        public void onAddListener(Class<?> eventType, IEventListener<?> listener) {
            if (RpcCall.class.isAssignableFrom(eventType)) {
                Class<? extends RpcCall> type = castEventType(eventType);
                String methodName = getMethodName(type);
                if (methodName != null) {
                    fCallTypes.put(methodName, eventType);
                }
            }
        }

        /**
         * @see org.webreformatter.commons.events.IEventListenerInterceptor#onRemoveListener(java.lang.Class,
         *      org.webreformatter.commons.events.IEventListener)
         */
        @Override
        public void onRemoveListener(
            Class<?> eventType,
            IEventListener<?> listener) {
            if (RpcCall.class.isAssignableFrom(eventType)) {
                Class<? extends RpcCall> type = castEventType(eventType);
                String methodName = getMethodName(type);
                if (methodName != null) {
                    fCallTypes.remove(methodName);
                }
            }
        }
    };

    public RpcCallBuilder(IEventListenerRegistry registry) {
        registry.addListenerInterceptor(fListenerInterceptor);
    }

    protected Class<?> getEventType(String methodName) {
        return fCallTypes.get(methodName);
    }

    /**
     * This method translates types of events in corresponding RPC method names.
     * It can be overloaded in subclasses.
     * 
     * @param type the type of the event
     * @return an RPC method name corresponding to the specified event type
     */
    protected String getMethodName(Class<? extends RpcCall> type) {
        return RpcCall.getMethodName(type);
    }

    /**
     * @see org.webreformatter.commons.rpc.IRpcCallBuilder#newRpcCall(org.webreformatter.commons.json.rpc.RpcRequest)
     */
    public RpcCall newRpcCall(RpcRequest request) throws Exception {
        RpcCall result = null;
        String methodName = request.getMethod();
        Class<?> type = getEventType(methodName);
        if (type != null) {
            try {
                Constructor<?> constructor = type
                    .getConstructor(RpcRequest.class);
                result = (RpcCall) constructor.newInstance(request);
            } catch (NoSuchMethodException e) {
            }
            if (result == null) {
                try {
                    Constructor<?> constructor = type.getConstructor();
                    result = (RpcCall) constructor.newInstance();
                    result.setRequest(request);
                } catch (NoSuchMethodException e) {
                }
            }
        }
        return result;
    }

}