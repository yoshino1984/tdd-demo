package yoshino.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

/**
 * @author xiaoyi
 **/
public class ContainerTest {
    private ContextConfig config;

    @BeforeEach
    public void setUp() {
        config = new ContextConfig();
    }

    @Nested
    public class DependenciesSelection {

        @Nested
        public class ProviderType {

        }

    }

    @Nested
    public class LifecycleManagement {

    }
}

interface Component {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency {
}

interface AnotherDependency {
}






