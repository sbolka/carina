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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used for test name TUID injecting using some rules
 * @author brutskov
 */
@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface TUID {

    String getterName = "getter";

    /**
     * Test name TUID prefix
     */
    String prefix() default "";

    /**
     * Test name TUID postfix
     */
    String postfix() default "";

    /**
     * Test name TUID provider
     * Mutually exclusive with the getter attribute
     */
    String tuidProvider();

    /**
     * Test name TUID generator
     * Mutually exclusive with the tuidProvider attribute
     */
    Class<? extends ITUIDGeneratorListener> getter() default SimpleTUIDGeneratorListener.class;

    /**
     * Inject additional info parameters
     */
    AdditionalInfo[] additionalInfos() default {AdditionalInfo.NOT};

    /**
     * TUID additional info instances
     */
    enum AdditionalInfo {
        CLASS_INSTANCE, DATA_PROVIDER_PARAMS, NOT
    }
}
