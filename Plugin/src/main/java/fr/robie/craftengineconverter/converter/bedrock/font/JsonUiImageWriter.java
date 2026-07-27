package fr.robie.craftengineconverter.converter.bedrock.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.messageflow.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Emits the JSON-UI needed to display a Java font image at its true pixel size on Bedrock chest
 * containers.
 * <p>
 * Bedrock cannot render a font glyph larger than one character box, so tall glyphs (GUI overlays)
 * cannot go through {@code font/glyph_XX.png} at all. JSON-UI is the only mechanism that can draw an
 * arbitrary image at an arbitrary size — but it has <b>no font or glyph hook whatsoever</b>. The only
 * string tooling available is the arithmetic operator set, so "does this text contain my marker?" has
 * to be spelled as delete-the-substring-and-see-if-it-changed:
 * <pre>(not ($atext - '&#x4E00;' = $atext))</pre>
 * The marker character itself is never replaced inline — it still occupies its place in the container
 * title and simply renders as nothing.
 * <p>
 * <b>The title text comes from {@code $container_title}, which is a {@code $} variable, not a
 * {@code #} binding.</b> The engine injects it at screen level and it propagates down the tree by
 * template substitution, which makes it invisible to the {@code #}-binding data model. Visibility is
 * therefore a plain {@code visible} <i>property</i> holding the expression directly — there is no
 * {@code bindings} array and no {@code "visible": false} default, because a property expression always
 * evaluates. Routing {@code $container_title} through a {@code binding_type: "view"} entry instead is
 * the one thing that silently never resolves, and is what makes container screens look impossible.
 * {@code $container_title} is also aliased through {@code $atext} first, and the marker is
 * <b>single-quoted</b> inside the expression; both are required.
 * <p>
 * <b>Structure follows {@code concepts/chest_screen_clean.json}</b>, a known-working pack. Rather than
 * patching vanilla with {@code modifications}, the four controls that need to change are redefined
 * outright in the {@code chest} namespace. Bedrock merges pack UI files with vanilla per top-level key,
 * so redefining {@code small_chest_panel} / {@code large_chest_panel} replaces just those two controls
 * and every other vanilla control in the file survives untouched. Image controls are written inline
 * into {@code root_panel.controls} — no wrapper panel, no template, no separate namespace file, and
 * hence no {@code _ui_defs.json} entry.
 * <p>
 * The two screen controls are redefined as well, to gate the layout on a marker: pocket devices
 * normally render {@code pocket_containers.*}, which carries none of these images. With the gate, a
 * pocket player opening a marked container gets the desktop layout (and the image); an unmarked
 * container is left on the stock pocket layout.
 * <p>
 * Only {@code ui/chest_screen.json} is produced. Server forms and the HUD title were previously also
 * targeted; both were removed, as the HUD factory insert failed to resolve at runtime
 * ({@code Type not specified (or @-base not found)}) and neither surface is where CraftEngine renders
 * its GUIs.
 * <p>
 * <b>Placement is approximate.</b> Java positions these overlays with {@code ascent} plus a run of
 * negative-space offset characters for horizontal nudging. Those offset characters have no Bedrock
 * equivalent and are dropped, so horizontal placement information is simply gone; the vertical
 * {@code offset} of {@code -ascent} with the default centre anchoring is a deterministic default, not a
 * faithful reproduction. Note also that JSON-UI is unversioned and deprecated in favour of Ore UI, so
 * generated UI can break on any Bedrock update.
 */
final class JsonUiImageWriter {
    // Above chest_panel (layer 5) so the overlay covers the slot grid; CraftEngine artwork is
    // transparent where the slots are. Matches the reference pack.
    private static final int IMAGE_LAYER = 11;

    // Where the container title sits, measured from the top of the chest window. Bedrock does not put
    // it where Java does — Java's AbstractContainerScreen uses titleLabelY = 6, whereas vanilla
    // chest_screen.json stacks chest_label's offset (-1) onto its parent panel's (12 small / 11 large).
    // Anchoring to Java's value renders every overlay 4-5px too high, so these follow Bedrock.
    // Raise both to push every image further down.
    private static final int TITLE_TOP_Y_SMALL = 12 - 1;
    private static final int TITLE_TOP_Y_LARGE = 11 - 1;
    // Bedrock's default UI font is 8px tall with a 7px ascent, so its baseline is 7 below the label top.
    private static final int FONT_ASCENT = 7;

    /**
     * One tall glyph destined for JSON-UI rather than a glyph page.
     *
     * @param codePoint   the character that triggers the image
     * @param sourcePng   the Java source PNG to copy into the pack
     * @param texturePath the Bedrock texture reference, without the {@code .png} extension
     * @param srcX        cell rect within {@code sourcePng} — only emitted as {@code uv} when the
     * @param srcY        cell is a sub-rect of a larger multi-character sheet
     * @param height      the provider's Java {@code height}, in logical font pixels
     * @param ascent      the provider's Java {@code ascent}, in logical font pixels
     */
    record UiImage(int codePoint, Path sourcePng, String texturePath,
                   int srcX, int srcY, int srcW, int srcH,
                   int fullW, int fullH, int height, int ascent) {

        boolean isWholeImage() {
            return this.srcX == 0 && this.srcY == 0
                    && this.srcW == this.fullW && this.srcH == this.fullH;
        }

        String controlName() {
            return String.format("craftengine_image_%04X", this.codePoint);
        }

        /** The marker character this glyph reacts to. */
        String marker() {
            return new String(Character.toChars(this.codePoint));
        }
    }

    void write(Collection<UiImage> images, Path packDir, Path texturesDir) {
        if (images.isEmpty()) return;

        Path uiDir = packDir.resolve("ui");
        try {
            Files.createDirectories(uiDir);
        } catch (IOException e) {
            Logger.error("Failed to create ui output directory", e);
            return;
        }

        int copied = copyTextures(images, texturesDir);
        FileCacheManager.saveJsonToFile(uiDir.resolve("chest_screen.json"), buildChestScreen(images));

        Logger.info("Saved " + images.size() + " JSON-UI image(s) to ui/chest_screen.json ("
                + copied + " texture(s) copied)");
    }

    // Several glyphs can share one sheet, so copy each distinct source only once.
    private int copyTextures(Collection<UiImage> images, Path texturesDir) {
        Set<Path> done = new LinkedHashSet<>();
        int copied = 0;
        for (UiImage image : images) {
            if (!done.add(image.sourcePng().toAbsolutePath())) continue;
            // texturePath is pack-root relative and extension-less, e.g.
            // "textures/minecraft/font/gui/custom/item_browser"
            String relative = image.texturePath().substring("textures/".length()) + ".png";
            Path dest = texturesDir.resolve(relative);
            try {
                Files.createDirectories(dest.getParent());
                Files.copy(image.sourcePng(), dest, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                Logger.error("Failed to copy font image " + image.sourcePng(), e);
            }
        }
        return copied;
    }

    private JsonObject buildChestScreen(Collection<UiImage> images) {
        JsonObject root = new JsonObject();
        root.addProperty("namespace", "chest");

        root.add("small_chest_panel", buildChestPanel(images, false));
        root.add("large_chest_panel", buildChestPanel(images, true));

        // A $ variable holding the whole boolean, referenced from each variables entry's "requires" —
        // the same shape the reference pack uses for its own marker test.
        String markerTest = buildMarkerTest(images);
        root.add("small_chest_screen@common.inventory_screen_common",
                buildScreen(markerTest, "chest.small_chest_panel", "pocket_containers.small_chest_panel"));
        root.add("large_chest_screen@common.inventory_screen_common",
                buildScreen(markerTest, "chest.large_chest_panel", "pocket_containers.large_chest_panel"));
        return root;
    }

    /**
     * Vanilla's chest panel, rebuilt verbatim, with the image controls appended to
     * {@code root_panel.controls}. Only the two entries that differ between the small and large
     * variants are branched on; everything else is identical in both.
     */
    private JsonObject buildChestPanel(Collection<UiImage> images, boolean large) {
        String prefix = large ? "large" : "small";

        JsonArray chestControls = new JsonArray();
        chestControls.add(ref(prefix + "_chest_panel_top_half@chest." + prefix + "_chest_panel_top_half"));
        chestControls.add(ref("inventory_panel_bottom_half_with_label@common.inventory_panel_bottom_half_with_label"));
        chestControls.add(ref("hotbar_grid@common.hotbar_grid_template"));
        chestControls.add(ref("inventory_take_progress_icon_button@common.inventory_take_progress_icon_button"));
        JsonObject flying = new JsonObject();
        flying.addProperty("layer", 15);
        chestControls.add(named("flying_item_renderer@common.flying_item_renderer", flying));

        JsonObject chestPanel = new JsonObject();
        chestPanel.addProperty("type", "panel");
        chestPanel.addProperty("layer", 5);
        chestPanel.add("controls", chestControls);

        JsonArray rootControls = new JsonArray();
        rootControls.add(ref("common_panel@common.common_panel"));
        rootControls.add(named("chest_panel", chestPanel));
        rootControls.add(ref("inventory_selected_icon_button@common.inventory_selected_icon_button"));
        rootControls.add(ref("gamepad_cursor@common.gamepad_cursor_button"));
        // The overlays go last so they render above their siblings at equal layer.
        int titleTopY = large ? TITLE_TOP_Y_LARGE : TITLE_TOP_Y_SMALL;
        for (UiImage image : images) {
            rootControls.add(named(image.controlName(), buildImageControl(image, titleTopY)));
        }

        JsonObject rootPanel = new JsonObject();
        if (large) rootPanel.add("size", vector(176, 220));
        rootPanel.addProperty("layer", 1);
        rootPanel.add("controls", rootControls);

        JsonArray outer = new JsonArray();
        outer.add(ref("container_gamepad_helpers@common.container_gamepad_helpers"));
        if (large) {
            JsonObject details = new JsonObject();
            details.addProperty("control_name", "@chest.selected_item_details");
            outer.add(named("selected_item_details_factory@common.selected_item_details_factory", details));
            JsonObject lock = new JsonObject();
            lock.addProperty("control_name", "@common.item_lock_notification");
            outer.add(named("item_lock_notification_factory@common.item_lock_notification_factory", lock));
        } else {
            outer.add(ref("selected_item_details_factory@common.selected_item_details_factory"));
            outer.add(ref("item_lock_notification_factory@common.item_lock_notification_factory"));
        }
        outer.add(named("root_panel@common.root_panel", rootPanel));

        JsonObject panel = new JsonObject();
        panel.addProperty("type", "panel");
        panel.add("controls", outer);
        return panel;
    }

    /**
     * One overlay, positioned to match where Java draws it.
     * <p>
     * Java scales the source bitmap to {@code height} logical pixels tall and places its top edge
     * {@code ascent} pixels above the container title's baseline. Measured from the top of the chest
     * window, where {@code titleTopY} is Bedrock's own label position for this panel size:
     * <pre>image_top = titleTopY + FONT_ASCENT - ascent</pre>
     * Anchoring {@code top_middle} to {@code top_middle} makes that the offset directly, and keeps the
     * result independent of the parent's height — which matters because the small and large chest
     * panels differ in both title position and window height. (The reference pack encodes an equivalent
     * position centre-anchored: for its {@code ascent} 73 / 256px overlay,
     * {@code -60 + 128 - 110 = -42}, its actual offset.)
     * <p>
     * Horizontal placement is <b>not</b> recoverable — Java nudges these with negative-space offset
     * characters that have no Bedrock equivalent and are dropped upstream — so the overlay is simply
     * centred.
     */
    private JsonObject buildImageControl(UiImage image, int titleTopY) {
        // Java renders the bitmap at `height` logical pixels tall, whatever the source resolution.
        int displayH = image.height();
        int displayW = Math.max(1, Math.round((float) image.srcW() * displayH / image.srcH()));

        JsonObject control = new JsonObject();
        control.addProperty("type", "image");
        control.addProperty("texture", image.texturePath());
        control.addProperty("layer", IMAGE_LAYER);
        control.add("size", vector(displayW, displayH));
        control.addProperty("anchor_from", "top_middle");
        control.addProperty("anchor_to", "top_middle");
        control.add("offset", vector(0, titleTopY + FONT_ASCENT - image.ascent()));
        control.addProperty("$atext", "$container_title");
        control.addProperty("visible", "(not ($atext - '" + image.marker() + "' = $atext))");

        // Only a glyph taken from a multi-character sheet needs the texture cropped.
        if (!image.isWholeImage()) {
            control.add("uv", vector(image.srcX(), image.srcY()));
            control.add("uv_size", vector(image.srcW(), image.srcH()));
            control.addProperty("keep_ratio", false);
        }
        return control;
    }

    /**
     * Vanilla's screen control with its {@code variables} gate widened: the desktop layout is selected
     * whenever the container title carries one of our markers, and the pocket layout only when it does
     * not. Without this a pocket player would get {@code pocket_containers.*}, which has no overlay.
     */
    private JsonObject buildScreen(String markerTest, String screenContent, String pocketContent) {
        JsonObject desktop = new JsonObject();
        desktop.addProperty("requires", "($desktop_screen or $craftengine_marker)");
        desktop.addProperty("$screen_content", screenContent);
        desktop.addProperty("$screen_bg_content", "common.screen_background");
        desktop.addProperty("$screen_background_alpha", 0.4);

        JsonObject pocket = new JsonObject();
        pocket.addProperty("requires", "($pocket_screen and (not $craftengine_marker))");
        pocket.addProperty("$use_custom_pocket_toast", true);
        pocket.addProperty("$screen_content", pocketContent);

        JsonArray variables = new JsonArray();
        variables.add(desktop);
        variables.add(pocket);

        JsonObject screen = new JsonObject();
        screen.addProperty("$craftengine_marker", markerTest);
        screen.addProperty("$close_on_player_hurt|default", true);
        screen.addProperty("$use_custom_pocket_toast|default", false);
        screen.addProperty("close_on_player_hurt", "$close_on_player_hurt");
        screen.addProperty("use_custom_pocket_toast", "$use_custom_pocket_toast");
        screen.add("variables", variables);
        return screen;
    }

    // "contains" is a real JSON-UI operator; the reference pack gates its own layout the same way.
    private static String buildMarkerTest(Collection<UiImage> images) {
        StringBuilder test = new StringBuilder("(");
        boolean first = true;
        for (UiImage image : images) {
            if (!first) test.append(" or ");
            test.append("($container_title contains '").append(image.marker()).append("')");
            first = false;
        }
        return test.append(")").toString();
    }

    /** A controls-array entry that only references a template, with no overrides. */
    private static JsonObject ref(String controlName) {
        return named(controlName, new JsonObject());
    }

    /** A controls-array entry wrapping one named control. */
    private static JsonObject named(String controlName, JsonObject body) {
        JsonObject entry = new JsonObject();
        entry.add(controlName, body);
        return entry;
    }

    private static JsonArray vector(int x, int y) {
        JsonArray vec = new JsonArray();
        vec.add(x);
        vec.add(y);
        return vec;
    }
}
