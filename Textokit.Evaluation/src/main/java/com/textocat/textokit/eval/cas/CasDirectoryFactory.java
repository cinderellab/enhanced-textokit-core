
/*
 *    Copyright 2015 Textocat
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.textocat.textokit.eval.cas;

import org.apache.uima.cas.TypeSystem;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Rinat Gareev
 */
public class CasDirectoryFactory {

    public static CasDirectory createDirectory(TypeSystem typeSystem, String implClassName,
                                               Map<String, String> props) {
        try {
            @SuppressWarnings("unchecked")
            Class<CasDirectory> implClass = (Class<CasDirectory>) Class.forName(implClassName);
            CasDirectory result = implClass.newInstance();
            result.setTypeSystem(typeSystem);
            for (String curPropName : props.keySet()) {
                setProperty(result, curPropName, props.get(curPropName));
            }
            result.init();
            return result;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setProperty(Object bean, String propName, String propValueStr)
            throws Exception {
        String setterName = "set" + capitalize(propName);
        Method setter = findMethod(bean.getClass(), setterName, 1);
        if (setter == null) {
            throw new IllegalStateException(String.format("Can't find method %s in %s",
                    setterName, bean.getClass()));
        }
        Class<?> propClass = setter.getParameterTypes()[0];
        Object propValue = convertString(propClass, propValueStr);
        setter.invoke(bean, propValue);
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertString(Class<T> targetType, String src) {
        if (Integer.class.equals(targetType)) {
            return (T) Integer.valueOf(src);
        }
        if (Long.class.equals(targetType)) {
            return (T) Long.valueOf(src);
        }
        if (File.class.equals(targetType)) {
            return (T) new File(src);
        }
        throw new IllegalStateException("Can't convert from String to " + targetType);
    }

    private static Method findMethod(Class<?> clazz, String name, int parametersNumber) {
        for (Method curMethod : clazz.getMethods()) {
            if (curMethod.getName().equals(name)
                    && curMethod.getParameterTypes().length == parametersNumber) {
                return curMethod;
            }
        }
        return null;
    }

    private static String capitalize(String str) {
        if (Character.isUpperCase(str.charAt(0))) {
            return str;
        } else {
            return new StringBuilder(str).replace(0, 1, str.substring(0, 1).toUpperCase())
                    .toString();
        }
    }
}