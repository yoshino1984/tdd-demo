package yoshino.tdd.restful;

import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.ext.Providers;
import yoshino.tdd.di.Context;

public interface Runtime {

    Context getApplicationContext();

    Providers getProviders();

    ResourceContext createResourceContext();

    ResourceRouter createResourceRouter();
}
