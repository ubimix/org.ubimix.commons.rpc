/**
 * 
 */
package org.webreformatter.commons.rpc;

import org.webreformatter.commons.events.calls.CallEvent;
import org.webreformatter.commons.json.JsonObject;

/**
 * @author kotelnikov
 */
public abstract class RpcEvent extends CallEvent<JsonObject, JsonObject> {

    private boolean fCachedResponse;

    public RpcEvent() {
        super(new JsonObject());
    }

    /**
     * @param request
     */
    public RpcEvent(JsonObject request) {
        super(request);
    }

    public boolean isCachedResponse() {
        return fCachedResponse;
    }

    public void setCachedResponse(boolean cached) {
        fCachedResponse = cached;
    }

    public void setResponse(Throwable t) {
        onError(t);
        JsonObject error = new JsonObject();
        StringBuilder buf = new StringBuilder();
        StackTraceElement[] stackTrace = t.getStackTrace();
        for (StackTraceElement e : stackTrace) {
            buf.append("\n  ");
            buf.append(e.getClassName()
                + "#"
                + e.getMethodName()
                + " ("
                + e.getFileName()
                + ":"
                + e.getLineNumber()
                + ")");
            buf.append(e);
        }
        buf.append("\n");
        error
            .setValue("code", 505)
            .setValue("message", t.getMessage())
            .setValue("stackTrace", buf.toString());
        JsonObject json = new JsonObject();
        json.setValue("error", error);
        setResponse(json);
    }
}
