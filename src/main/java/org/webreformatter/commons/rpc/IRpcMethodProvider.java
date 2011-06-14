package org.webreformatter.commons.rpc;

/**
 * Instances of this type are used to translate event types to corresponding RPC
 * method names.
 * 
 * @author kotelnikov
 */
public interface IRpcMethodProvider {
    /**
     * This method translates types of events in corresponding RPC method names.
     * 
     * @param type the type of the event
     * @return an RPC method name corresponding to the specified event type
     */
    String getMethodName(Class<? extends RpcEvent> type);
}