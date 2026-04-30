package fr.robie.craftengineconverter.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the minimum Minecraft version (inclusive) for which a class should be registered.
 * Classes annotated with this will be skipped on servers running versions older than the declared minimum.
 *
 * <p>Example: {@code @SinceVersion("1.21.4")} registers the class only on 1.21.4 and above.
 *
 * <p>Can be combined with {@link UntilVersion} to define a closed version range,
 * and with {@link PaperOnly} / {@link SpigotOnly} for platform filtering.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SinceVersion {

    /**
     * The minimum Minecraft version string, e.g. {@code "1.21.4"}.
     */
    String value();
}
