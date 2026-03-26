package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities.BlockEntityConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities.ItemDisplayRenderer;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.AnchorTypeBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.BooleanBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.HorizontalDirectionBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import net.momirealms.craftengine.core.block.properties.type.AnchorType;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import org.jetbrains.annotations.NotNull;

public class ButtonBlockState extends AbstractDefaultBlockState {
    public ButtonBlockState(@NotNull String baseBlock, @NotNull String pressedItem, @NotNull String notPressedItem, @NotNull ModelConfiguration model) {
        BooleanBlockStateProperty poweredProperty = new BooleanBlockStateProperty("powered", false);
        AnchorTypeBlockStateProperty faceProperty = new AnchorTypeBlockStateProperty("face", AnchorType.FLOOR);
        HorizontalDirectionBlockStateProperty facingProperty = new HorizontalDirectionBlockStateProperty("facing", HorizontalDirection.NORTH);

        this.addProperty(poweredProperty);
        this.addProperty(faceProperty);
        this.addProperty(facingProperty);

        for (AnchorType face : AnchorType.values()) {
            for (HorizontalDirection facing : HorizontalDirection.values()) {
                for (boolean powered : new boolean[]{false, true}) {
                    String faceName = face.name().toLowerCase();
                    String facingName = facing.name().toLowerCase();
                    String appearanceName = String.format("face=%s,facing=%s,powered=%s", faceName, facingName, powered);
                    String state = String.format("%s[face=%s,facing=%s,powered=%s]", baseBlock, faceName, facingName, powered);

                    String item = powered ? pressedItem : notPressedItem;
                    String rotation = null;
                    String translation = null;

                    switch (face) {
                        case FLOOR -> {
                            translation = "0,0.00023,0";
                            rotation = switch (facing) {
                                case EAST -> "0,90,0";
                                case WEST -> "0,-90,0";
                                case NORTH -> "0,180,0";
                                default -> null; // SOUTH
                            };
                        }
                        case WALL -> {
                            switch (facing) {
                                case NORTH -> {
                                    rotation = "-90,0,0";
                                    translation = "0,0,-0.00023";
                                }
                                case SOUTH -> {
                                    rotation = "90,0,180";
                                    translation = "0,0,0.00023";
                                }
                                case WEST -> {
                                    rotation = "0,90,90";
                                    translation = "-0.00023,0,0";
                                }
                                case EAST -> {
                                    rotation = "0,270,-90";
                                    translation = "0.00023,0,0";
                                }
                            }
                        }
                        case CEILING -> {
                            translation = "0,-0.00023,0";
                            rotation = switch (facing) {
                                case NORTH -> "0,180,180";
                                case SOUTH -> "0,0,180";
                                case WEST -> "0,90,180";
                                case EAST -> "-90,-90,-90";
                            };
                        }
                    }

                    ItemDisplayRenderer renderer = new ItemDisplayRenderer()
                            .setItem(item)
                            .setScale("1.0005");
                    if (rotation != null) {
                        renderer.setRotation(rotation);
                    }
                    if (translation != null) {
                        renderer.setTranslation(translation);
                    }

                    BlockEntityConfiguration blockEntity = new BlockEntityConfiguration();
                    blockEntity.addEntityRenderer(renderer);

                    this.addAppearanceWithEntity(appearanceName, state, model, blockEntity);

                    this.addVariant(new BlockVariant(appearanceName)
                            .addVariantCondition(faceProperty, face)
                            .addVariantCondition(facingProperty, facing)
                            .addVariantCondition(poweredProperty, powered));
                }
            }
        }
    }
}
