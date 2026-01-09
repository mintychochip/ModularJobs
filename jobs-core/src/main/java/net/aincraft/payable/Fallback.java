package net.aincraft.payable;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Binding annotation for the fallback ExperienceBarColorProvider.
 */
@BindingAnnotation
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Fallback {
}
