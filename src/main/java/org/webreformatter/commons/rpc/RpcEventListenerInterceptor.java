package org.webreformatter.commons.rpc;

import java.util.HashMap;
import java.util.Map;

import org.webreformatter.commons.events.IEventListener;
import org.webreformatter.commons.events.IEventListenerInterceptor;
import org.webreformatter.commons.events.IEventListenerRegistry;

/**
 * This event listener registry is used to translate RPC method names to
 * corresponding {@link RpcEvent} event types. It implements the
 * {@link IEventListenerRegistry} interface, so it should be used as a normal
 * event listener registry.
 * 
 * @author kotelnikov
 */
public class RpcEventListenerInterceptor implements IEventListenerInterceptor {

    private Map<String, Class<?>> fCallTypes = new HashMap<String, Class<?>>();

    private IRpcMethodProvider fMethodProvider;

    public RpcEventListenerInterceptor(IRpcMethodProvider methodProvider) {
        fMethodProvider = methodProvider;
    }

    @SuppressWarnings("unchecked")
    protected <E> Class<? extends RpcEvent> cast(final Class<E> eventType) {
        Class<? extends RpcEvent> type = (Class<? extends RpcEvent>) eventType;
        return type;
    }

    public Class<?> getEventType(String methodName) {
        return fCallTypes.get(methodName);
    }

    public void onAddListener(Class<?> eventType, IEventListener<?> listener) {
        if (RpcEvent.class.isAssignableFrom(eventType)) {
            Class<? extends RpcEvent> type = cast(eventType);
            String methodName = fMethodProvider.getMethodName(type);
            if (methodName != null) {
                fCallTypes.put(methodName, eventType);
            }
        }
    }

    public void onRemoveListener(Class<?> eventType, IEventListener<?> listener) {
        if (RpcEvent.class.isAssignableFrom(eventType)) {
            Class<? extends RpcEvent> type = cast(eventType);
            String methodName = fMethodProvider.getMethodName(type);
            if (methodName != null) {
                fCallTypes.remove(methodName);
            }
        }
    }

}