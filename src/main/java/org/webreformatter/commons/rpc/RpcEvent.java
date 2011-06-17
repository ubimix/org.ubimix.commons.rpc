/**
 * 
 */
package org.webreformatter.commons.rpc;

import org.webreformatter.commons.events.calls.CallEvent;
import org.webreformatter.commons.json.JsonObject;
import org.webreformatter.commons.json.JsonValue;
import org.webreformatter.commons.json.JsonValue.IJsonValueFactory;
import org.webreformatter.commons.json.rpc.RpcError;

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

    public RpcError getError() {
        RpcError error = null;
        JsonObject response = getResponse();
        if (response != null) {
            error = response.getValue("error", RpcError.FACTORY);
        }
        return error;
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonValue> T getRequestValue() {
        return (T) getRequest();
    }

    public <T extends JsonObject> T getResponseAs(IJsonValueFactory<T> factory) {
        JsonObject response = getResponse();
        if (response == null) {
            return null;
        }
        return factory.newValue(response.getJsonObject());
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonValue> T getResponseValue() {
        return (T) getResponse();
    }

    public boolean isCachedResponse() {
        return fCachedResponse;
    }

    public void setCachedResponse(boolean cached) {
        fCachedResponse = cached;
    }

    public void setError(RpcError error) {
        JsonObject json = new JsonObject();
        json.setValue("error", error);
        setResponse(json);
    }

    public void setError(Throwable t) {
        onError(t);
        RpcError error = new RpcError();
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
        setError(error);
    }
}
