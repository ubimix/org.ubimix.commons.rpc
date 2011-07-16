/**
 * 
 */
package org.webreformatter.commons.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.webreformatter.commons.events.EventListenerRegistry;
import org.webreformatter.commons.events.EventManager;
import org.webreformatter.commons.events.IEventManager;
import org.webreformatter.commons.events.calls.CallListener;
import org.webreformatter.commons.json.JsonObject;
import org.webreformatter.commons.json.JsonValue;
import org.webreformatter.commons.json.rpc.IRpcCallHandler;
import org.webreformatter.commons.json.rpc.IRpcCallHandler.IRpcCallback;
import org.webreformatter.commons.json.rpc.RpcRequest;
import org.webreformatter.commons.json.rpc.RpcResponse;

/**
 * @author kotelnikov
 */
public class RpcEventHandlerTest extends TestCase {

    public static interface ISocket {
        InputStream getInputStream();

        OutputStream getOutputStream();
    }

    public static class MyMethod extends RpcEvent {

    }

    public static class Pipe {

        private PipedInputStream fClientInput;

        private PipedOutputStream fClientOutput;

        private PipedInputStream fServerInput;

        private PipedOutputStream fServerOutput;

        public Pipe() throws IOException {
            fClientOutput = new PipedOutputStream();
            fServerOutput = new PipedOutputStream();
            fServerInput = new PipedInputStream(fClientOutput);
            fClientInput = new PipedInputStream(fServerOutput);
        }

        public ISocket getClientSocket() {
            return new ISocket() {

                public InputStream getInputStream() {
                    return fClientInput;
                }

                public OutputStream getOutputStream() {
                    return fClientOutput;
                }

            };
        }

        public ISocket getServerSocket() {
            return new ISocket() {
                public InputStream getInputStream() {
                    return fServerInput;
                }

                public OutputStream getOutputStream() {
                    return fServerOutput;
                }
            };

        }

    }

    /**
     * @param name
     */
    public RpcEventHandlerTest(String name) {
        super(name);
    }

    protected String getMessage(String firstName, String lastName) {
        return "Hello, " + firstName + " " + lastName + "!";
    }

