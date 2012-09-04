/**
 * 
 */
package org.ubimix.commons.rpc;

import org.ubimix.commons.json.JsonValue;
import org.ubimix.commons.json.rpc.RpcRequest;

/**
 * @author kotelnikov
 */
public class SandboxNamespace {

    public static class SayHello extends RpcCall {

        /**
         * This constructor has to be implemented to automatically instantiate
         * this type of events.
         * 
         * @param request
         */
        public SayHello(RpcRequest request) {
            super(request);
        }

        public SayHello(String id, JsonValue params) {
            super(id, params);
        }

    }

    /**
     * 
     */
    public SandboxNamespace() {
    }

}
