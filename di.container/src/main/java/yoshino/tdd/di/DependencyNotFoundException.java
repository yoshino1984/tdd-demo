package yoshino.tdd.di;

/**
 * @author xiaoyi
 * 2022/12/31 10:40
 * @since
 **/

public class DependencyNotFoundException extends RuntimeException {
    public Class<?> getInstance() {
        return instance;
    }

    Class<?> instance;

    public DependencyNotFoundException(Class<?> instance) {
        this.instance = instance;
    }


}
