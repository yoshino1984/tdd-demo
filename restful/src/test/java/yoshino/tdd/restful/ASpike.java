package yoshino.tdd.restful;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Application;
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

    static class ResourceServlet extends HttpServlet {

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
            Object result = dispatch(req, rootResource, rc);

            MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(result.getClass(), null, null, null);

            writer.writeTo(result, null, null, null, null, null, resp.getOutputStream());
        }

        private Object dispatch(HttpServletRequest req, Stream<Class<?>> rootResources, ResourceContext rc) {
            try {
                Class<?> rootClass = rootResources.findFirst().get();
                Object rootResource = rc.initResource(context.get(ComponentRef.of(rootClass)).get());
                Method method = Arrays.stream(rootResource.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
                return method.invoke(rootResource);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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
