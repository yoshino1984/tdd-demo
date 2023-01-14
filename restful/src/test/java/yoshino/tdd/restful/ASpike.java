package yoshino.tdd.restful;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

import yoshino.tdd.di.ComponentRef;
import yoshino.tdd.di.Config;
import yoshino.tdd.di.Context;
import yoshino.tdd.di.ContextConfig;

public class ASpike {
    Server server;

    @BeforeEach
    public void start() throws Exception {
        server = new Server(8080);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler(server, "/");
        TestApplication application = new TestApplication();
        handler.addServlet(new ServletHolder(new ResourceServlet(application, new TestProviders(application))), "/");
        server.setHandler(handler);

        server.start();
    }

    @AfterEach
    public void stop() throws Exception {
        server.stop();
    }


    @Test
    public void should() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8080/")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals("prefixprefixtest", response.body());
    }

    class ResourceServlet extends HttpServlet {

        private final Context context;
        private TestApplication application;

        private Providers providers;

        ResourceServlet(TestApplication application, Providers providers) {
            this.application = application;
            this.providers = providers;
            this.context = application.getContext();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Stream<Class<?>> rootResource = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));

            ResourceContext rc = application.createResourceContext(req, resp);

            OutboundResponse result = dispatch(req, rootResource, rc);
            GenericEntity entity = result.getGenericEntity();

            MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), result.getAnnotations(), result.getMediaType());

            writer.writeTo(result, entity.getRawType(), entity.getType(), result.getAnnotations(), result.getMediaType(), result.getHeaders(), resp.getOutputStream());
        }

        private OutboundResponse dispatch(HttpServletRequest req, Stream<Class<?>> rootResources, ResourceContext rc) {
            try {
                Class<?> rootClass = rootResources.findFirst().get();
                Object rootResource = rc.initResource(context.get(ComponentRef.of(rootClass)).get());
                Method method = Arrays.stream(rootResource.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
                Object result = method.invoke(rootResource);

                GenericEntity entity = new GenericEntity(result, method.getGenericReturnType());

                return new OutboundResponse() {

                    @Override
                    GenericEntity getGenericEntity() {
                        return entity;
                    }

                    @Override
                    Annotation[] getAnnotations() {
                        return new Annotation[0];
                    }

                    @Override
                    public int getStatus() {
                        return 0;
                    }

                    @Override
                    public StatusType getStatusInfo() {
                        return null;
                    }

                    @Override
                    public Object getEntity() {
                        return result;
                    }

                    @Override
                    public <T> T readEntity(Class<T> aClass) {
                        return null;
                    }

                    @Override
                    public <T> T readEntity(GenericType<T> genericType) {
                        return null;
                    }

                    @Override
                    public <T> T readEntity(Class<T> aClass, Annotation[] annotations) {
                        return null;
                    }

                    @Override
                    public <T> T readEntity(GenericType<T> genericType, Annotation[] annotations) {
                        return null;
                    }

                    @Override
                    public boolean hasEntity() {
                        return false;
                    }

                    @Override
                    public boolean bufferEntity() {
                        return false;
                    }

                    @Override
                    public void close() {

                    }

                    @Override
                    public MediaType getMediaType() {
                        return null;
                    }

                    @Override
                    public Locale getLanguage() {
                        return null;
                    }

                    @Override
                    public int getLength() {
                        return 0;
                    }

                    @Override
                    public Set<String> getAllowedMethods() {
                        return null;
                    }

                    @Override
                    public Map<String, NewCookie> getCookies() {
                        return null;
                    }

                    @Override
                    public EntityTag getEntityTag() {
                        return null;
                    }

                    @Override
                    public Date getDate() {
                        return null;
                    }

                    @Override
                    public Date getLastModified() {
                        return null;
                    }

                    @Override
                    public URI getLocation() {
                        return null;
                    }

                    @Override
                    public Set<Link> getLinks() {
                        return null;
                    }

                    @Override
                    public boolean hasLink(String s) {
                        return false;
                    }

                    @Override
                    public Link getLink(String s) {
                        return null;
                    }

                    @Override
                    public Link.Builder getLinkBuilder(String s) {
                        return null;
                    }

                    @Override
                    public MultivaluedMap<String, Object> getMetadata() {
                        return null;
                    }

                    @Override
                    public MultivaluedMap<String, String> getStringHeaders() {
                        return null;
                    }

                    @Override
                    public String getHeaderString(String s) {
                        return null;
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    abstract class OutboundResponse extends Response {

        abstract GenericEntity getGenericEntity();
        abstract Annotation[] getAnnotations();

    }

    interface ResourceRouter {
        OutboundResponse dispatch(HttpServletRequest req, ResourceContext rc);
    }

    static class TestProviders implements Providers {

        private final List<MessageBodyWriter> writers;
        private TestApplication application;

        public TestProviders(TestApplication application) {
            this.application = application;

            List<Class<?>> rootClasses = application.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();

            this.writers = (List<MessageBodyWriter>) rootClasses.stream().map(c -> application.getContext().get(ComponentRef.of(c)).get()).toList();
        }

        @Override
        public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
            return null;
        }

        @Override
        public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
            return writers.stream().filter(it -> it.isWriteable(aClass, type, annotations, mediaType)).findFirst().get();
        }

        @Override
        public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> aClass) {
            return null;
        }

        @Override
        public <T> ContextResolver<T> getContextResolver(Class<T> aClass, MediaType mediaType) {
            return null;
        }
    }

    static class StringMessageBodyWriter implements MessageBodyWriter<String> {

        @Named("prefix")
        @Inject
        private String prefix;

        public StringMessageBodyWriter() {
        }

        @Override
        public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
            return aClass == String.class;
        }

        @Override
        public void writeTo(String s, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException {

            PrintWriter writer = new PrintWriter(outputStream);
            writer.write(prefix + s);
            writer.flush();
        }
    }

    static class TestApplication extends Application {
        private Context context;

        public TestApplication() {
            ContextConfig config = new ContextConfig();
            config.from(getConfig());
            List<Class<?>> rootClasses = this.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            for (Class rootClass : rootClasses) {
                config.component(rootClass, rootClass);
            }

            List<Class<?>> resourceClasses = this.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class)).toList();
            for (Class resourceClass : resourceClasses) {
                config.component(resourceClass, resourceClass);
            }
            context = config.getContext();
        }

        public Config getConfig() {
            return new Config() {
                @Named("prefix")
                private String prefix = "prefix";
            };
        }

        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TestResource.class, StringMessageBodyWriter.class);
        }

        public Context getContext() {
            return context;
        }

        public ResourceContext createResourceContext(HttpServletRequest req, HttpServletResponse resp) {
            return null;
        }
    }

    @Path("/")
    static class TestResource {

        @Named("prefix")
        @Inject
        String prefix;

        public TestResource() {
        }

        @GET
        public String get() {
            return prefix + "test";
        }
    }

}
