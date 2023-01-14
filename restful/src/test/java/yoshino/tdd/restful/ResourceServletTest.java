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
    private OutboundResponseBuilder response;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);

        when(runtime.createResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getProviders()).thenReturn(providers);

        return new ResourceServlet(runtime);
    }

    @BeforeEach
    public void before() {
        response = new OutboundResponseBuilder();
        RuntimeDelegate runtimeDelegate = mock(RuntimeDelegate.class);
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
    public void should_use_status_code_from_response() throws Exception {
        response.status(Response.Status.NOT_MODIFIED).buildReturn(router);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_use_http_headers_from_response() throws Exception {
        response.headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(),
            new NewCookie.Builder("USER_ID").value("user").build()).buildReturn(router);

        HttpResponse<String> httpResponse = get("test");

        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    @Test
    public void should_write_entity_to_http_body_using_message_body_writer() throws Exception {
        response.entity(new GenericEntity<>("entity", String.class), new Annotation[0]).mediaType(MediaType.TEXT_PLAIN_TYPE).buildReturn(router);

        HttpResponse<String> httpResponse = get("test");

        assertEquals("entity", httpResponse.body());
    }

    @Test
    public void should_use_response_from_web_application_exception() throws Exception {
        response.status(Response.Status.FORBIDDEN).buildThrow(router);

        HttpResponse<String> httpResponse = get("test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_build_response_by_exception_mapper_if_not_web_application_exception() throws Exception {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);

        when(providers.getExceptionMapper(RuntimeException.class)).thenReturn(exception -> response.status(Response.Status.FORBIDDEN).build());

        HttpResponse<String> httpResponse = get("test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    // todo 500 if MessageBodyWriter not found
    // todo entity is null, ignore MessageBodyWriter
    class OutboundResponseBuilder {
        private Response.Status status = Response.Status.OK;
        private MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        private GenericEntity<String> entity = new GenericEntity<>("entity", String.class);
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

        public OutboundResponseBuilder entity(GenericEntity<String> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        public OutboundResponseBuilder mediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public void buildReturn(ResourceRouter router) {
            build(response -> when(router.dispatch(any(), eq(resourceContext))).thenReturn(response));
        }

        public void buildThrow(ResourceRouter router) {
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
