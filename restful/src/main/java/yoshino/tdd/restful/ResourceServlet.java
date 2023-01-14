package yoshino.tdd.restful;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;

import java.io.IOException;
import java.lang.annotation.Annotation;

public class ResourceServlet extends HttpServlet {

    private Runtime runtime;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.createResourceRouter();

        OutboundResponse response;
        try {
            response = router.dispatch(req, runtime.createResourceContext(req, resp));
        } catch (WebApplicationException e) {
            response = (OutboundResponse) e.getResponse();
        } catch (Throwable throwable) {
            ExceptionMapper mapper = runtime.getProviders().getExceptionMapper(throwable.getClass());
            response = (OutboundResponse) mapper.toResponse(throwable);
        }

        resp.setStatus(response.getStatus());
        MultivaluedMap<String, Object> headers = response.getHeaders();
        for (String name : headers.keySet()) {
            for (Object value : headers.get(name)) {
                resp.addHeader(name, value.toString());
            }
        }

        GenericEntity entity = response.getGenericEntity();
        MessageBodyWriter writer = runtime.getProviders().getMessageBodyWriter(entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType());
        writer.writeTo(entity.getEntity(), entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType(), headers, resp.getOutputStream());
    }
}
