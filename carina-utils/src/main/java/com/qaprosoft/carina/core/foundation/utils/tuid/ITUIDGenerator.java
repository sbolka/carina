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

import org.apache.commons.lang3.StringUtils;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Needs to implement defaults TUID generators
 * @author brutskov
 */
interface ITUIDGenerator extends ITUIDGeneratorListener {

    /**
     * Builds TUID string from test class instance
     * @param testResult - testNG transfer test data object
     * @param instanceParams - class instance specific parameters
     * @return string built with testNG dto context and test class instance parameters
     */
    String buildFromFactoryInstance(ITestResult testResult, Object... instanceParams);

    /**
     * Builds TUID string from data provider parameters
     * @param testResult - testNG transfer test data object
     * @return string built with testNG dto context
     */
    String buildFromDataProviderParams(ITestResult testResult);

    /**
     * Delimiter getter to join any parameters list
     * @return joint array
     */
    String getDelimiter();

    /**
     * Trigger to invoke builder method with appropriate {@link com.qaprosoft.carina.core.foundation.utils.tuid.TUID.AdditionalInfo} enum
     * @param additionalInfos - {@link com.qaprosoft.carina.core.foundation.utils.tuid.TUID.AdditionalInfo} enum
     * @param testResult - testNG transfer test data object
     * @param instanceParams - class instance specific parameters
     * @return string built by appropriate {@link com.qaprosoft.carina.core.foundation.utils.tuid.TUID.AdditionalInfo} enum
     */
    default String buildFor(TUID.AdditionalInfo[] additionalInfos, ITestResult testResult, Object... instanceParams) {
        return Arrays.stream(additionalInfos).distinct().map(additionalInfo -> {
            String result = null;
            switch (additionalInfo) {
                case CLASS_INSTANCE:
                    result = buildFromFactoryInstance(testResult, instanceParams);
                    break;
                case DATA_PROVIDER_PARAMS:
                    result = buildFromDataProviderParams(testResult);
                    break;
                default:
                    break;
            }
            return result;
        }).filter(additionalInfo -> ! StringUtils.isBlank(additionalInfo)).collect(Collectors.joining(getDelimiter()));
    }
}
