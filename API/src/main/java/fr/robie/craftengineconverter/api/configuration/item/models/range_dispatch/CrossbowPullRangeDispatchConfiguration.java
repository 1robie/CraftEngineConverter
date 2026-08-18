package fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch;

/**
 * How far a crossbow has been pulled, from 0 to 1.
 *
 * <h2>Not the same thing as {@code use_duration}</h2>
 * {@link UseDurationRangeDispatchConfiguration} reports ticks, which is why it carries a {@code scale} to turn them
 * into a fraction. This property is <b>already</b> a fraction: the Java client divides the elapsed time by the
 * crossbow's own charge duration, which Quick Charge shortens, so no scale exists to read and none is meaningful.
 * Anything reproducing it has to normalise for itself.
 */
public class CrossbowPullRangeDispatchConfiguration extends RangeDispatchModelConfiguration {

    public CrossbowPullRangeDispatchConfiguration() {
        super("minecraft:crossbow/pull");
    }
}
