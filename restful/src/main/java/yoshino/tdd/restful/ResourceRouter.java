package yoshino.tdd.restful;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;


/**
 * @author xiaoyi
 * 2023/1/14 11:45
 * @since
 **/
interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest req, ResourceContext rc);
}
