package yoshino.tdd.restful;

import jakarta.ws.rs.container.ResourceContext;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xiaoyi
 * 2023/1/14 11:45
 * @since
 **/
interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest req, ResourceContext rc);
}
