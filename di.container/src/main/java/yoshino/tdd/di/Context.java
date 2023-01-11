package yoshino.tdd.di;

import java.util.Optional;

/**
 * @author xiaoyi
 * 2022/12/31 12:26
 * @since
 **/
public interface Context {

    <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> componentRef);

}
