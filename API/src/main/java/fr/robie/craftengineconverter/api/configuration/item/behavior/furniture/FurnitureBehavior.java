package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * One entry of a furniture's {@code behavior} list. CraftEngine reads that list as maps keyed by {@code type}, so
 * each behaviour describes itself as a map rather than writing into a section of its own.
 */
public interface FurnitureBehavior {

    @NotNull Map<String, Object> serialize();
}
