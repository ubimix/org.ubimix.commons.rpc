package org.webreformatter.commons.rpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.webreformatter.commons.events.EventListenerRegistry;
import org.webreformatter.commons.events.IEventListener;
import org.webreformatter.commons.events.IEventListenerRegistration;
import org.webreformatter.commons.events.IEventListenerRegistry;

/**
 * This event listener registry is used to translate RPC method names to
 * corresponding {@link RpcEvent} event types. It implements the
 * {@link IEventListenerRegistry} interface, so it should be used as a
 * normal event listener registry.
 * 
 * @author kotelnikov
 */
public class RpcEventListenerRegistry
    implements
    IEventListenerRegistry {

    private Map<String, Class<?>> fCallTypes = new HashMap<String, Class<?>>();

    private IRpcMethodProvider fMethodProvider;

    private IEventListenerRegistry fRegistry;

    public RpcEventListenerRegistry(
        IEventListenerRegistry registry,
        IRpcMethodProvider methodProvider) {
        fRegistry = registry;
        fMethodProvider = methodProvider;
    }

    public RpcEventListenerRegistry(IRpcMethodProvider methodProvider) {
        this(new EventListenerRegistry(), methodProvider);
    }

    public synchronized <E> IEventListenerRegistration addListener(
        final Class<E> eventType,
        final IEventListener<? super E> listener) {
        final IEventListenerRegistration registration = fRegistry
            .addListener(eventType, listener);
        IEventListenerRegistration result = registration;
        if (RpcEvent.class.isAssignableFrom(eventType)) {
            Class<? extends RpcEvent> type = cast(eventType);
            String methodName = fMethodProvider.getMethodName(type);
            if (methodName != null) {
                fCallTypes.put(methodName, eventType);
                result = new IEventListenerRegistration() {
                    public boolean unregister() {
                        return removeListener(eventType, listener);
                    }
                };
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected <E> Class<? extends RpcEvent> cast(final Class<E> eventType) {
        Class<? extends RpcEvent> type = (Class<? extends RpcEvent>) eventType;
        return type;
    }

    public Class<?> getEventType(String methodName) {
        return fCallTypes.get(methodName);
    }

    public <E> List<IEventListener<?>> getListeners(Class<E> eventType) {
        return fRegistry.getListeners(eventType);
    }

    public synchronized <E> boolean removeListener(
        Class<E> eventType,
        IEventListener<? super E> listener) {
        boolean result = fRegistry.removeListener(eventType, listener);
        if (result) {
            if (RpcEvent.class.isAssignableFrom(eventType)) {
                Class<? extends RpcEvent> type = cast(eventType);
                String methodName = fMethodProvider.getMethodName(type);
                if (methodName != null) {
                    fCallTypes.remove(methodName);
                }
            }
        }
        return result;
    }

}