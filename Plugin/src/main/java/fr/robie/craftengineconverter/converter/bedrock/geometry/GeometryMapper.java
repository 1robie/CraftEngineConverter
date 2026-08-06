package fr.robie.craftengineconverter.converter.bedrock.geometry;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GeometryMapper {
    private static final float[] CENTRE_OFFSET = {8.0F, 0.0F, 8.0F};

    /**
     * The point a held item's pose animation turns it about: the model's own centre.
     * <p>
     * Java applies a {@code display} transform and then shifts the model by half a block, so the transform pivots
     * on model coordinate {@code (8, 8, 8)} — the centre. Cubes here are written at Java minus
     * {@link #CENTRE_OFFSET}, which puts that centre at {@code (0, 8, 0)} in bone space.
     * <p>
     * It has to be the same for every item geometry. The Java-model path used to set no pivot at all while the
     * generated flat and lattice geometries set {@code (0, 8, -0.25)}, so one animation posed a 3D item and a 2D
     * item about two different points.
     */
    private static final float[] ITEM_PIVOT = {0.0F, 8.0F, 0.0F};

    public BedrockGeometry mapGeometry(String identifier, JavaBlockModel model, int textureWidth, int textureHeight) {
        BedrockGeometry geo = new BedrockGeometry(identifier)
                .withVisibleBoundsWidth(4.0F)
                .withVisibleBoundsHeight(4.0F)
                .withVisibleBoundsOffset(0.0F, 0.75F, 0.0F)
                .withTextureWidth(textureWidth)
                .withTextureHeight(textureHeight);

        BedrockGeometry.Bone bone = geo.addBone("bone")
                .withBinding("q.item_slot_to_bone_name(context.item_slot)")
                .withPivot(ITEM_PIVOT[0], ITEM_PIVOT[1], ITEM_PIVOT[2]);

        for (JavaBlockModel.Element element : model.elements()) {
            this.mapElement(element, bone, textureWidth, textureHeight);
        }

        return geo;
    }

    /**
     * The same conversion as {@link #mapGeometry}, for a <b>block</b> rather than a held item.
     * <p>
     * Two differences, both small and both necessary. The bone carries no binding: {@code item_slot_to_bone_name}
     * is an attachable query and means nothing on a block. And each face is given a material instance named after
     * the Java texture variable it uses, because that is how a block points its faces at different textures — a
     * log's {@code end} on the top and bottom, {@code side} around it.
     *
     * @param instanceNames receives, per face, the Java texture variable that face samples (e.g. {@code "end"}),
     *                      so the caller can build the matching {@code material_instances}
     */
    public BedrockGeometry mapBlockGeometry(String identifier, JavaBlockModel model,
                                            java.util.Set<String> instanceNames) {
        BedrockGeometry geo = new BedrockGeometry(identifier)
                .withTextureWidth((int) JavaBlockModel.UV_SPACE)
                .withTextureHeight((int) JavaBlockModel.UV_SPACE);

        BedrockGeometry.Bone bone = geo.addBone("bone");

        for (JavaBlockModel.Element element : model.elements()) {
            this.mapElement(element, bone, (int) JavaBlockModel.UV_SPACE, (int) JavaBlockModel.UV_SPACE,
                    model, instanceNames, true);
        }

        return geo;
    }

    /**
     * A model with a blockstate's {@code x}/{@code y} rotation <b>baked into the cubes</b>.
     * <p>
     * The alternative is Geyser's {@code transformation}, and it cannot be trusted for a combined rotation: Java
     * applies {@code x} then {@code y}, and composing the two in the other order negates the {@code y} turn. That is
     * why a stair was right on two facings and showed its back on the other two — only its {@code half=top} variants
     * carry {@code x: 180}. Baking removes the ordering question entirely, and as a bonus reproduces {@code uvlock}
     * (which Geyser has no field for) because each face keeps the UV it was authored with.
     * <p>
     * Exact, because a blockstate rotation is always a multiple of 90 degrees: a box maps onto a box, and a face
     * normal onto another axis.
     */
    public BedrockGeometry mapRotatedBlockGeometry(String identifier, JavaBlockModel model,
                                                   java.util.Set<String> instanceNames, int rotX, int rotY) {
        BedrockGeometry geo = new BedrockGeometry(identifier)
                .withTextureWidth((int) JavaBlockModel.UV_SPACE)
                .withTextureHeight((int) JavaBlockModel.UV_SPACE);

        BedrockGeometry.Bone bone = geo.addBone("bone");
        for (JavaBlockModel.Element element : rotateModel(model, rotX, rotY).elements()) {
            this.mapElement(element, bone,
                    (int) JavaBlockModel.UV_SPACE, (int) JavaBlockModel.UV_SPACE, model, instanceNames, true);
        }
        return geo;
    }

    /**
     * The same model with every element turned, so callers that need the rotated <b>bounds</b> — the collision and
     * selection boxes — measure exactly the shape that was drawn instead of repeating the rotation themselves.
     */
    public static JavaBlockModel rotateModel(JavaBlockModel model, int rotX, int rotY) {
        if (rotX == 0 && rotY == 0) return model;
        JavaBlockModel rotated = new JavaBlockModel(model.parent().orElse(null), model.ambientOcclusion());
        rotated.setGuiLightFront(model.guiLightFront());
        model.textures().forEach(rotated::addTexture);
        for (JavaBlockModel.Element element : model.elements()) {
            rotated.addElement(rotateElement(element, rotX, rotY));
        }
        return rotated;
    }

    /** Turns one element about the block centre, carrying each face round to the direction it now points. */
    private static JavaBlockModel.Element rotateElement(JavaBlockModel.Element element, int rotX, int rotY) {
        if (rotX == 0 && rotY == 0) return element;

        float[] from = {element.fromX(), element.fromY(), element.fromZ()};
        float[] to = {element.toX(), element.toY(), element.toZ()};

        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float x : new float[]{from[0], to[0]}) {
            for (float y : new float[]{from[1], to[1]}) {
                for (float z : new float[]{from[2], to[2]}) {
                    float[] p = {x - 8.0F, y - 8.0F, z - 8.0F};
                    rotate(p, rotX, rotY);
                    for (int axis = 0; axis < 3; axis++) {
                        min[axis] = Math.min(min[axis], p[axis] + 8.0F);
                        max[axis] = Math.max(max[axis], p[axis] + 8.0F);
                    }
                }
            }
        }

        JavaBlockModel.Element rotated = new JavaBlockModel.Element(min[0], min[1], min[2], max[0], max[1], max[2]);
        for (JavaBlockModel.Face face : element.faces()) {
            rotated.addFace(rotatedDirection(face.direction(), rotX, rotY), face.texture(),
                    face.u0(), face.v0(), face.u1(), face.v1(),
                    rotatedFaceRotation(face.direction(), face.rotation(), rotY), face.tintIndex());
        }
        // The element's own rotation goes round with it. Its pivot is a point like any other, and its axis maps onto
        // another axis because the turn is a multiple of 90 degrees; when that axis comes out pointing the other way
        // the angle has to change sign with it, or the part would tilt the wrong way.
        element.rotation().ifPresent(r -> {
            float[] pivot = {r.ox() - 8.0F, r.oy() - 8.0F, r.oz() - 8.0F};
            rotate(pivot, rotX, rotY);
            float[] axis = switch (r.axis()) {
                case "x" -> new float[]{1, 0, 0};
                case "y" -> new float[]{0, 1, 0};
                default -> new float[]{0, 0, 1};
            };
            rotate(axis, rotX, rotY);
            int dominant = 0;
            for (int i = 1; i < 3; i++) if (Math.abs(axis[i]) > Math.abs(axis[dominant])) dominant = i;
            String newAxis = switch (dominant) {
                case 0 -> "x";
                case 1 -> "y";
                default -> "z";
            };
            float angle = axis[dominant] < 0 ? -r.angle() : r.angle();
            rotated.setRotation(pivot[0] + 8.0F, pivot[1] + 8.0F, pivot[2] + 8.0F, angle, newAxis, r.rescale());
        });
        return rotated;
    }

    /**
     * A face's texture rotation after a variant's Y rotation is baked in.
     * <p>
     * Turning a block about Y carries the four side faces onto one another, and each keeps its own texture upright
     * as it goes — so their {@code rotation} is unchanged. The top and bottom faces are the exception: the axis of
     * the turn runs through them, so the turn happens <b>within</b> the face's own plane and shows up as a quarter
     * turn of its texture. Down turns the opposite way to up, since it is seen from the other side.
     * <p>
     * Without this a glazed-terracotta-style block shows the same pattern orientation on top for all four of its
     * facings, instead of the pattern following the block round.
     */
    public static int rotatedFaceRotation(String direction, int faceRotation, int rotY) {
        int turn = switch (direction) {
            case "up" -> rotY;
            case "down" -> -rotY;
            default -> 0;
        };
        return ((faceRotation + turn) % 360 + 360) % 360;
    }

    /**
     * Where a face points after the rotation, found by turning its normal and snapping to the nearest axis. Derived
     * rather than tabulated so it cannot disagree with how the corners moved.
     */
    public static String rotatedDirection(String direction, int rotX, int rotY) {
        float[] normal = switch (direction) {
            case "north" -> new float[]{0, 0, -1};
            case "south" -> new float[]{0, 0, 1};
            case "west" -> new float[]{-1, 0, 0};
            case "east" -> new float[]{1, 0, 0};
            case "up" -> new float[]{0, 1, 0};
            case "down" -> new float[]{0, -1, 0};
            default -> null;
        };
        if (normal == null) return direction;

        rotate(normal, rotX, rotY);
        int axis = 0;
        for (int i = 1; i < 3; i++) {
            if (Math.abs(normal[i]) > Math.abs(normal[axis])) axis = i;
        }
        boolean positive = normal[axis] > 0;
        return switch (axis) {
            case 0 -> positive ? "east" : "west";
            case 1 -> positive ? "up" : "down";
            default -> positive ? "south" : "north";
        };
    }

    /**
     * X then Y, the order a blockstate applies them in, and <b>negated</b>.
     * <p>
     * Vanilla builds a variant's rotation as {@code rotationYXZ(-y, -x, 0)}, so a blockstate's angles turn the model
     * the opposite way round to a plain right-handed rotation. Two checks against vanilla agree: {@code block/stairs}
     * has its raised step on the east and is the {@code facing=east} variant, while {@code facing=south} is
     * {@code y: 90} — so {@code y: 90} must carry east to south, which only the negated form does. Applying them
     * as given carried east to north, a 180-degree error on exactly two of the four facings, which is why a stair
     * showed its back on two of them.
     */
    private static void rotate(float[] p, int rotX, int rotY) {
        if (rotX != 0) {
            double a = Math.toRadians(-rotX);
            double y = p[1] * Math.cos(a) - p[2] * Math.sin(a);
            double z = p[1] * Math.sin(a) + p[2] * Math.cos(a);
            p[1] = (float) y;
            p[2] = (float) z;
        }
        if (rotY != 0) {
            double a = Math.toRadians(-rotY);
            double x = p[0] * Math.cos(a) + p[2] * Math.sin(a);
            double z = -p[0] * Math.sin(a) + p[2] * Math.cos(a);
            p[0] = (float) x;
            p[2] = (float) z;
        }
    }

    private void mapElement(JavaBlockModel.Element element, BedrockGeometry.Bone bone, int texW, int texH) {
        this.mapElement(element, bone, texW, texH, null, null, true);
    }

    /**
     * @param mirrorX whether to mirror the element along X.
     *                <p>
     *                True for a held item, because Bedrock entity models are authored mirrored relative to Java.
     *                <b>False for a block</b>, which sits in world space where Bedrock's axes match Java's —
     *                mirroring one moves it to the opposite side of its own block and reverses its texture, which
     *                is what put doors in the wrong half of their frame and crossed a flower's 45-degree sheets the
     *                wrong way. A symmetric shape such as a cactus hides the difference entirely.
     */
    private void mapElement(JavaBlockModel.Element element, BedrockGeometry.Bone bone, int texW, int texH,
                            JavaBlockModel model, java.util.Set<String> instanceNames, boolean mirrorX) {
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

        if (mirrorX) origin[0] = -(origin[0] + size[0]);

        BedrockGeometry.Cube cube = bone.addCube(origin[0], origin[1], origin[2], size[0], size[1], size[2]);

        for (JavaBlockModel.Face face : element.faces()) {
            String instance = model == null ? null : materialInstanceFor(face, model);
            if (instance != null && instanceNames != null) instanceNames.add(instance);
            this.mapFace(face, cube, texW, texH, from, to, instance, mirrorX);
        }

        element.rotation().ifPresent(rot -> {
            // A Java rotation origin is already in the same 0-16 model units as from/to, so it only needs
            // recentring — dividing by 1/16 multiplied it by 16 and threw the pivot far outside the model,
            // which visibly flung any rotated cube away from the rest of the item.
            float[] rotOrigin = {
                    rot.ox() - CENTRE_OFFSET[0],
                    rot.oy() - CENTRE_OFFSET[1],
                    rot.oz() - CENTRE_OFFSET[2]
            };
            if (mirrorX) rotOrigin[0] = -rotOrigin[0];
            cube.withPivot(rotOrigin[0], rotOrigin[1], rotOrigin[2]);

            // A rotation carried through a mirror is the mirror's conjugate of it, and for the X mirror used here
            // (x -> -x) that has one answer per axis: turning about X is unchanged, because the mirror plane
            // contains the X axis and the two commute, while turning about Y or Z reverses, because the mirror
            // flips the sense of the plane each sweeps. Negating X instead — as this did while compensating for
            // Blockbench's export rather than for our own transform — tilts a rotated part the wrong way about the
            // one axis that should have been left alone.
            float bedrockAngle = switch (rot.axis()) {
                case "y", "z" -> mirrorX ? -rot.angle() : rot.angle();
                default -> rot.angle();
            };
            float[] rotVec = switch (rot.axis()) {
                case "x" -> new float[]{bedrockAngle, 0, 0};
                case "y" -> new float[]{0, bedrockAngle, 0};
                case "z" -> new float[]{0, 0, bedrockAngle};
                default -> new float[]{0, 0, 0};
            };
            cube.withRotation(rotVec[0], rotVec[1], rotVec[2]);

            if (rot.rescale()) {
                float scaleFactor = 1.0F / (float) Math.cos(Math.toRadians(rot.angle()));
                float minDim = Math.min(size[0], Math.min(size[1], size[2]));
                float inflate = (scaleFactor - 1.0F) * minDim / 2.0F;
                if (inflate > 0.001F) cube.withInflate(inflate);
            }
        });
    }

    /**
     * The material instance a block face should use: the Java texture variable it names, with the {@code #}
     * dropped. A face naming a literal texture rather than a variable has no instance to share and returns null.
     */
    private static String materialInstanceFor(JavaBlockModel.Face face, JavaBlockModel model) {
        String texture = face.texture();
        if (texture == null || !texture.startsWith("#")) return null;

        String key = texture.substring(1);
        // Follow the variable to make sure it actually binds to something; the name of the first bound key is
        // what the material instance is called.
        String value = model.textures().get(key);
        for (int hop = 0; hop < 8 && value != null && value.startsWith("#"); hop++) {
            key = value.substring(1);
            value = model.textures().get(key);
        }
        return value == null ? null : key;
    }

    private void mapFace(JavaBlockModel.Face face, BedrockGeometry.Cube cube, int texW, int texH,
                         float[] from, float[] to) {
        this.mapFace(face, cube, texW, texH, from, to, null);
    }

    private void mapFace(JavaBlockModel.Face face, BedrockGeometry.Cube cube, int texW, int texH,
                         float[] from, float[] to, String materialInstance) {
        this.mapFace(face, cube, texW, texH, from, to, materialInstance, false);
    }

    /**
     * @param mirrorX whether the cube this face belongs to was mirrored along X, in which case the face has to be
     *                mirrored with it. Mirroring is not just a position change: the sides that faced east and west
     *                have swapped places, and every face keeps its own left and right, so its U must run the other
     *                way. Moving the cube and leaving its faces alone is a <b>half mirror</b>, and its symptoms are
     *                on record — the comment that removed block mirroring in the first place described a shape that
     *                "moves to the opposite side of its own block and reverses its texture", which is exactly what a
     *                half mirror does. That is why this takes the flag rather than the caller mirroring positions
     *                alone.
     */
    private void mapFace(JavaBlockModel.Face face, BedrockGeometry.Cube cube, int texW, int texH,
                         float[] from, float[] to, String materialInstance, boolean mirrorX) {
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

        int uvRotation = ((face.rotation() % 360) + 360) % 360;

        // The UV is deliberately left alone, even though the cube is mirrored. Bedrock mirrors a geometry's
        // positions and then samples each face's UV rect as authored, so reversing U here mirrors the texture a
        // second time and it comes out back to front.
        //
        // Vanilla's doors are the proof, because they encode a hinge side in nothing but UV direction: geometry
        // identical, and door_bottom_left's west face reads [0,0,16,16] where door_bottom_right's reads
        // [16,0,0,16]. Flipping U turned every left-hinged door into a right-hinged one. Symmetric textures such
        // as planks hide this completely, which is why fence gates and stairs looked correct either way.

        // Java UVs span 0-16 whatever the texture's resolution (see JavaBlockModel.UV_SPACE), so they are
        // rescaled into whatever space this geometry declares.
        float widthMul = texW / JavaBlockModel.UV_SPACE;
        float heightMul = texH / JavaBlockModel.UV_SPACE;
        uvOriginU *= widthMul;
        uvOriginV *= heightMul;
        uvSizeU *= widthMul;
        uvSizeV *= heightMul;

        cube.withFace(dir, uvOriginU, uvOriginV, uvSizeU, uvSizeV, materialInstance, uvRotation);
    }

    // No mirroredDirection here on purpose: a face keeps its authored name through the mirror.
    //
    // Swapping east and west looked right — the sides do change places geometrically — but Bedrock's face names are
    // absolute, naming the world direction the face ends up pointing, so renaming them mirrors the block a second
    // time. Doors proved it by facing: their panel is thin in X, so its large faces are west and east, and a swap
    // exchanges exactly the two faces whose UVs carry the hinge. At y=0 and y=180 (facing east and west) those
    // faces stay on the X axis and the hinge came out inverted; at y=90 and y=270 the rotation carries them onto
    // north and south, the swap touches only the thin edges, and the same door was correct. Wrong precisely where
    // the swap did something, right precisely where it did not.

    public static String getMeaningfulMaterialInstanceName(String face, String texture, int elementIndex) {
        return texture + "_" + face + "_" + elementIndex;
    }

    /**
     * @deprecated kept only as a fallback for callers that cannot supply the source image
     * (e.g. the texture file failed to load). Produces the old zero-depth flat plane.
     * Prefer {@link #createFlatItemGeometry(String, BufferedImage)}.
     */
    /**
     * A zero-depth, two-sided plane covering the whole texture rect.
     * <p>
     * The right shape for an item whose animation frames have <b>different silhouettes</b>. Extrusion bakes
     * one silhouette into walls and slabs, but a single geometry serves every frame, so any frame that is
     * not that shape renders wrong: pixels outside the baked mask have no face to draw on, and walls baked
     * where a smaller frame has nothing stand away from the visible pixels, leaving the shape looking
     * unclosed. A plane has no walls and no mask, so each frame's own alpha shapes it exactly.
     */
    public static BedrockGeometry createFlatItemPlane(String identifier, int textureWidth, int textureHeight) {
        return createFlatItemGeometry(identifier, textureWidth, textureHeight);
    }

    /**
     * @deprecated prefer {@link #createFlatItemGeometry(String, BufferedImage)} for still textures, or
     *         {@link #createFlatItemPlane(String, int, int)} when frames change silhouette.
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
                .withPivot(ITEM_PIVOT[0], ITEM_PIVOT[1], ITEM_PIVOT[2]);

        BedrockGeometry.Cube cube = bone.addCube(-8.0F, 0, -0.5F, 16.0F, 16.0F, 0);

        cube.withFace("north", textureWidth, 0, -textureWidth, textureHeight, "#layer0_north_0");
        cube.withFace("south", 0, 0, textureWidth, textureHeight, "#layer0_south_0");

        return geo;
    }

    /**
     * A grid of one 1-unit-deep cube per texture pixel, each cube UV-mapped to that single pixel on all
     * six faces.
     * <p>
     * This is the only shape that is genuinely <b>correct for an animated item</b>. Extrusion
     * ({@link #createFlatItemGeometry(String, BufferedImage)}) bakes one frame's silhouette into slabs and
     * boundary walls, so every other frame renders wrong; a plane
     * ({@link #createFlatItemPlane(String, int, int)}) is shape-agnostic but flat. Here every pixel owns a
     * closed box, and a box whose pixel is transparent is discarded whole by the alpha-test material — so
     * the silhouette comes from the texture at render time, per frame, in 3D, with real extruded sides.
     * <p>
     * Because nothing about the geometry depends on any particular texture, <b>one lattice serves every
     * animated item of the same frame size</b> and the identifier should be derived from the size rather
     * than the item.
     * <p>
     * Transparent pixels are deliberately <b>not</b> skipped: doing so would reintroduce the dependency on
     * one frame's mask and defeat the entire point.
     * <p>
     * The bone name, binding, pivot and the 16×16×1 box it occupies match
     * {@link #createFlatItemGeometry(String, BufferedImage)} exactly, so existing animations, render
     * controllers and attachable scripts apply unchanged.
     */
    public static BedrockGeometry createPixelLatticeGeometry(String identifier, int texW, int texH) {
        BedrockGeometry geo = new BedrockGeometry(identifier)
                .withVisibleBoundsWidth(4.0F)
                .withVisibleBoundsHeight(4.0F)
                .withVisibleBoundsOffset(0.0F, 0.75F, 0.0F)
                .withTextureWidth(texW)
                .withTextureHeight(texH);

        BedrockGeometry.Bone bone = geo.addBone("bone")
                .withBinding("q.item_slot_to_bone_name(context.item_slot)")
                .withPivot(ITEM_PIVOT[0], ITEM_PIVOT[1], ITEM_PIVOT[2]);

        float unitX = 16.0F / texW;
        float unitY = 16.0F / texH;
        float depth = 1.0F;
        float zOrigin = -depth / 2.0F;

        for (int y = 0; y < texH; y++) {
            for (int x = 0; x < texW; x++) {
                BedrockGeometry.Cube cube = bone.addCube(
                        -8.0F + x * unitX,
                        16.0F - (y + 1) * unitY,
                        zOrigin,
                        unitX, unitY, depth);

                for (String face : new String[]{"north", "south", "east", "west", "up"}) {
                    cube.withFace(face, x, y, 1, 1, "#layer0_south_0");
                }
                // The down face samples the same pixel with V flipped, as vanilla-style item lattices do.
                cube.withFace("down", x, y + 1, 1, -1, "#layer0_south_0");
            }
        }

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
                .withPivot(ITEM_PIVOT[0], ITEM_PIVOT[1], ITEM_PIVOT[2]);

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