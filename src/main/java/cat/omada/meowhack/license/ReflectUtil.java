/*
 * Originally from fabric-license-check by Flowey (GPL-3.0).
 * https://github.com/FloweyTheFlower/fabric-license-check
 * Integrated into meowhack, also GPL-3.0.
 */
package cat.omada.meowhack.license;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectUtil {
    public static Result<Class<?>, Exception> getClass(String clazzName) {
        try {
            return Result.of(Class.forName(clazzName));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public static Result<Method, Exception> getMethod(Class<?> clazz, String method, Class<?>... args) {
        try {
            return Result.of(clazz.getMethod(method, args));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public static Result<Field, Exception> getField(Class<?> clazz, String method) {
        try {
            return Result.of(clazz.getField(method));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public static Result<Object, Exception> invokeMethod(Method method, Object that, Object... args) {
        try {
            return Result.of(method.invoke(that, args));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    public static Result<Object, Exception> invokeStatic(Method method, Object... args) {
        try {
            return Result.of(method.invoke(null, args));
        } catch (Exception e) {
            return Result.error(e);
        }
    }
}
