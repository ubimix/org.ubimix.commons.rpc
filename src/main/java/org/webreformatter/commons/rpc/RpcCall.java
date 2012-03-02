package org.webreformatter.commons.rpc;

import org.webreformatter.commons.events.calls.CallEvent;
import org.webreformatter.commons.json.JsonArray;
import org.webreformatter.commons.json.JsonObject;
import org.webreformatter.commons.json.JsonValue;
import org.webreformatter.commons.json.rpc.RpcError;
import org.webreformatter.commons.json.rpc.RpcRequest;
import org.webreformatter.commons.json.rpc.RpcResponse;

/**
 * This is a common super-class for all remote (RPC) call events.
 * 
 * @author kotelnikov
 */
public abstract class RpcCall extends CallEvent<RpcRequest, RpcResponse> {

    /**
     * Creates and returns a new {@link RpcError} instance using the information
     * from the given exception
     * 
     * @param code the code of the error to set
     * @param t the exception used as a source of information for the resulting
     *        error object
     * @return an {@link RpcError} instance filled with the information from the
     *         given exception (or error)
     */
    public static RpcError getError(int code, Throwable t) {
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
            .setValue("code", code)
            .setValue("message", t.getMessage())
            .setValue("stackTrace", buf.toString());
        return error;
    }

    /**
     * Creates and returns a new {@link RpcError} instance using the information
     * from the given exception. This method sets the
     * {@link RpcError#ERROR_INTERNAL_ERROR} internal error code.
     * 
     * @param t the exception used as a source of information for the resulting
     *        error object
     * @return an {@link RpcError} instance filled with the information from the
     *         given exception (or error)
     */
    public static RpcError getError(Throwable t) {
        return getError(RpcError.ERROR_INTERNAL_ERROR, t);
    }

