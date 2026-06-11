package fr.robie.craftengineconverter.api.configuration.item.behavior.block;

import fr.robie.craftengineconverter.api.common.TrackableSettings;
import fr.robie.yamllibrary.ConfigurationSection;
import net.momirealms.craftengine.core.block.setting.PushReaction;
import net.momirealms.craftengine.core.util.Instrument;
import net.momirealms.craftengine.core.util.Tristate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class BlockSettings extends TrackableSettings {
    private final String item;

    private float hardness = 2;
    private float resistance = 2;
    private boolean isRandomlyTicking = false;
    private PushReaction pushReaction = PushReaction.NORMAL;
    private int mapColor = 0;
    private boolean burnable = false;
    private int fireSpreadChance = 0;
    private int burnChance = 0;
    private boolean replaceable = false;
    private Tristate isRedstoneConductor = Tristate.UNDEFINED;
    private Tristate isSuffocating = Tristate.UNDEFINED;
    private Tristate isViewBlocking = Tristate.UNDEFINED;
    // Sounds
    private String fallSound = null;
    private String hitSound = null;
    private String breakSound = null;
    private String stepSound = null;
    private String placeSound = null;

    private boolean requireCorrectTools = false;
    private boolean respectToolComponent = false;
    private List<String> correctTools = null;
    private float incorrectToolDigSpeed = 0.3f;
    private List<String> tags = null;
    private List<String> clientBoundTags = null;

    private Instrument instrument = null;
    private String fluidState = null;
    private String supportShape = null;

    private float friction = 0.6f;
    private float jumpFactor = 1.0f;
    private float speedFactor = 1.0f;

    private int luminance = 0;
    private boolean canOcclude = false;

    private Integer blockLight = null;
    private boolean propagateSkyLight = true;


    public BlockSettings(String item) {
        this.item = item;
        super.markInitialized();
    }

    // -------------------------
    // Getters
    // -------------------------

    public String getItem() {
        return this.item;
    }

    public float getHardness() {
        return this.hardness;
    }

    public float getResistance() {
        return this.resistance;
    }

    public boolean isRandomlyTicking() {
        return this.isRandomlyTicking;
    }

    public PushReaction getPushReaction() {
        return this.pushReaction;
    }

    public int getMapColor() {
        return this.mapColor;
    }

    public boolean isBurnable() {
        return this.burnable;
    }

    public int getFireSpreadChance() {
        return this.fireSpreadChance;
    }

    public int getBurnChance() {
        return this.burnChance;
    }

    public boolean isReplaceable() {
        return this.replaceable;
    }

    public Tristate getIsRedstoneConductor() {
        return this.isRedstoneConductor;
    }

    public Tristate getIsSuffocating() {
        return this.isSuffocating;
    }

    public Tristate getIsViewBlocking() {
        return this.isViewBlocking;
    }

    public String getFallSound() {
        return this.fallSound;
    }

    public String getHitSound() {
        return this.hitSound;
    }

    public String getBreakSound() {
        return this.breakSound;
    }

    public String getStepSound() {
        return this.stepSound;
    }

    public String getPlaceSound() {
        return this.placeSound;
    }

    public boolean isRequireCorrectTools() {
        return this.requireCorrectTools;
    }

    public boolean isRespectToolComponent() {
        return this.respectToolComponent;
    }

    public List<String> getCorrectTools() {
        return this.correctTools;
    }

    public float getIncorrectToolDigSpeed() {
        return this.incorrectToolDigSpeed;
    }

    public List<String> getTags() {
        return this.tags;
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = List.of(tag);
        } else if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public List<String> getClientBoundTags() {
        return this.clientBoundTags;
    }

    public Instrument getInstrument() {
        return this.instrument;
    }

    public String getFluidState() {
        return this.fluidState;
    }

    public String getSupportShape() {
        return this.supportShape;
    }

    public float getFriction() {
        return this.friction;
    }

    public float getJumpFactor() {
        return this.jumpFactor;
    }

    public float getSpeedFactor() {
        return this.speedFactor;
    }

    public int getLuminance() {
        return this.luminance;
    }

    public boolean isCanOcclude() {
        return this.canOcclude;
    }

    public Integer getBlockLight() {
        return this.blockLight;
    }

    public boolean isPropagateSkyLight() {
        return this.propagateSkyLight;
    }

    public BlockSettings setHardness(float hardness) {
        this.hardness = hardness;
        return this;
    }

    public BlockSettings setResistance(float resistance) {
        this.resistance = resistance;
        return this;
    }

    public BlockSettings setRandomlyTicking(boolean randomlyTicking) {
        this.isRandomlyTicking = randomlyTicking;
        return this;
    }

    public BlockSettings setPushReaction(PushReaction pushReaction) {
        this.pushReaction = pushReaction;
        return this;
    }

    public BlockSettings setMapColor(int mapColor) {
        this.mapColor = mapColor;
        return this;
    }

    public BlockSettings setBurnable(boolean burnable) {
        this.burnable = burnable;
        return this;
    }

    public BlockSettings setFireSpreadChance(int fireSpreadChance) {
        this.fireSpreadChance = fireSpreadChance;
        return this;
    }

    public BlockSettings setBurnChance(int burnChance) {
        this.burnChance = burnChance;
        return this;
    }

    public BlockSettings setReplaceable(boolean replaceable) {
        this.replaceable = replaceable;
        return this;
    }

    public BlockSettings setRedstoneConductor(Tristate redstoneConductor) {
        this.isRedstoneConductor = redstoneConductor;
        return this;
    }

    public BlockSettings setSuffocating(Tristate suffocating) {
        this.isSuffocating = suffocating;
        return this;
    }

    public BlockSettings setViewBlocking(Tristate viewBlocking) {
        this.isViewBlocking = viewBlocking;
        return this;
    }

    public BlockSettings setFallSound(String fallSound) {
        this.fallSound = fallSound;
        return this;
    }

    public BlockSettings setHitSound(String hitSound) {
        this.hitSound = hitSound;
        return this;
    }

    public BlockSettings setBreakSound(String breakSound) {
        this.breakSound = breakSound;
        return this;
    }

    public BlockSettings setStepSound(String stepSound) {
        this.stepSound = stepSound;
        return this;
    }

    public BlockSettings setPlaceSound(String placeSound) {
        this.placeSound = placeSound;
        return this;
    }

    public BlockSettings setRequireCorrectTools(boolean requireCorrectTools) {
        this.requireCorrectTools = requireCorrectTools;
        return this;
    }

    public BlockSettings setRespectToolComponent(boolean respectToolComponent) {
        this.respectToolComponent = respectToolComponent;
        return this;
    }

    public BlockSettings setCorrectTools(List<String> correctTools) {
        this.correctTools = correctTools;
        return this;
    }

    public BlockSettings setIncorrectToolDigSpeed(float incorrectToolDigSpeed) {
        this.incorrectToolDigSpeed = incorrectToolDigSpeed;
        return this;
    }

    public BlockSettings setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public BlockSettings setClientBoundTags(List<String> clientBoundTags) {
        this.clientBoundTags = clientBoundTags;
        return this;
    }

    public BlockSettings setInstrument(Instrument instrument) {
        this.instrument = instrument;
        return this;
    }

    public BlockSettings setFluidState(String fluidState) {
        this.fluidState = fluidState;
        return this;
    }

    public BlockSettings setSupportShape(String supportShape) {
        this.supportShape = supportShape;
        return this;
    }

    public BlockSettings setFriction(float friction) {
        this.friction = friction;
        return this;
    }

    public BlockSettings setJumpFactor(float jumpFactor) {
        this.jumpFactor = jumpFactor;
        return this;
    }

    public BlockSettings setSpeedFactor(float speedFactor) {
        this.speedFactor = speedFactor;
        return this;
    }

    public BlockSettings setLuminance(int luminance) {
        this.luminance = luminance;
        return this;
    }

    public BlockSettings setCanOcclude(boolean canOcclude) {
        this.canOcclude = canOcclude;
        return this;
    }

    public BlockSettings setBlockLight(Integer blockLight) {
        this.blockLight = blockLight;
        return this;
    }

    public BlockSettings setPropagateSkyLight(boolean propagateSkyLight) {
        this.propagateSkyLight = propagateSkyLight;
        return this;
    }

    public void serialize(@NotNull ConfigurationSection settingsConfigurationSection) {
        if (this.item != null)
            settingsConfigurationSection.set("item", this.item);

        if (this.hardness != 2f)
            settingsConfigurationSection.set("hardness", this.hardness);
        if (this.resistance != 2f)
            settingsConfigurationSection.set("resistance", this.resistance);
        if (this.isRandomlyTicking)
            settingsConfigurationSection.set("is-randomly-ticking", true);
        if (this.pushReaction != PushReaction.NORMAL)
            settingsConfigurationSection.set("push-reaction", this.pushReaction.name());
        if (this.mapColor != 0)
            settingsConfigurationSection.set("map-color", this.mapColor);
        if (this.burnable)
            settingsConfigurationSection.set("burnable", true);
        if (this.fireSpreadChance != 0)
            settingsConfigurationSection.set("fire-spread-chance", this.fireSpreadChance);
        if (this.burnChance != 0)
            settingsConfigurationSection.set("burn-chance", this.burnChance);
        if (this.replaceable)
            settingsConfigurationSection.set("replaceable", true);
        if (this.isRedstoneConductor != Tristate.UNDEFINED)
            settingsConfigurationSection.set("is-redstone-conductor", this.isRedstoneConductor.asBoolean());
        if (this.isSuffocating != Tristate.UNDEFINED)
            settingsConfigurationSection.set("is-suffocating", this.isSuffocating.asBoolean());
        if (this.isViewBlocking != Tristate.UNDEFINED)
            settingsConfigurationSection.set("is-view-blocking", this.isViewBlocking.asBoolean());

        if (this.breakSound != null || this.stepSound != null || this.placeSound != null || this.hitSound != null || this.fallSound != null) {
            ConfigurationSection sounds = settingsConfigurationSection.createSection("sounds");
            if (this.breakSound != null)
                sounds.set("break", this.breakSound);
            if (this.stepSound != null)
                sounds.set("step", this.stepSound);
            if (this.placeSound != null)
                sounds.set("place", this.placeSound);
            if (this.hitSound != null)
                sounds.set("hit", this.hitSound);
            if (this.fallSound != null)
                sounds.set("fall", this.fallSound);
        }

        if (this.requireCorrectTools)
            settingsConfigurationSection.set("require-correct-tools", true);
        if (this.respectToolComponent)
            settingsConfigurationSection.set("respect-tool-component", true);
        if (this.correctTools != null && !this.correctTools.isEmpty())
            settingsConfigurationSection.set("correct-tools", this.correctTools);
        if (this.incorrectToolDigSpeed != 0.3f)
            settingsConfigurationSection.set("incorrect-tool-dig-speed", this.incorrectToolDigSpeed);
        if (this.tags != null && !this.tags.isEmpty())
            settingsConfigurationSection.set("tags", this.tags);
        if (this.clientBoundTags != null && !this.clientBoundTags.isEmpty())
            settingsConfigurationSection.set("client-bound-tags", this.clientBoundTags);

        if (this.instrument != null)
            settingsConfigurationSection.set("instrument", this.instrument.name());
        if (this.fluidState != null)
            settingsConfigurationSection.set("fluid-state", this.fluidState);
        if (this.supportShape != null)
            settingsConfigurationSection.set("support-shape", this.supportShape);

        if (this.friction != 0.6f)
            settingsConfigurationSection.set("friction", this.friction);
        if (this.jumpFactor != 1.0f)
            settingsConfigurationSection.set("jump-factor", this.jumpFactor);
        if (this.speedFactor != 1.0f)
            settingsConfigurationSection.set("speed-factor", this.speedFactor);

        if (this.luminance != 0)
            settingsConfigurationSection.set("luminance", this.luminance);
        if (this.canOcclude)
            settingsConfigurationSection.set("can-occlude", true);
        if (this.blockLight != null)
            settingsConfigurationSection.set("block-light", this.blockLight);
        if (!this.propagateSkyLight)
            settingsConfigurationSection.set("propagate-skylight", false);
    }

    @Override
    protected int computeHashCode() {
        return Objects.hash(
                this.hardness, this.resistance, this.isRandomlyTicking, this.pushReaction, this.mapColor,
                this.burnable, this.fireSpreadChance, this.burnChance, this.replaceable,
                this.isRedstoneConductor, this.isSuffocating, this.isViewBlocking,
                this.fallSound, this.hitSound, this.breakSound, this.stepSound, this.placeSound,
                this.requireCorrectTools, this.respectToolComponent, this.correctTools,
                this.incorrectToolDigSpeed, this.tags, this.clientBoundTags,
                this.instrument, this.fluidState, this.supportShape,
                this.friction, this.jumpFactor, this.speedFactor,
                this.luminance, this.canOcclude, this.blockLight, this.propagateSkyLight
        );
    }
}