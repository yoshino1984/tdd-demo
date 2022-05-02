package yoshino.tdd.di;

import java.util.Optional;

/**
 * @author xiaoyi
 * 2022/5/2 14:58
 * @since
 **/
public interface Context {
    <T> Optional<T> get(Class<T> type);
}