    protected String readString(InputStream input) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024 * 10];
            int len;
            while ((len = input.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            String str = new String(out.toByteArray(), "UTF-8");
            return str;
        } finally {
            input.close();
        }
    }

    /**
     * 
     */
    public void testClientServer() {
        IRpcMethodProvider methodProvider = new DefaultRpcMethodProvider();
        IEventManager clientEventManager = new EventManager();
        IEventManager serverEventManager;
        {
            final EventListenerRegistry listenerRegistry = new EventListenerRegistry();
            serverEventManager = new EventManager(listenerRegistry);

            RpcEventListenerInterceptor rpcInterceptor = new RpcEventListenerInterceptor(
                methodProvider);
            listenerRegistry.addListenerInterceptor(rpcInterceptor);

            // Initialization of the "transport" layer

            // Server-side code. Translates RPC calls to events.
            RpcCallToEventTranslator callToEvent = new RpcCallToEventTranslator(
                rpcInterceptor,
                serverEventManager);

            // Client-side code. Translates events to RPC calls.
            RpcEventToCallTranslator eventToCall = new RpcEventToCallTranslator(
                callToEvent,
                methodProvider);
            clientEventManager.addListener(RpcEvent.class, eventToCall);
        }

        serverEventManager.addListener(
            MyMethod.class,
            new CallListener<MyMethod>() {
                @Override
                protected void handleRequest(MyMethod event) {
                    JsonObject request = event.getRequest();
                    JsonObject response = new JsonObject();
                    String message = getMessage(
                        request.getString("firstName"),
                        request.getString("lastName"));
                    response.setValue("message", message);
                    event.setResponse(response);
                }
            });

        String firstName = "John";
        String lastName = "Smith";
        final String[] testResult = { null };
        MyMethod event = new MyMethod();
        JsonObject params = new JsonObject();
        params.setValue("firstName", firstName);
        params.setValue("lastName", lastName);
        event.setRequest(params);
        clientEventManager.fireEvent(event, new CallListener<MyMethod>() {
            @Override
            protected void handleResponse(MyMethod event) {
                JsonObject response = event.getResponse();
                assertNotNull(response);
                testResult[0] = response.getString("message");
            }
        });
        String control = getMessage(firstName, lastName);
        assertEquals(control, testResult[0]);
    }

    public void testClientServerWithSerialization() throws Exception {
        final IRpcMethodProvider methodProvider = new DefaultRpcMethodProvider();
        final Pipe pipe = new Pipe();
        final String[] testResult = { null };
        final String firstName = "John";
        final String lastName = "Smith";

        Executor executor = Executors.newCachedThreadPool();

        // Server task
        FutureTask<Object> server = new FutureTask<Object>(
            new Callable<Object>() {
                public Object call() throws Exception {
                    RpcEventListenerInterceptor rpcListenerRegistry = new RpcEventListenerInterceptor(
                        methodProvider);
                    EventListenerRegistry listenerRegistry = new EventListenerRegistry();
                    listenerRegistry
                        .addListenerInterceptor(rpcListenerRegistry);
                    IEventManager eventManager = new EventManager(
                        listenerRegistry);

                    // Prepare the server-side stuff
                    eventManager.addListener(
                        MyMethod.class,
                        new CallListener<MyMethod>() {
                            @Override
                            protected void handleRequest(MyMethod event) {
                                JsonObject request = event.getRequest();
                                JsonObject response = new JsonObject();
                                String message = getMessage(
                                    request.getString("firstName"),
                                    request.getString("lastName"));
                                response.setValue("message", message);
                                event.setResponse(response);
                            }
                        });

                    // Really call the server-side code
                    {
                        final ISocket socket = pipe.getServerSocket();
                        InputStream input = socket.getInputStream();
                        String str = readString(input);
                        RpcRequest request = new RpcRequest(str);
                        RpcCallToEventTranslator serverHandler = new RpcCallToEventTranslator(
                            rpcListenerRegistry,
                            eventManager);
                        serverHandler.handle(request, new IRpcCallback() {
                            public void finish(RpcResponse response) {
                                JsonValue result = response.getResultAsJson();
                                OutputStream out = socket.getOutputStream();
                                try {
                                    writeString(result.toString(), out);
                                } catch (IOException e) {
                                    // Just ignore...
                                    e.printStackTrace();
                                }
                            }
                        });
                        return null;
                    }
                }
            });

        // Client task
        FutureTask<Object> client = new FutureTask<Object>(
            new Callable<Object>() {
                public Object call() throws Exception {
                    IEventManager clientEventManager = new EventManager();
                    // Client-side code responsible for sending serialized
                    // requests to the server.
                    {
                        IRpcCallHandler clientCallHandler = new IRpcCallHandler() {
                            public void handle(
                                RpcRequest request,
                                IRpcCallback callback) {
                                try {
                                    // Sends data
                                    ISocket socket = pipe.getClientSocket();
                                    OutputStream out = socket.getOutputStream();
                                    writeString(request.toString(), out);

                                    // Receive results
                                    InputStream input = socket.getInputStream();
                                    String str = readString(input);
                                    JsonObject result = JsonObject
                                        .newValue(str);
                                    RpcResponse response = new RpcResponse(
                                        request);
                                    response.setResult(result);
                                    callback.finish(response);
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        };
                        RpcEventToCallTranslator clientHandler = new RpcEventToCallTranslator(
                            clientCallHandler,
                            methodProvider);
                        clientEventManager.addListener(
                            RpcEvent.class,
                            clientHandler);
                    }

                    MyMethod event = new MyMethod();
                    JsonObject params = new JsonObject();
                    params.setValue("firstName", firstName);
                    params.setValue("lastName", lastName);
                    event.setRequest(params);
                    clientEventManager.fireEvent(
                        event,
                        new CallListener<MyMethod>() {
                            @Override
                            protected void handleResponse(MyMethod event) {
                                JsonObject response = event.getResponse();
                                assertNotNull(response);
                                testResult[0] = response.getString("message");
                            }
                        });
                    return null;
                }
            });

        // Launch asynchronously the client and the server
        executor.execute(client);
        executor.execute(server);

        // Wait while they finish their work
        TimeUnit units = TimeUnit.SECONDS;
        units = TimeUnit.MINUTES;
        client.get(5, units);
        server.get(5, units);

        // Check results
        String control = getMessage(firstName, lastName);
        assertEquals(control, testResult[0]);
    }

    public void testRpcEventListenerRegistry() {
        IRpcMethodProvider methodProvider = new DefaultRpcMethodProvider();

        RpcEventListenerInterceptor rpcListenerRegistry = new RpcEventListenerInterceptor(
            methodProvider);
        final EventListenerRegistry registry = new EventListenerRegistry();
        registry.addListenerInterceptor(rpcListenerRegistry);
        registry.addListener(MyMethod.class, new CallListener<MyMethod>() {
        });
        String methodName = methodProvider.getMethodName(MyMethod.class);
        Class<?> type = rpcListenerRegistry.getEventType(methodName);
        assertSame(MyMethod.class, type);
    }

    public void testRpcMethodProvider() {
        IRpcMethodProvider methodProvider = new DefaultRpcMethodProvider();
        class Abc {
            class Cde extends RpcEvent {

            }
        }
        String name = methodProvider.getMethodName(Abc.Cde.class);
        assertEquals("rpcEventHandlerTest.1Abc.cde", name);
    }

    /**
     * @throws Exception
     */
    public void testServer() throws Exception {
        final EventListenerRegistry registry = new EventListenerRegistry();
        IRpcMethodProvider methodProvider = new DefaultRpcMethodProvider();
        RpcEventListenerInterceptor rpcListenerRegistry = new RpcEventListenerInterceptor(
            methodProvider);
        IEventManager eventManager = new EventManager(registry);
        registry.addListenerInterceptor(rpcListenerRegistry);

        eventManager.addListener(MyMethod.class, new CallListener<MyMethod>() {
            @Override
            protected void handleRequest(MyMethod event) {
                JsonObject request = event.getRequest();
                JsonObject response = new JsonObject();
                String message = getMessage(
                    request.getString("firstName"),
                    request.getString("lastName"));
                response.setValue("message", message);
                event.setResponse(response);
            }
        });

        String firstName = "John";
        String lastName = "Smith";
        String methodName = methodProvider.getMethodName(MyMethod.class);
        RpcRequest request = new RpcRequest()
            .setMethod(methodName)
            .setId(123)
            .setParamObject("firstName", firstName, "lastName", lastName);

        final String[] testResult = { null };
        RpcCallToEventTranslator handlers = new RpcCallToEventTranslator(
            rpcListenerRegistry,
            eventManager);
        handlers.handle(request, new IRpcCallback() {
            public void finish(RpcResponse response) {
                JsonObject result = response.getResultObject();
                assertNotNull(result);
                testResult[0] = result.getString("message");
            }
        });
        String control = getMessage(firstName, lastName);
        assertEquals(control, testResult[0]);
    }

    protected void writeString(String str, OutputStream out) throws IOException {
        try {
            out.write(str.getBytes("UTF-8"));
            out.flush();
        } finally {
            out.close();
        }
    }

}
