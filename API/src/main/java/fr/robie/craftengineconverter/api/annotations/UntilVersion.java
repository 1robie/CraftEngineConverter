package fr.robie.craftengineconverter.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the maximum Minecraft version (inclusive) for which a class should be registered.
 * Classes annotated with this will be skipped on servers running versions newer than the declared maximum.
 *
 * <p>Example: {@code @UntilVersion("1.21.3")} registers the class only on 1.21.3 and below.
 *
 * <p>Can be combined with {@link SinceVersion} to define a closed version range,
 * and with {@link PaperOnly} / {@link SpigotOnly} for platform filtering.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UntilVersion {

    /**
     * The maximum Minecraft version string, e.g. {@code "1.21.3"}.
     */
    String value();
}
