package fr.robie.craftengineconverter.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as Paper-only.
 * Classes annotated with this will only be registered when running on Paper or Paper-based servers.
 * Can be combined with {@link SinceVersion} and {@link UntilVersion} for finer-grained control.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PaperOnly {
}
