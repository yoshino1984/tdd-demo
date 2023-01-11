package yoshino.tdd.di;

/**
 * @author xiaoyi
 * 2023/1/8 17:16
 * @since
 **/
interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
