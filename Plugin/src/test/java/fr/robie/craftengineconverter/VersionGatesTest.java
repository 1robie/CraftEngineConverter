package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.utils.MinecraftVersion;
import fr.robie.craftengineconverter.converter.bedrock.item.VersionGates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CraftEngine's {@code $$} version gates.
 * <p>
 * These decide whether an item is converted at all, and getting them wrong is invisible: an entry that fails its
 * gate simply is not there. That is exactly how the trident, the spear and the elytra were lost — not by
 * converting badly, but by never being read.
 */
class VersionGatesTest {

    /**
     * The shapes the YAML layer produces. {@code .} is the configuration path separator, so a single authored key
     * arrives split across sections and has to be rejoined before it means anything; an item id never looks like
     * one of the pieces.
     */
    @Test
    void tellsGateFragmentsApartFromItemIds() {
        // $$>=1.21.4#topaz_trident splits into these three.
        assertTrue(VersionGates.isFragment("$$>=1"));
        assertTrue(VersionGates.isFragment("21"));
        assertTrue(VersionGates.isFragment("4#topaz_trident"));

        // $$1.20.1~1.21.3#topaz_trident splits into these five - note the range's middle.
        assertTrue(VersionGates.isFragment("$$1"));
        assertTrue(VersionGates.isFragment("20"));
        assertTrue(VersionGates.isFragment("1~1"));
        assertTrue(VersionGates.isFragment("3#topaz_trident"));

        assertFalse(VersionGates.isFragment("default:topaz_trident"));
        assertFalse(VersionGates.isFragment("minecraft:bow"));
        assertFalse(VersionGates.isFragment("settings"));
        assertFalse(VersionGates.isFragment(""));
    }

    @Test
    void stripsTheLabelFromARejoinedGate() {
        assertEquals("$$>=1.21.4", VersionGates.expressionOf("$$>=1.21.4#topaz_trident"));
        assertEquals("$$>=1.20.3", VersionGates.expressionOf("$$>=1.20.3"));
        assertTrue(VersionGates.isLabelled("$$>=1.21.4#topaz_trident"));
        assertFalse(VersionGates.isLabelled("$$>=1.20.3"));
    }

    /** The sample pack's two competing tridents: exactly one must survive for any given version. */
    @Test
    void theTwoTridentGatesAreMutuallyExclusive() {
        String modern = "$$>=1.21.4";
        String legacy = "$$1.20.1~1.21.3";

        MinecraftVersion current = MinecraftVersion.parse("1.21.11");
        assertTrue(VersionGates.accepts(modern, current));
        assertFalse(VersionGates.accepts(legacy, current));

        MinecraftVersion old = MinecraftVersion.parse("1.20.4");
        assertFalse(VersionGates.accepts(modern, old));
        assertTrue(VersionGates.accepts(legacy, old));

        // The boundaries are inclusive on both sides, as CraftEngine writes them.
        assertTrue(VersionGates.accepts(legacy, MinecraftVersion.parse("1.20.1")));
        assertTrue(VersionGates.accepts(legacy, MinecraftVersion.parse("1.21.3")));
        assertTrue(VersionGates.accepts(modern, MinecraftVersion.parse("1.21.4")));
    }

    @Test
    void readsEveryComparisonForm() {
        MinecraftVersion v = MinecraftVersion.parse("1.21.4");
        assertTrue(VersionGates.accepts("$$>=1.21.4", v));
        assertTrue(VersionGates.accepts("$$<=1.21.4", v));
        assertFalse(VersionGates.accepts("$$>1.21.4", v));
        assertFalse(VersionGates.accepts("$$<1.21.4", v));
        assertTrue(VersionGates.accepts("$$>1.21.3", v));
        assertTrue(VersionGates.accepts("$$1.21.4", v));
        assertFalse(VersionGates.accepts("$$1.21.5", v));
    }

    /**
     * Without Bukkit there is no server version, and every {@code >=} gate would fail — which is the silent drop
     * this whole class exists to end, and would make the headless conversion useless for exactly the items it was
     * added for. Unknown therefore means newest.
     */
    @Test
    void anUnknownVersionResolvesToTheNewestBranch() {
        MinecraftVersion target = VersionGates.targetVersion();
        assertTrue(VersionGates.accepts("$$>=1.21.4", target),
                "the modern trident must be kept when the version is unknown");
        assertFalse(VersionGates.accepts("$$1.20.1~1.21.3", target),
                "and its legacy twin must not, or the item would convert twice");
        assertTrue(VersionGates.accepts("$$>=1.21.11", target), "the spear too");
    }

    /** An unreadable gate converts its entries rather than dropping them: dropping is the failure being fixed. */
    @Test
    void anUnreadableGateAdmitsEverything() {
        assertTrue(VersionGates.accepts("$$not-a-version", MinecraftVersion.parse("1.21.4")));
        assertTrue(VersionGates.accepts("$$", MinecraftVersion.parse("1.21.4")));
    }
}
