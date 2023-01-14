package yoshino.tdd.restful;

import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;

import java.lang.annotation.Annotation;

/**
 * @author xiaoyi
 * 2023/1/14 11:45
 * @since
 **/
abstract class OutboundResponse extends Response {

    abstract GenericEntity getGenericEntity();

    abstract Annotation[] getAnnotations();

}
