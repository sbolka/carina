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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.qaprosoft.carina.core.foundation.utils.tuid.TUID.AdditionalInfo.NOT;

/**
 * Helps use basic logic of test name TUID generators
 * @author brutskov
 */
public class TUIDUtils extends AnnotationUtils {

    private static final String TUID_ERROR_MESSAGE_PATTERN = "Cannot resolve TUID provider: %s";

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
                ITUIDGeneratorListener generatorListener = getTUIDGeneratorListener(testResult, annotation);
                result = String.format(SpecialKeywords.TUID_PARAM, prefix + generatorListener.getTUID(testResult, count, params) + postfix);
                if (!Arrays.asList(annotation.additionalInfos()).contains(NOT)) {
                    ITUIDGenerator additionalGenerator = (ITUIDGenerator) getTUIDGeneratorListener(testResult, null);
                    additionalInfo.append(additionalGenerator.buildFor(annotation.additionalInfos(), testResult, params));
                }
                return result;
            })) + objectToString(additionalInfo);
        } else {
            tuid = getDefaultTUID(testResult, count, params);
        }
        return tuid;
    }

    private static String invokeTUIDProvider(final ITestResult testResult, final Method method, int count, Object... params) {
        String result = null;
        try {
            result = objectToString(method.invoke(testResult.getInstance(), testResult, count, params));
        } catch (Exception e) {
            throw new AnnotationResolverException(getErrorMessage("cannot find provider method with parameters"));
        }
        return result;
    }

    /**
     * Gets default TUID base on class instance parameters and data provider
     * @param testResult - testNG transfer test data object
     * @param count - invocation count
     * @param params - class instance specific parameters
     * @return string built with basic test env info
     */
    public static String getDefaultTUID(final ITestResult testResult, final int count, final Object... params) {
        ITUIDGenerator additionalGenerator = (ITUIDGenerator) getTUIDGeneratorListener(testResult, null);
        return additionalGenerator != null ? additionalGenerator.getTUID(testResult, count, params) : null;
    }

    /**
     * Gets default TUID generator listener if annotation is null or by annotation instance
     * @param annotation - annotation instance
     * @return needed TUID generator listener
     */
    public static ITUIDGeneratorListener getTUIDGeneratorListener(final ITestResult testResult, final TUID annotation) {
        if(annotation != null) {
            checkAnnotation(annotation);
        }
        ITUIDGeneratorListener additionalGenerator = null;
        try {
            Class<? extends ITUIDGeneratorListener> generatorClass = annotation != null ? annotation.tuidProvider().isEmpty() ? annotation.getter() : null : AnnotationUtils.<Class<ITUIDGeneratorListener>>getAnnotationDefaultValue(TUID.class, TUID.getterName);
            additionalGenerator = generatorClass != null ? generatorClass.newInstance() : annotation != null ? (testResult1, invocationCount, instanceParams) -> invokeTUIDProvider(testResult1, getTUIDProviderMethod(testResult, annotation), invocationCount, instanceParams) : null;
        } catch (InstantiationException e) {
            LOGGER.error(getErrorMessage("cannot find default constructor: ") + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LOGGER.error(getErrorMessage("have not access to default constructor: ") + e.getMessage(), e);
        }
        return additionalGenerator;
    }

    private static void checkAnnotation(final TUID annotation) {
        if(! annotation.getter().isAssignableFrom(SimpleTUIDGeneratorListener.class) && ! annotation.tuidProvider().isEmpty()) {
            throw new AnnotationResolverException(getErrorMessage("getter and tuidProvider attributes are mutually exclusive."));
        }
    }

    private static Method getTUIDProviderMethod(final ITestResult testResult, final TUID annotation) {
        Method tuidProviderMethod = null;
        if(! annotation.tuidProvider().isEmpty()) {
            List<Method> tuidProviderMethods = MethodUtils.getMethodsListWithAnnotation(testResult.getTestClass().getRealClass(), TUIDProvider.class).stream().filter(method -> method.getAnnotation(TUIDProvider.class).name().equals(annotation.tuidProvider())).collect(Collectors.toList());
            String message = tuidProviderMethods.size() == 0 ? "cannot find tuid provider with name '" + annotation.tuidProvider() + "'" : tuidProviderMethods.size() > 1 ? "multiple tuid providers with name '" + annotation.tuidProvider() + "' were found" : null;
            if(message != null) {
                throw new AnnotationResolverException(getErrorMessage(message));
            }
            tuidProviderMethod = tuidProviderMethods.get(0);
            if(ITUIDGeneratorListener.class.isAnnotationPresent(FunctionalInterface.class)) {
                Method originMethod = ITUIDGeneratorListener.class.getDeclaredMethods()[0];
                if(! Arrays.equals(originMethod.getParameterTypes(), tuidProviderMethod.getParameterTypes())) {
                    throw new AnnotationResolverException(getErrorMessage("tuid provider has invalid parameters. Parameter types should be (" + Arrays.stream(originMethod.getParameterTypes()).map(Class::getName).collect(Collectors.joining(", ")) + ")"));
                }
            }
        }
        return tuidProviderMethod;
    }

    private static String getErrorMessage(String messagePostfix) {
        return String.format(TUID_ERROR_MESSAGE_PATTERN, messagePostfix);
    }

    public static String objectToString(Object o) {
        String result = null;
        if(o != null) {
            result = o.toString();
            Matcher matcher = Pattern.compile("(?=@).+$").matcher(result);
            if(matcher.find()) {
                result = o.getClass().getName();
            }
        }
        return result;
    }
}
