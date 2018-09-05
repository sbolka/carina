/*******************************************************************************
 * Copyright 2013-2018 QaProSoft (http://www.qaprosoft.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.qaprosoft.carina.core.foundation.utils;

import org.apache.log4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Contains basic annotations supporting methods
 * @author brutskov
 */
public class AnnotationUtils {

    protected static final Logger LOGGER = Logger.getLogger(AnnotationUtils.class);

    /**
     * Gets list of annotations found in annotated element
     * @param annotatedElementSupplier - supplier of element for look up
     * @param annotationType - annotaion class for look up
     * @param <T> - class should be an annotation
     * @return list of annotations found in element
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> List<T> getAnnotationInstances(final Supplier<AnnotatedElement> annotatedElementSupplier, final Class<T> annotationType) {
        List<T> result = null;
        if(annotatedElementSupplier.get().isAnnotationPresent(annotationType)) {
            Annotation[] annotations = annotatedElementSupplier.get().getAnnotations();
            result = Arrays.stream(annotations).filter(annotation -> annotation.annotationType().isAssignableFrom(annotationType)).map(annotation -> (T) annotation).collect(Collectors.toList());
        }
        return result;
    }

    /**
     * Does smth in each annotation item
     * @param annotatedElementSupplier - supplier of element for look up
     * @param annotationType - annotaion class for look up
     * @param function - some action with annotated element
     * @param <T> - class should be an annotation
     * @param <R> - return type of the action
     * @return list of action results
     */
    public static <T extends Annotation, R> List<R> iterateAnnotations(final Supplier<AnnotatedElement> annotatedElementSupplier, final Class<T> annotationType, final Function<T, R> function) {
        return getAnnotationInstances(annotatedElementSupplier, annotationType).stream().map(function).collect(Collectors.toList());
    }

    /**
     * Gets annotation default value by annotation class and annotation method
     * @param aClass - annotaion class for look up
     * @param methodName - method to get default value
     * @param <T> - return type of annotation method
     * @return default value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationDefaultValue(Class<? extends Annotation> aClass, String methodName) {
        T result = null;
        try {
            Method annotationMethod = aClass.getDeclaredMethod(methodName);
            if(annotationMethod != null) {
                result = (T) annotationMethod.getDefaultValue();
            }
        } catch (NoSuchMethodException e) {
        }
        return result;
    }
}
