package yoshino.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.Provider;
import java.util.List;
import java.util.Optional;

/**
 * @author xiaoyi
 * 2022/12/31 12:26
 * @since
 **/
public interface Context {

    Optional getType(Type type);

}