    /**
     * This method implements default algorithm of detection of RPC method names
     * by types of {@link RpcCall} objects.
     * 
     * @param type the type of an event to translate in a call method
     * @return the name of the RPC method
     */
    public static String getMethodName(Class<? extends RpcCall> type) {
        String name = type.getName();
        int idx = name.lastIndexOf(".");
        if (idx > 0) {
            name = name.substring(idx + 1);
        }
        StringBuilder buf = new StringBuilder();
        boolean begin = true;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '$') {
                buf.append('.');
                begin = true;
            } else {
                if (begin) {
                    ch = Character.toLowerCase(ch);
                }
                begin = false;
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    /**
     * Creates and returns a new "method not found" error ({@link RpcError}). It
     * sets the {@link RpcError#ERROR_METHOD_NOT_FOUND} error code.
     * 
     * @return a new "method not found" error
     */
    public static RpcError newMethodNotFoundError() {
        return new RpcError(
            RpcError.ERROR_METHOD_NOT_FOUND,
            "Method was not found.");
    }

    /**
     * This flag is used by the {@link RpcCallsDispatcher} to distinguish local
     * calls and requests received from the remote peer.
     */
    private boolean fRemote;

    /**
     * This constructor HAVE to be implemented in subclasses. It is used to
     * automatically instantiate call objects.
     * 
     * @param request the request object
     */
    public RpcCall(RpcRequest request) {
        super(request);
        RpcRequest req = getRequest();
        String methodName = req.getMethod();
        if (methodName == null) {
            Class<? extends RpcCall> type = getClass();
            methodName = getMethodName(type);
            req.setMethod(methodName);
        }
    }

    /**
     * Creates a new call request using the specified request identifier and
     * parameters.
     * 
     * @param id the unique identifier of this call
     * @param params parameters of the call
     */
    public RpcCall(String id, JsonValue params) {
        this(new RpcRequest().<RpcRequest> setId(id).setParams(params));
    }

    /**
     * Creates a new call request using the specified request identifier and
     * parameters.
     * 
     * @param id the unique identifier of the call
     * @param method the name of the remote method to call
     * @param params parameters of the call
     */
    public RpcCall(String id, String method, JsonValue params) {
        this(new RpcRequest()
            .<RpcRequest> setId(id)
            .<RpcRequest> setMethod(method)
            .setParams(params));
    }

    /**
     * Returns the name of the RPC method. This is a "shortcut" for the
     * {@link #getRequest()}.getMethod() call.
     * 
     * @return the name of the RPC method
     */
    public String getMethod() {
        RpcRequest request = getRequest();
        String method = request.getMethod();
        return method;
    }

    /**
     * Returns parameter object for this call. This is a "shortcut" for the
     * {@link #getRequest()}.getParams() call.
     * 
     * @return parameter object for this call
     */
    public JsonValue getParams() {
        RpcRequest request = getRequest();
        JsonValue params = request.getParams();
        return params;
    }

    /**
     * Returns an array of parameters for this call. This is a "shortcut" for
     * the {@link #getRequest()}.getParamsAsArray() call.
     * 
     * @return array of parameters for this call
     */
    public JsonArray getParamsAsArray() {
        RpcRequest request = getRequest();
        JsonArray params = request.getParamsAsArray();
        return params;

    }

    /**
     * Returns the parameter object for this call. This is a "shortcut" for the
     * {@link #getRequest()}.getParamsAsObject() call.
     * 
     * @return object with named parameters
     */
    public JsonObject getParamsAsObject() {
        RpcRequest request = getRequest();
        JsonObject params = request.getParamsAsObject();
        return params;
    }

    /**
     * Returns the resulting error object ({@link RpcError}) if any. This method
     * returns <code>null</code> if this call is not finished yet (if the
     * {@link CallEvent#reply(Object)} was not called yet).
     * 
     * @return the error object for this call
     */
    public RpcError getResultError() {
        RpcResponse response = getResponse();
        return response != null ? response.getError() : null;
    }

    /**
     * Returns the resulting object. This method returns <code>null</code> if
     * this call is not finished yet (if the {@link CallEvent#reply(Object)} was
     * not called yet).
     * 
     * @return the result of this call
     */
    public JsonObject getResultObject() {
        RpcResponse response = getResponse();
        return response != null ? response.getResultObject() : null;
    }

    /**
     * This method returns <code>true</code> if there are errors in the response
     * 
     * @return <code>true</code> if the response object contains errors.
     */
    public boolean hasResponseErrors() {
        RpcResponse response = getResponse();
        return response != null && response.hasErrors();
    }

    /**
     * Returns <code>true</code> if this call was created from a remote message.
     * This method is used internally by the {@link RpcCallsDispatcher} object
     * to distinguish incoming and outcoming calls.
     * 
     * @return <code>true</code> if this call was created from a remote message.
     */
    protected boolean isRemote() {
        return fRemote;
    }

    /**
     * Replies to this call with the specified resulting object. Note that this
     * method calls the {@link CallEvent#reply(Object)} method. So after this
     * call the response will be sent to the caller (locally or remotely).
     * 
     * @param result the result of this call.
     */
    public void reply(JsonValue result) {
        RpcRequest request = getRequest();
        super.reply(new RpcResponse()
            .<RpcResponse> setId(request.getId())
            .<RpcResponse> setResult(result));
    }

    /**
     * Replies to this call with the given error. Note that this method calls
     * the {@link CallEvent#reply(Object)} method. So after this call the
     * response will be sent to the caller (locally or remotely).
     * 
     * @param error the error used as an error.
     */
    public void setError(RpcError error) {
        JsonObject json = new JsonObject();
        json.setValue("error", error);
        reply(json);
    }

    /**
     * Replies to this call with the given error. This method builds a resulting
     * {@link RpcError} object using the {@link #getError(Throwable)} method and
     * after that it sends the result to the caller using the
     * {@link #setError(RpcError)} method. Note that this method calls the
     * {@link CallEvent#reply(Object)} method. So after this call the response
     * will be sent to the caller (locally or remotely).
     * 
     * @param error the error used as an error.
     */
    public void setError(Throwable t) {
        onError(t);
        RpcError error = getError(t);
        setError(error);
    }

    /**
     * Marks this call as a remote call. This method is used internally by the
     * {@link RpcCallsDispatcher} and it should not be called directly.
     * 
     * @param remote the flag to set
     */
    protected void setRemote(boolean remote) {
        fRemote = remote;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "{\n"
            + "\"request\": "
            + getRequest()
            + ",\n"
            + "\"response\": "
            + getResponse()
            + "\n}";
    }

}