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
import com.qaprosoft.carina.core.foundation.utils.AnnotationUtils;
import com.qaprosoft.carina.core.foundation.utils.TestNamingParameters;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.log4j.Logger;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base implementation of TUID generator
 * @author brutskov
 */
class SimpleTUIDGeneratorListener implements ITUIDGenerator {

    private static final Logger LOGGER = Logger.getLogger(SimpleTUIDGeneratorListener.class);
    private static final String DIVIDER = ", ";

    /**
     * Transfer basic test env info to implementation
     * @param testResult - testNG transfer test data object
     * @param count - count of current test invocations
     * @param instanceParams - class instance specific parameters
     * @return string built with basic test env info
     */
    @Override
    public String getTUID(ITestResult testResult, int count, Object... instanceParams) {
        return buildFromFactoryInstance(testResult, instanceParams) + buildFromDataProviderParams(testResult);
    }

    /**
     * Builds TUID string from test class instance using {@link TestNamingParameters} annotation
     * @param testResult - testNG transfer test data object
     * @param instanceParams - class instance specific parameters
     * @return string built with testNG dto context and test class instance parameters
     */
    @Override
    public String buildFromFactoryInstance(ITestResult testResult, Object... instanceParams) {
        String result = StringUtils.EMPTY;
        if(testResult.getTestClass().getRealClass().isAnnotationPresent(TestNamingParameters.class)) {
            result = result + buildString(SpecialKeywords.FACTORY_PARAM, DIVIDER, getTestNamingNames(testResult));
        } else if(! ArrayUtils.isEmpty(instanceParams)) {
            result = result + buildString(SpecialKeywords.FACTORY_PARAM, DIVIDER, instanceParams);
        }
        return result;
    }

    /**
     * Builds TUID string from data provider parameters
     * @param testResult - testNG transfer test data object
     * @return string built with testNG dto context
     */
    @Override
    public String buildFromDataProviderParams(ITestResult testResult) {
        String result = StringUtils.EMPTY;
        final Method testMethod = testResult.getMethod().getConstructorOrMethod().getMethod();
        if(testMethod.isAnnotationPresent(Test.class) && ! StringUtils.isBlank(testMethod.getAnnotation(Test.class).dataProvider())
                && testResult.getParameters().length > 0) {
            result = buildString(SpecialKeywords.DATA_ROVIDER_PARAMS, DIVIDER, testResult.getParameters());
        }
        return result;
    }

    /**
     * Delimiter getter to join any parameters list. Value: <b>, '</b>
     * @return joint array
     */
    @Override
    public String getDelimiter() {
        return DIVIDER;
    }

    /**
     * Builds string by {@link String} pattern with only <b>ONE</b> placeholder using parameters which will be joined by delimiter
     * @param pattern - {@link String} pattern with placeholder
     * @param delimiter - delimiter to join
     * @param parameters - parameters to join
     * @return built string
     */
    private static String buildString(final String pattern, final String delimiter, final Object... parameters) {
        return String.format(pattern, Arrays.stream(parameters).map(TUIDUtils::objectToString).collect(Collectors.joining(delimiter)));
    }

    /**
     * Handles {@link TestNamingParameters} annotations and gets fields values by {@link TestNamingParameters}.names()
     * @param test - testNG result object
     * @return list of fields values
     */
    private static List<Object> getTestNamingNames(final ITestResult test) {
        return AnnotationUtils.<TestNamingParameters, List<Object>>iterateAnnotations(() -> test.getTestClass().getRealClass(), TestNamingParameters.class, annotation -> {
            String[] values = annotation.names();
            return Arrays.stream(values).filter(v -> FieldUtils.getField(test.getTestClass().getRealClass(), v) != null).map(v -> {
                Field field = FieldUtils.getField(test.getTestClass().getRealClass(), v, true);
                Object value = null;
                try {
                    value = FieldUtils.readField(field, test.getInstance(), true);
                } catch (IllegalAccessException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                return value;
            }).collect(Collectors.toList());
        }).stream().flatMap(Collection::stream).collect(Collectors.toList());
    }
}
