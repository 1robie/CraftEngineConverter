package fr.robie.craftengineconverter.converter.bedrock.texture;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record CachedTextureInfo(Path sourcePath, String bedrockTextureKey, String bedrockTexturePath,
                                Optional<AnimationInfo> animation) {
    public CachedTextureInfo(Path sourcePath, String bedrockTextureKey, String bedrockTexturePath) {
        this(sourcePath, bedrockTextureKey, bedrockTexturePath, Optional.empty());
    }

    public String bedrockTextureDir() {
        String p = this.bedrockTexturePath;
        if (p.endsWith(".png")) {
            p = p.substring(0, p.length() - 4);
        }
        return p;
    }

    public record AnimationInfo(List<FrameInfo> frames, int frameWidth, int frameHeight, int frameRowCount) {
        public int totalFrameCount() {
            return this.frames.size();
        }

        public int defaultTickTime() {
            if (this.frames.isEmpty()) {
                return 1;
            }
            return this.frames.getFirst().time();
        }
    }

    public record FrameInfo(int index, int time) {
    }
}
