package org.elasticsearch.primaryaware.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.primaryaware.rest.bulk.RestAsyncBulkAction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionUtils {
    private static Logger logger = LogManager.getLogger(ReflectionUtils.class);

    public static <T> T getField(Object target, String fieldName) {
        return getClassField(target.getClass(), target, fieldName);
    }

    public static <T> T getSuperField(Object target, String fieldName) {
        return getClassField(target.getClass().getSuperclass(), target, fieldName);
    }

    public static <T> T getClassField(Class clazz, Object target, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (field == null) {
                throw new Exception("target: " + target + " doesn't has field: " + fieldName);
            }
            field.setAccessible(true);
            return (T)field.get(target);
        } catch (Exception e) {
            String errorMsg = "ReflectionUtils.getClassField, target: " + target + "  doesn't has field: " + fieldName;
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    public static <T> T invokeMethod(Object target, String methodName, Class[] argTypes, Object... args) {
        return invokeMethod(target.getClass(), target, methodName, argTypes, args);
    }

    public static <T> T invokeSuperMethod(Object target, String methodName, Class[] argTypes, Object... args) {
        return invokeMethod(target.getClass().getSuperclass(), target, methodName, argTypes, args);
    }

    public static <T> T invokeMethod(Class clazz, Object target, String methodName, Class[] argTypes, Object... args) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, argTypes);
            if (method == null) {
                throw new Exception("target: " + target + " doesn't has method: " + methodName);
            }
            method.setAccessible(true);
            return (T)method.invoke(target, args);
        } catch (Exception e) {
            String errorMsg = "ReflectionUtils.invokeMethod, target: " + target + " doesn't has method: " + methodName;
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    public static <T> T newInstance(Class clazz, Class[] argTypes, Object... args) {
        try {
            Constructor<T> clazzConstructor = clazz.getDeclaredConstructor(argTypes);
            if (clazzConstructor == null) {
                throw new Exception("class: " + clazz + " doesn't has constructor with args: " + Arrays.toString(argTypes));
            }
            clazzConstructor.setAccessible(true);
            return clazzConstructor.newInstance(args);
        } catch (Exception e) {
            String errorMsg = "ReflectionUtils.newInstance, class: " + clazz + " doesn't has constructor with args: " + Arrays.toString(argTypes);
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

}
