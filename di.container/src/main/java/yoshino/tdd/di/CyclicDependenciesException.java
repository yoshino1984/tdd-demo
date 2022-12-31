package yoshino.tdd.di;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xiaoyi
 * 2022/12/31 10:51
 * @since
 **/
public class CyclicDependenciesException extends RuntimeException {

    List<Class<?>> dependencies;

    public CyclicDependenciesException(Class<?> dependencyType) {
        dependencies = new ArrayList<>();
        dependencies.add(dependencyType);
    }
    public CyclicDependenciesException(CyclicDependenciesException e, Class<?> componentType) {
        dependencies = new ArrayList<>();
        dependencies.addAll(e.getDependencies());
        dependencies.add(componentType);
    }


    public List<Class<?>> getDependencies() {
        return dependencies;
    }

}
