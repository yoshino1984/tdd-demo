package yoshino.tdd.di.exception;

import yoshino.tdd.di.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * @author xiaoyi
 * 2022/12/31 10:51
 * @since
 **/
public class CyclicDependenciesException extends RuntimeException {

    List<Component> components;

    public CyclicDependenciesException(Stack<Component> visiting) {
        components = new ArrayList<>();
        components.addAll(visiting);
    }


    public List<Class<?>> getComponents() {
        return components.stream().map(Component::type).collect(Collectors.toList());
    }

}
