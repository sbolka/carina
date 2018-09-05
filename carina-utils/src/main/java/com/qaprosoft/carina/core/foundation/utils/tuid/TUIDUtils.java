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
package com.qaprosoft.carina.core.foundation.utils.tuid;

import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.exception.AnnotationResolverException;
import com.qaprosoft.carina.core.foundation.utils.AnnotationUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.testng.ITestResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.qaprosoft.carina.core.foundation.utils.tuid.TUID.AdditionalInfo.NOT;

/**
 * Helps use basic logic of test name TUID generators
 * @author brutskov
 */
public class TUIDUtils extends AnnotationUtils {

    /**
     * Gets TUID by testNG transfer context object, test thread id , count, test class instance parameters
     * @param testResult - testNG transfer test data object
     * @param count - invocation count
     * @param params - class instance specific parameters
     * @return string built with basic test env info
     */
    public static String getTUID(final ITestResult testResult, int count, Object... params) {
        String tuid;
        if(testResult.getMethod().getConstructorOrMethod().getMethod().isAnnotationPresent(TUID.class)) {
            StringBuilder additionalInfo = new StringBuilder();
            tuid = String.join(StringUtils.SPACE, AnnotationUtils.<TUID, String>iterateAnnotations(() -> testResult.getMethod().getConstructorOrMethod().getMethod(), TUID.class, annotation -> {
                String result = null;
                String prefix = annotation.prefix();
                String postfix = annotation.postfix();
                String tuidProviderName = annotation.tuidProvider();
                ITUIDGeneratorListener generatorListener = getTUIDGeneratorListener(annotation);
                result = String.format(SpecialKeywords.TUID_PARAM, prefix + generatorListener.getTUID(testResult, count, params) + postfix);
                if (!Arrays.asList(annotation.additionalInfos()).contains(NOT)) {
                    ITUIDGenerator additionalGenerator = (ITUIDGenerator) getTUIDGeneratorListener(null);
                    additionalInfo.append(additionalGenerator.buildFor(annotation.additionalInfos(), testResult, params));
                }
                return result;
            })) + additionalInfo.toString();
        } else {
            tuid = getDefaultTUID(testResult, count, params);
        }
        return tuid;
    }

    private static Method getTUIDProvider(final ITestResult testResult, final String tuidProviderName) {
        List<Method> tuidProviders = MethodUtils.getMethodsListWithAnnotation(testResult.getTestClass().getRealClass(), TUIDProvider.class).stream().filter(method -> method.getAnnotation(TUIDProvider.class).name().equalsIgnoreCase(tuidProviderName)).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(tuidProviders)) {
            throw new AnnotationResolverException("Cannot find TUID provider with name '" + tuidProviderName + ".");
        } else if(tuidProviders.size() > 1) {
            throw new AnnotationResolverException("Cannot resolve TUID provider with name '" + tuidProviderName + "'. Required one annotated element but found " + tuidProviders.size());
        }
        Method tuidProvider = tuidProviders.get(0);
        try {
            ITUIDGeneratorListener.class.getMethod("getTUID", tuidProvider.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new AnnotationResolverException("Cannot resolve TUID provider with name '" + tuidProviderName + "'. Parameters ahould be " + Arrays.stream(ITUIDGeneratorListener.class.getMethods()).filter(method -> method.getName().equals("getTUID")).findFirst().orElse(null).getParameterTypes().toString());
        }
        return tuidProvider;
    }

    private static void invokeTUIDProvider(final ITestResult testResult, final String tuidProviderName, int count, Object... params) {
        try {
            MethodUtils.invokeMethod(testResult.getInstance(), tuidProviderName, testResult, count, params);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Gets default TUID base on class instance parameters and data provider
     * @param testResult - testNG transfer test data object
     * @param count - invocation count
     * @param params - class instance specific parameters
     * @return string built with basic test env info
     */
    public static String getDefaultTUID(final ITestResult testResult, final int count, final Object... params) {
        ITUIDGenerator additionalGenerator = (ITUIDGenerator) getTUIDGeneratorListener(null);
        return additionalGenerator != null ? additionalGenerator.getTUID(testResult, count, params) : null;
    }

    /**
     * Gets default TUID generator listener if annotation is null or by annotation instance
     * @param annotation - annotation instance
     * @return needed TUID generator listener
     */
    public static ITUIDGeneratorListener getTUIDGeneratorListener(final TUID annotation) {
        //checkAnnotation(annotation);
        ITUIDGeneratorListener additionalGenerator = null;
        try {
            Class<? extends ITUIDGeneratorListener> generatorClass = annotation != null ? annotation.getter() : AnnotationUtils.<Class<ITUIDGeneratorListener>>getAnnotationDefaultValue(TUID.class, TUID.getterName);
            additionalGenerator = generatorClass.newInstance();
        } catch (InstantiationException e) {
            LOGGER.error("Cannot find default constructor: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Have not access to default constructor: " + e.getMessage(), e);
        }
        return additionalGenerator;
    }

   /* private Class<? extends ITUIDGeneratorListener> getGeneratorClass(final TUID annotation) {
        Class<? extends ITUIDGeneratorListener> generatorClass = null;
        if(annotation != null) {
            gene
        }
        Class<? extends ITUIDGeneratorListener> generatorClass = annotation != null ? annotation.getter() : AnnotationUtils.<Class<ITUIDGeneratorListener>>getAnnotationDefaultValue(TUID.class, TUID.getterName);

    }*/

    private static void checkAnnotation(final TUID annotation) {
        if(annotation.getter() != null && annotation.tuidProvider() != null) {
            throw new AnnotationResolverException("Cannot resolve TUID provider: getter and tuidProvider attributes are mutually exclusive.");
        }
    }
}
