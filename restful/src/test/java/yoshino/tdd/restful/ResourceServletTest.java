package yoshino.tdd.restful;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author xiaoyi
 * 2023/1/14 13:29
 * @since
 **/
public class ResourceServletTest extends ServletTest {

    private Runtime runtime;
    private ResourceRouter router;
    private ResourceContext resourceContext;
    private Providers providers;
    private RuntimeDelegate runtimeDelegate;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);

        when(runtime.createResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getProviders()).thenReturn(providers);
        when(providers.getExceptionMapper(eq(NullPointerException.class)))
            .thenReturn(exception -> response().status(Response.Status.INTERNAL_SERVER_ERROR).entity(null, new Annotation[0]).build());

        return new ResourceServlet(runtime);
    }

    @BeforeEach
    public void before() {
        runtimeDelegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(runtimeDelegate);

        when(runtimeDelegate.createHeaderDelegate(NewCookie.class)).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            @Override
            public NewCookie fromString(String value) {
                return null;
            }

            @Override
            public String toString(NewCookie value) {
                return value.getName() + "=" + value.getValue();
            }
        });

    }

    @Test
    public void should_use_status_code_from_response() {
        response().status(Response.Status.NOT_MODIFIED).returnFrom(router);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_use_http_headers_from_response() {
        response().headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(),
            new NewCookie.Builder("USER_ID").value("user").build()).returnFrom(router);

        HttpResponse<String> httpResponse = get("test");

        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    @Test
    public void should_write_entity_to_http_body_using_message_body_writer() {
        response().entity(new GenericEntity<>("entity", String.class), new Annotation[0]).mediaType(MediaType.TEXT_PLAIN_TYPE).returnFrom(router);

        HttpResponse<String> httpResponse = get("test");

        assertEquals("entity", httpResponse.body());
    }

    @Test
    public void should_use_response_from_web_application_exception() {
        response().status(Response.Status.FORBIDDEN).throwFrom(router);

        HttpResponse<String> httpResponse = get("test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_build_response_by_exception_mapper_if_not_web_application_exception() {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);

        when(providers.getExceptionMapper(RuntimeException.class)).thenReturn(exception -> response().status(Response.Status.FORBIDDEN).build());

        HttpResponse<String> httpResponse = get("test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_not_call_message_body_writer_if_entity_is_null() {
        response().entity(null, new Annotation[0]).mediaType(MediaType.TEXT_PLAIN_TYPE).returnFrom(router);

        HttpResponse<String> httpResponse = get("test");

        assertEquals(Response.Status.OK.getStatusCode(), httpResponse.statusCode());
        assertEquals("", httpResponse.body());
    }

    @Test
    public void should_return_internal_error_if_message_body_writer_not_found() {
        response().returnFrom(router);

        when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE))).thenReturn(null);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_return_internal_error_if_exception_mapper_not_found() {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);

        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(null);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_return_internal_error_if_header_delegate_not_found() {
        response().headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(),
            new NewCookie.Builder("USER_ID").value("user").build()).returnFrom(router);

        when(runtimeDelegate.createHeaderDelegate(eq(NewCookie.class))).thenReturn(null);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_use_response_from_web_application_exception_thrown_by_providers_getExceptionMapper() {
        WebApplicationException exception = new WebApplicationException(response().status(Response.Status.FORBIDDEN).build());

        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);

        when(providers.getExceptionMapper(RuntimeException.class)).thenThrow(exception);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_handle_exception_thrown_by_providers_getExceptionMapper() {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(IllegalArgumentException.class);

        when(providers.getExceptionMapper(IllegalArgumentException.class)).thenThrow(RuntimeException.class);

        when(providers.getExceptionMapper(RuntimeException.class)).thenReturn(exception -> response().status(Response.Status.FORBIDDEN).entity(null, new Annotation[0]).build());


        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_use_response_from_web_application_thrown_by_exceptionMapper_toResponse() {
        webApplicationExceptionThrowFrom(this::exceptionMapper_toResponse);
    }

    @Test
    public void should_handle_exception_thrown_by_exceptionMapper_toResponse() {
        otherExceptionsThrowFrom(this::exceptionMapper_toResponse);
    }

    @Test
    public void should_use_response_from_web_application_exception_thrown_by_by_runtimeDelegate_createHeaderDelegate() {
        webApplicationExceptionThrowFrom(this::runtimeDelegate_createHeaderDelegate);
    }

    @Test
    public void should_handle_exception_thrown_by_runtimeDelegate_createHeaderDelegate() {
        otherExceptionsThrowFrom(this::runtimeDelegate_createHeaderDelegate);
    }

    @Test
    public void should_use_response_from_web_application_exception_thrown_by_headerDelegate_toString() {
        webApplicationExceptionThrowFrom(this::headerDelegate_toString);
    }

    @Test
    public void should_handle_exception_thrown_by_headerDelegate_toString() {
        otherExceptionsThrowFrom(this::headerDelegate_toString);
    }

    @Test
    public void web_application_exception_thrown_from_providers_getMessageBodyWriter() {
        webApplicationExceptionThrowFrom(this::providers_getMessageBodyWriter);
    }

    @Test
    public void web_application_exception_thrown_from_messageBodyWriter_writeTo() {
        webApplicationExceptionThrowFrom(this::messageBodyWriter_writeTo);
    }

    @Test
    public void other_exception_thrown_from_providers_getMessageBodyWriter() {
        otherExceptionsThrowFrom(this::providers_getMessageBodyWriter);
    }

    @Test
    public void other_exception_thrown_from_messageBodyWriter_writeTo() {
        otherExceptionsThrowFrom(this::messageBodyWriter_writeTo);
    }

    private void exceptionMapper_toResponse(RuntimeException exception) {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);

        when(providers.getExceptionMapper(RuntimeException.class)).thenReturn(e -> {
            throw exception;
        });
    }

    private void runtimeDelegate_createHeaderDelegate(RuntimeException exception) {
        response().headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(),
            new NewCookie.Builder("USER_ID").value("user").build()).returnFrom(router);

        when(runtimeDelegate.createHeaderDelegate(NewCookie.class)).thenThrow(exception);
    }

    private void headerDelegate_toString(RuntimeException exception) {
        response().headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(),
            new NewCookie.Builder("USER_ID").value("user").build()).returnFrom(router);

        when(runtimeDelegate.createHeaderDelegate(NewCookie.class)).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            @Override
            public NewCookie fromString(String value) {
                return null;
            }

            @Override
            public String toString(NewCookie value) {
                throw exception;
            }
        });
    }

    private void webApplicationExceptionThrowFrom(Consumer<RuntimeException> caller) {
        RuntimeException exception = new WebApplicationException(response().status(Response.Status.FORBIDDEN).build());

        caller.accept(exception);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    private void otherExceptionsThrowFrom(Consumer<RuntimeException> callers) {
        RuntimeException exception = new IllegalArgumentException();

        callers.accept(exception);

        when(providers.getExceptionMapper(eq(IllegalArgumentException.class)))
            .thenReturn(e -> response().status(Response.Status.FORBIDDEN).entity(null, new Annotation[0]).build());

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    private void providers_getMessageBodyWriter(RuntimeException exception) {
        response().entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(router);

        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
            .thenThrow(exception);
    }

    private void messageBodyWriter_writeTo(RuntimeException exception) {
        response().entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(router);

        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
            .thenReturn(new MessageBodyWriter<>() {
                @Override
                public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                    return false;
                }

                @Override
                public void writeTo(Double aDouble, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                    throw exception;
                }
            });
    }


    private OutboundResponseBuilder response() {
        return new OutboundResponseBuilder();
    }

    class OutboundResponseBuilder {
        private Response.Status status = Response.Status.OK;
        private MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        private GenericEntity<?> entity = new GenericEntity<>("entity", String.class);
        private Annotation[] annotations = new Annotation[0];
        private MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        public OutboundResponseBuilder status(Response.Status status) {
            this.status = status;
            return this;
        }

        public OutboundResponseBuilder headers(String key, Object... values) {
            this.headers.addAll(key, values);
            return this;
        }

        public OutboundResponseBuilder entity(GenericEntity<?> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        public OutboundResponseBuilder mediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public void returnFrom(ResourceRouter router) {
            build(response -> when(router.dispatch(any(), eq(resourceContext))).thenReturn(response));
        }

        public void throwFrom(ResourceRouter router) {
            build(response -> {
                WebApplicationException exception = new WebApplicationException(response);
                when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
            });
        }

        private void build(Consumer<OutboundResponse> consumer) {
            consumer.accept(build());
        }

        public OutboundResponse build() {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getStatusInfo()).thenReturn(status);
            when(response.getHeaders()).thenReturn(headers);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);

            stubMessageBodyWriter();

            return response;
        }

        private void stubMessageBodyWriter() {
            when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), same(annotations), eq(mediaType))).thenReturn(new MessageBodyWriter<>() {
                @Override
                public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                    return false;
                }

                @Override
                public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                    PrintWriter writer = new PrintWriter(entityStream);
                    writer.write(s);
                    writer.flush();
                }
            });
        }
    }
}
