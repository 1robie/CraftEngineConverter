package fr.robie.craftengineconverter.api.configuration.bedrock.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FlipbookTextureData {
    private final String atlasTile;
    private final String flipbookTexture;
    private final int ticksPerFrame;
    private final List<Integer> frames;
    private final int replicate;
    private final boolean blendFrames;

    public FlipbookTextureData(
            @NotNull String atlasTile,
            @NotNull String flipbookTexture,
            int ticksPerFrame,
            @NotNull List<Integer> frames,
            int replicate,
            boolean blendFrames
    ) {
        this.atlasTile = atlasTile;
        this.flipbookTexture = flipbookTexture;
        this.ticksPerFrame = ticksPerFrame;
        this.frames = frames;
        this.replicate = replicate;
        this.blendFrames = blendFrames;
    }

    public String atlasTile() { return this.atlasTile; }
    public String flipbookTexture() { return this.flipbookTexture; }
    public String getAtlasTile() {
        return this.atlasTile;
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("atlas_tile", this.atlasTile);
        obj.addProperty("flipbook_texture", this.flipbookTexture);
        obj.addProperty("ticks_per_frame", this.ticksPerFrame);
        JsonArray frameArray = new JsonArray();
        for (int f : this.frames) {
            frameArray.add(f);
        }
        obj.add("frames", frameArray);
        obj.addProperty("replicate", this.replicate);
        obj.addProperty("blend_frames", this.blendFrames);
        return obj;
    }
}
