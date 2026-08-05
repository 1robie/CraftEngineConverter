package fr.robie.craftengineconverter.api.configuration.template;

/**
 * A template could not be resolved — an unknown template id, or a {@code ${placeholder}} with neither a
 * bound argument nor a fallback.
 * <p>
 * Thrown rather than skipped because a half-resolved template would emit an item with a
 * {@code ${...}} still in its material or texture path, which fails later and much less legibly. Callers
 * catch this per item so one bad template does not abort the whole conversion.
 */
public class TemplateException extends RuntimeException {

    public TemplateException(String message) {
        super(message);
    }
}
