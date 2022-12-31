package yoshino.tdd.di;

import java.util.Optional;

/**
 * @author xiaoyi
 * 2022/12/31 12:26
 * @since
 **/
public interface Context {

    <Type> Optional<Type> get(Class<Type> type);

}
