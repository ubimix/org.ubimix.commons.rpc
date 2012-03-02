/**
 * 
 */
package org.webreformatter.commons.rpc;

import junit.framework.TestCase;

import org.webreformatter.commons.events.EventManager;
import org.webreformatter.commons.events.IEventManager;
import org.webreformatter.commons.events.calls.CallListener;
import org.webreformatter.commons.json.JsonObject;
import org.webreformatter.commons.rpc.RpcCallsDispatcher.RpcMessenger;
import org.webreformatter.commons.rpc.SandboxNamespace.SayHello;

/**
 * @author kotelnikov
 */
public class RpcCallsDispatcherTest extends TestCase {

    protected IEventManager fClientEventManager = new EventManager();

    protected IEventManager fServerEventManager = new EventManager();

    /**
     * @param name
     */
    public RpcCallsDispatcherTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        RpcCallsDispatcher clientDispatcher = new RpcCallsDispatcher();
        RpcCallsDispatcher serverDispatcher = new RpcCallsDispatcher();
        final RpcMessenger[] clientConnector = { null };
        final RpcMessenger[] serverConnector = { null };
        clientConnector[0] = new RpcMessenger() {
            @Override
            protected void sendMessage(String msg) {
                serverConnector[0].onMessage(msg);
            }
        };
        serverConnector[0] = new RpcMessenger() {
            @Override
            protected void sendMessage(String msg) {
                clientConnector[0].onMessage(msg);
            }
        };
        clientDispatcher.init(fClientEventManager, clientConnector[0]);
        serverDispatcher.init(fServerEventManager, serverConnector[0]);
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception {
        fServerEventManager.addListener(
            SayHello.class,
            new CallListener<SayHello>() {
                @Override
                protected void handleRequest(SayHello event) {
                    JsonObject params = event.getParamsAsObject();
                    String name = params.getString("name");
                    JsonObject response = new JsonObject();
                    response.setValue("msg", "Hello, " + name + "!");
                    response.setValue("type", "REMOTE");
                    event.reply(response);
                }
            });

        JsonObject params = new JsonObject();
        params.setValue("name", "John Smith");
        SayHello event = new SayHello("123", params);
        final JsonObject[] result = { null };
        fClientEventManager.fireEvent(event, new CallListener<SayHello>() {
            @Override
            protected void handleResponse(SayHello event) {
                result[0] = event.getResultObject();
            }
        });

        assertFalse(event.hasErrors());
        JsonObject expectedResult = new JsonObject();
        expectedResult.setValue("msg", "Hello, John Smith!");
        expectedResult.setValue("type", "REMOTE");
        assertEquals(expectedResult, result[0]);
        assertEquals(result[0], event.getResultObject());

        // Check that the same calls could be handled locally.
        // If the event is handled locally it will be never serialized and sent
        // to the peer (to the fServerEventManager).
        CallListener<SayHello> localListener = new CallListener<SayHello>() {
            @Override
            protected void handleRequest(SayHello event) {
                JsonObject params = event.getParamsAsObject();
                String name = params.getString("name");
                JsonObject response = new JsonObject();
                response.setValue("msg", "Hello, " + name + "!");
                response.setValue("type", "LOCAL");
                event.reply(response);
            }
        };
        fClientEventManager.addListener(SayHello.class, localListener);
        event = new SayHello("345", params);
        fClientEventManager.fireEvent(event, new CallListener<SayHello>() {
            @Override
            protected void handleResponse(SayHello event) {
                result[0] = event.getResultObject();
            }
        });
        expectedResult.setValue("type", "LOCAL");
        assertEquals(expectedResult, result[0]);
        assertEquals(result[0], event.getResultObject());

        // Remove the local event handler and check that the event is sent
        // remotely.
        fClientEventManager.removeListener(SayHello.class, localListener);
        event = new SayHello("567", params);
        fClientEventManager.fireEvent(event, new CallListener<SayHello>() {
            @Override
            protected void handleResponse(SayHello event) {
                result[0] = event.getResultObject();
            }
        });
        expectedResult.setValue("type", "REMOTE");
        assertEquals(expectedResult, result[0]);
        assertEquals(result[0], event.getResultObject());

    }

    public void testCallObjects() {
        JsonObject params = new JsonObject();
        params.setValue("name", "John Smith");
        SayHello event = new SayHello("123", params);
        assertEquals("sandboxNamespace.sayHello", event.getMethod());
        assertEquals(params, event.getParams());
    }
}
