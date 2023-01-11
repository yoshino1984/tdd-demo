package yoshino.tdd.di;

import java.util.List;

/**
 * @author xiaoyi
 * 2023/1/8 16:48
 * @since
 **/
class SingletonProvider<T> implements ComponentProvider<T> {

    private T instance;
    private ComponentProvider<T> provider;

    SingletonProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (instance == null) {
            instance = provider.get(context);
        }
        return instance;
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}
