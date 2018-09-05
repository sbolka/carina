package com.qaprosoft.carina.core.foundation.utils.tuid;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface TUIDProvider {

    String name();
}
