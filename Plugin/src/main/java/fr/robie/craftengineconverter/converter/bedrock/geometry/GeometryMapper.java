package fr.robie.craftengineconverter.converter.bedrock.geometry;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GeometryMapper {
    private static final float[] CENTRE_OFFSET = {8.0F, 0.0F, 8.0F};

    public BedrockGeometry mapGeometry(String identifier, JavaBlockModel model, int textureWidth, int textureHeight) {
        BedrockGeometry geo = new BedrockGeometry(identifier)
                .withVisibleBoundsWidth(4.0F)
                .withVisibleBoundsHeight(4.0F)
                .withVisibleBoundsOffset(0.0F, 0.75F, 0.0F)
                .withTextureWidth(textureWidth)
                .withTextureHeight(textureHeight);

        BedrockGeometry.Bone bone = geo.addBone("bone")
                .withBinding("q.item_slot_to_bone_name(context.item_slot)");

        for (JavaBlockModel.Element element : model.elements()) {
            this.mapElement(element, bone, textureWidth, textureHeight);
        }

        return geo;
    }

    private void mapElement(JavaBlockModel.Element element, BedrockGeometry.Bone bone, int texW, int texH) {
        float fromX = element.fromX();
        float fromY = element.fromY();
        float fromZ = element.fromZ();
        float toX = element.toX();
        float toY = element.toY();
        float toZ = element.toZ();

        float[] from = {fromX, fromY, fromZ};
        float[] to = {toX, toY, toZ};

        float[] origin = {
                Math.min(from[0], to[0]) - CENTRE_OFFSET[0],
                Math.min(from[1], to[1]) - CENTRE_OFFSET[1],
                Math.min(from[2], to[2]) - CENTRE_OFFSET[2]
        };
        float[] size = {
                Math.abs(to[0] - from[0]),
                Math.abs(to[1] - from[1]),
                Math.abs(to[2] - from[2])
        };

        origin[0] = -(origin[0] + size[0]);

        BedrockGeometry.Cube cube = bone.addCube(origin[0], origin[1], origin[2], size[0], size[1], size[2]);

        for (JavaBlockModel.Face face : element.faces()) {
            this.mapFace(face, cube, texW, texH, from, to);
        }

        element.rotation().ifPresent(rot -> {
            float[] rotOrigin = {
                    rot.ox() / 0.0625F - CENTRE_OFFSET[0],
                    rot.oy() / 0.0625F - CENTRE_OFFSET[1],
                    rot.oz() / 0.0625F - CENTRE_OFFSET[2]
            };
            rotOrigin[0] = -rotOrigin[0];
            cube.withPivot(rotOrigin[0], rotOrigin[1], rotOrigin[2]);

            float bedrockAngle = switch (rot.axis()) {
                case "x" -> -rot.angle();
                case "y" -> rot.angle();
                case "z" -> rot.angle();
                default -> rot.angle();
            };
            float[] rotVec = switch (rot.axis()) {
                case "x" -> new float[]{bedrockAngle, 0, 0};
                case "y" -> new float[]{0, bedrockAngle, 0};
                case "z" -> new float[]{0, 0, bedrockAngle};
                default -> new float[]{0, 0, 0};
            };
            cube.withRotation(rotVec[0], rotVec[1], rotVec[2]);
        });
    }

    private void mapFace(JavaBlockModel.Face face, BedrockGeometry.Cube cube, int texW, int texH,
                         float[] from, float[] to) {
        String dir = face.direction();
        float u0 = face.u0();
        float v0 = face.v0();
        float u1 = face.u1();
        float v1 = face.v1();

        boolean isUpOrDown = dir.equals("up") || dir.equals("down");

        float uvOriginU, uvOriginV, uvSizeU, uvSizeV;

        if (isUpOrDown) {
            uvOriginU = u1;
            uvOriginV = v1;
            uvSizeU = u0 - u1;
            uvSizeV = v0 - v1;
        } else {
            uvOriginU = u0;
            uvOriginV = v0;
            uvSizeU = u1 - u0;
            uvSizeV = v1 - v0;
        }

        float widthMul = (float) texW / 16.0F;
        float heightMul = (float) texH / 16.0F;
        uvOriginU *= widthMul;
        uvOriginV *= heightMul;
        uvSizeU *= widthMul;
        uvSizeV *= heightMul;

        cube.withFace(dir, uvOriginU, uvOriginV, uvSizeU, uvSizeV);
    }

    public static String getMeaningfulMaterialInstanceName(String face, String texture, int elementIndex) {
        return texture + "_" + face + "_" + elementIndex;
    }

    /**
     * @deprecated kept only as a fallback for callers that cannot supply the source image
     * (e.g. the texture file failed to load). Produces the old zero-depth flat plane.
     * Prefer {@link #createFlatItemGeometry(String, BufferedImage)}.
     */
    @Deprecated
    public static BedrockGeometry createFlatItemGeometry(String identifier, int textureWidth, int textureHeight) {
        BedrockGeometry geo = new BedrockGeometry(identifier)
                .withVisibleBoundsWidth(4.0F)
                .withVisibleBoundsHeight(4.0F)
                .withVisibleBoundsOffset(0.0F, 0.75F, 0.0F)
                .withTextureWidth(textureWidth)
                .withTextureHeight(textureHeight);

        BedrockGeometry.Bone bone = geo.addBone("bone")
                .withBinding("q.item_slot_to_bone_name(context.item_slot)")
                .withPivot(0, 8.0F, -0.25F);

        BedrockGeometry.Cube cube = bone.addCube(-8.0F, 0, -0.5F, 16.0F, 16.0F, 0);

        cube.withFace("north", textureWidth, 0, -textureWidth, textureHeight, "#layer0_north_0");
        cube.withFace("south", 0, 0, textureWidth, textureHeight, "#layer0_south_0");

        return geo;
    }

    /**
     * Builds a flat-item geometry that is extruded to a depth of 1 model unit, following the
     * exact silhouette of the texture's alpha channel instead of a single zero-depth plane.
     * <p>
     * The algorithm:
     * <ol>
     *   <li>reads every pixel of {@code texture} and classifies it opaque/transparent;</li>
     *   <li>merges the opaque pixels into the smallest practical set of axis-aligned rectangles
     *       (front/back "slabs"), each extruded to depth 1 and textured with the exact matching
     *       sub-region of the source image (mirrored on the north face, matching the previous
     *       convention);</li>
     *   <li>traces the outer silhouette of the opaque mask and emits thin boundary walls
     *       (up/down/east/west) only where a slab edge is actually exposed to air, merging
     *       consecutive exposed pixels along each edge into single quads.</li>
     * </ol>
     * No transparent pixel is ever made solid, and internal borders between two touching slabs
     * are never given a wall face, since they aren't visible.
     */
    public static BedrockGeometry createFlatItemGeometry(String identifier, BufferedImage texture) {
        int texW = texture.getWidth();
        int texH = texture.getHeight();

        BedrockGeometry geo = new BedrockGeometry(identifier)
                .withVisibleBoundsWidth(4.0F)
                .withVisibleBoundsHeight(4.0F)
                .withVisibleBoundsOffset(0.0F, 0.75F, 0.0F)
                .withTextureWidth(texW)
                .withTextureHeight(texH);

        BedrockGeometry.Bone bone = geo.addBone("bone")
                .withBinding("q.item_slot_to_bone_name(context.item_slot)")
                .withPivot(0, 8.0F, -0.25F);

        boolean[][] opaque = new boolean[texH][texW];
        for (int y = 0; y < texH; y++) {
            for (int x = 0; x < texW; x++) {
                opaque[y][x] = ((texture.getRGB(x, y) >>> 24) & 0xFF) != 0;
            }
        }

        float unitX = 16.0F / texW;
        float unitY = 16.0F / texH;
        float depth = 1.0F;
        float zOrigin = -depth / 2.0F;

        // 1) Front/back slabs: cover the opaque mask with the fewest practical rectangles.
        for (int[] rect : mergeOpaqueRegions(opaque, texW, texH)) {
            int x0 = rect[0], y0 = rect[1], x1 = rect[2], y1 = rect[3];

            float originX = -8.0F + x0 * unitX;
            float sizeX = (x1 - x0) * unitX;
            float originY = 16.0F - y1 * unitY;
            float sizeY = (y1 - y0) * unitY;

            BedrockGeometry.Cube cube = bone.addCube(originX, originY, zOrigin, sizeX, sizeY, depth);

            cube.withFace("south", x1, y0, -(x1 - x0), y1 - y0, "#layer0_south_0");
            cube.withFace("north", x0, y0, x1 - x0, y1 - y0, "#layer0_north_0");
        }

        // 2) Boundary walls: thin quads along the silhouette give the extrusion its sides.
        addBoundaryWalls(bone, opaque, texW, texH, unitX, unitY, zOrigin, depth);

        return geo;
    }

    /**
     * Greedily covers every {@code true} cell of {@code opaque} with axis-aligned rectangles:
     * for each uncovered opaque cell (scanned top-to-bottom, left-to-right) it grows as wide as
     * possible, then as tall as possible while every cell in that width remains opaque and
     * uncovered, marks the covered cells, and repeats. Returns rectangles as
     * {@code {x0, y0, x1, y1}} (x1/y1 exclusive, in pixel coordinates).
     */
    private static List<int[]> mergeOpaqueRegions(boolean[][] opaque, int w, int h) {
        boolean[][] covered = new boolean[h][w];
        List<int[]> rects = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!opaque[y][x] || covered[y][x]) continue;

                int x1 = x + 1;
                while (x1 < w && opaque[y][x1] && !covered[y][x1]) x1++;

                int y1 = y + 1;
                outer:
                while (y1 < h) {
                    for (int xx = x; xx < x1; xx++) {
                        if (!opaque[y1][xx] || covered[y1][xx]) break outer;
                    }
                    y1++;
                }

                for (int yy = y; yy < y1; yy++) {
                    for (int xx = x; xx < x1; xx++) {
                        covered[yy][xx] = true;
                    }
                }
                rects.add(new int[]{x, y, x1, y1});
            }
        }
        return rects;
    }

    /**
     * Walks the four boundary directions of the alpha mask and emits one thin wall cuboid per
     * maximal run of exposed edge pixels (a pixel edge is "exposed" when its neighbour in that
     * direction is transparent or outside the texture). Edges shared between two opaque pixels
     * are internal and never get a wall.
     */
    private static void addBoundaryWalls(BedrockGeometry.Bone bone, boolean[][] opaque, int texW, int texH,
                                         float unitX, float unitY, float zOrigin, float depth) {
        // UP walls: top edge of the shape, merged along each row.
        for (int y = 0; y < texH; y++) {
            int runStart = -1;
            for (int x = 0; x <= texW; x++) {
                boolean exposed = x < texW && opaque[y][x] && (y == 0 || !opaque[y - 1][x]);
                if (exposed && runStart == -1) {
                    runStart = x;
                } else if (!exposed && runStart != -1) {
                    addWallCube(bone, "up", runStart, x, y, y, unitX, unitY, zOrigin, depth);
                    runStart = -1;
                }
            }
        }

        // DOWN walls: bottom edge of the shape, merged along each row.
        for (int y = 0; y < texH; y++) {
            int runStart = -1;
            for (int x = 0; x <= texW; x++) {
                boolean exposed = x < texW && opaque[y][x] && (y == texH - 1 || !opaque[y + 1][x]);
                if (exposed && runStart == -1) {
                    runStart = x;
                } else if (!exposed && runStart != -1) {
                    addWallCube(bone, "down", runStart, x, y, y, unitX, unitY, zOrigin, depth);
                    runStart = -1;
                }
            }
        }

        // WEST walls: left edge of the shape, merged along each column.
        for (int x = 0; x < texW; x++) {
            int runStart = -1;
            for (int y = 0; y <= texH; y++) {
                boolean exposed = y < texH && opaque[y][x] && (x == 0 || !opaque[y][x - 1]);
                if (exposed && runStart == -1) {
                    runStart = y;
                } else if (!exposed && runStart != -1) {
                    addWallCube(bone, "west", x, x, runStart, y, unitX, unitY, zOrigin, depth);
                    runStart = -1;
                }
            }
        }

        // EAST walls: right edge of the shape, merged along each column.
        for (int x = 0; x < texW; x++) {
            int runStart = -1;
            for (int y = 0; y <= texH; y++) {
                boolean exposed = y < texH && opaque[y][x] && (x == texW - 1 || !opaque[y][x + 1]);
                if (exposed && runStart == -1) {
                    runStart = y;
                } else if (!exposed && runStart != -1) {
                    addWallCube(bone, "east", x, x, runStart, y, unitX, unitY, zOrigin, depth);
                    runStart = -1;
                }
            }
        }
    }

    /**
     * Adds a single zero-thickness (in the in-plane axis) wall cuboid for one merged boundary
     * run and gives it just the one face it exists for. For "up"/"down" the run is horizontal
     * ({@code x0..x1} at fixed row {@code y0==y1}); for "west"/"east" the run is vertical
     * ({@code y0..y1} at fixed column {@code x0==x1}). The UV samples the one row/column of
     * source pixels the wall sits against, stretched across the depth - the same trick used for
     * block-side shading from a single top-down texture.
     */
    private static void addWallCube(BedrockGeometry.Bone bone, String direction,
                                    int x0, int x1, int y0, int y1,
                                    float unitX, float unitY, float zOrigin, float depth) {
        float originX, originY, sizeX, sizeY;
        float uvU, uvV, uvSizeU, uvSizeV;

        switch (direction) {
            case "up" -> {
                originX = -8.0F + x0 * unitX;
                sizeX = (x1 - x0) * unitX;
                originY = 16.0F - y0 * unitY; // top edge of row y0
                sizeY = 0.0F;
                uvU = x0; uvV = y0; uvSizeU = (x1 - x0); uvSizeV = 1;
            }
            case "down" -> {
                originX = -8.0F + x0 * unitX;
                sizeX = (x1 - x0) * unitX;
                originY = 16.0F - (y0 + 1) * unitY; // bottom edge of row y0
                sizeY = 0.0F;
                uvU = x0; uvV = y0; uvSizeU = (x1 - x0); uvSizeV = 1;
            }
            case "west" -> {
                originX = -8.0F + x0 * unitX; // left edge of column x0
                sizeX = 0.0F;
                originY = 16.0F - y1 * unitY;
                sizeY = (y1 - y0) * unitY;
                uvU = x0; uvV = y0; uvSizeU = 1; uvSizeV = (y1 - y0);
            }
            case "east" -> {
                originX = -8.0F + (x0 + 1) * unitX; // right edge of column x0
                sizeX = 0.0F;
                originY = 16.0F - y1 * unitY;
                sizeY = (y1 - y0) * unitY;
                uvU = x0; uvV = y0; uvSizeU = 1; uvSizeV = (y1 - y0);
            }
            default -> {
                return;
            }
        }

        BedrockGeometry.Cube wall = bone.addCube(originX, originY, zOrigin, sizeX, sizeY, depth);
        wall.withFace(direction, uvU, uvV, uvSizeU, uvSizeV, "#layer0_south_0");
    }
}