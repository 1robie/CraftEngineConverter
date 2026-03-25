package fr.robie.craftengineconverter.behavior;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.core.world.particle.ParticleConfig;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnergyBlastItemBehavior extends ItemBehavior {
    public static final Factory FACTORY = new Factory();
    private final long delayMs;
    private final double length;
    private final double spacing;
    private final long damage;
    private final ParticleConfig particleConfig;
    private final double damageBoxX;
    private final double damageBoxY;
    private final double damageBoxZ;
    private final boolean damagePlayersOnly;
    private final boolean stopOnFirstHit;

    private final Map<UUID, Long> lastUsedMap = new ConcurrentHashMap<>();

    public EnergyBlastItemBehavior(long delayMs, double length, double spacing, long damage, ParticleConfig particleConfig,
                                   double damageBoxX, double damageBoxY, double damageBoxZ,
                                   boolean damagePlayersOnly, boolean stopOnFirstHit) {
        this.delayMs = delayMs;
        this.length = length;
        this.spacing = spacing;
        this.damage = damage;
        this.particleConfig = particleConfig;
        this.damageBoxX = damageBoxX;
        this.damageBoxY = damageBoxY;
        this.damageBoxZ = damageBoxZ;
        this.damagePlayersOnly = damagePlayersOnly;
        this.stopOnFirstHit = stopOnFirstHit;
    }

    public InteractionResult use(World world, @Nullable Player player, InteractionHand hand) {
        if (player == null) {
            return InteractionResult.FAIL;
        }
        Long lastUsed = this.lastUsedMap.get(player.uuid());
        long now = System.currentTimeMillis();
        if (lastUsed != null && (now - lastUsed) < this.delayMs) {
            return InteractionResult.FAIL;
        }
        this.lastUsedMap.put(player.uuid(), now);

        WorldPosition eyePosition = player.eyePosition();

        float yaw = player.yRot();
        float pitch = player.xRot();

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

        int particleCount = (int) (this.length / this.spacing);

        Context emptyContext = ItemBuildContext.empty();

        org.bukkit.World bukkitWorld = (org.bukkit.World) world.platformWorld();

        boolean hitEntity = false;
        for (int i = 0; i <= particleCount; i++) {
            double distance = i * this.spacing;

            double x = eyePosition.x() + (dirX * distance);
            double y = eyePosition.y() + (dirY * distance);
            double z = eyePosition.z() + (dirZ * distance);
            Position pos = new Vec3d(x, y, z);
            ExistingBlock block = world.getBlock((int) pos.x(), (int) pos.y(), (int) pos.z());
            if (!block.blockState().isAir()) {
                break;
            }

            for (org.bukkit.entity.Entity entity : bukkitWorld.getNearbyEntities(
                    new Location(bukkitWorld, x, y, z),
                    this.damageBoxX, this.damageBoxY, this.damageBoxZ)) {
                if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
                    if (entity.getUniqueId().equals(player.uuid())) {
                        continue;
                    }
                    if (this.damagePlayersOnly && !(entity instanceof org.bukkit.entity.Player)) {
                        continue;
                    }
                    DamageSource damageSource = DamageSource.builder(DamageType.PLAYER_ATTACK)
                            .withCausingEntity((org.bukkit.entity.Player) player.platformPlayer())
                            .withDirectEntity((org.bukkit.entity.Player) player.platformPlayer())
                            .build();
                    livingEntity.damage(this.damage, damageSource);
                    hitEntity = true;
                    if (this.stopOnFirstHit) {
                        break;
                    }
                }
            }
            if (this.stopOnFirstHit && hitEntity) {
                break;
            }

            world.spawnParticle(pos, this.particleConfig.particleType, this.particleConfig.count.getInt(emptyContext), this.particleConfig.xOffset.getInt(emptyContext),
                    this.particleConfig.yOffset.getInt(emptyContext), this.particleConfig.zOffset.getInt(emptyContext), this.particleConfig.speed.getInt(emptyContext),
                    this.particleConfig.particleData, emptyContext);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        Player player = context.getPlayer();
        World world = context.getWorld();
        InteractionHand hand = context.getHand();
        return this.use(world, player, hand);
    }

    public static class Factory implements ItemBehaviorFactory<EnergyBlastItemBehavior> {
        @Override
        public EnergyBlastItemBehavior create(Pack pack, Path path, String node, Key id, Map<String, Object> arguments) {
            long delayMs = arguments.getOrDefault("delay_ms", 1000) instanceof Number n ? n.longValue() : 1000;
            double blastLength = arguments.getOrDefault("length", 10d) instanceof Number n ? n.doubleValue() : 10d;
            double blastSpacing = arguments.getOrDefault("spacing", 0.5d) instanceof Number n ? n.doubleValue() : 0.5d;
            long blastDamage = arguments.getOrDefault("damage", 5d) instanceof Number n ? n.longValue() : 5;
            ParticleConfig particleConfig = ParticleConfig.fromMap$blockEntity(arguments);
            double damageBoxX = arguments.getOrDefault("damage_box_x", 0.5) instanceof Number n1 ? n1.doubleValue() : 0.5;
            double damageBoxY = arguments.getOrDefault("damage_box_y", 0.5) instanceof Number n2 ? n2.doubleValue() : 0.5;
            double damageBoxZ = arguments.getOrDefault("damage_box_z", 0.5) instanceof Number n3 ? n3.doubleValue() : 0.5;
            boolean damagePlayersOnly = arguments.getOrDefault("damage_players_only", false) instanceof Boolean b1 ? b1 : false;
            boolean stopOnFirstHit = arguments.getOrDefault("stop_on_first_hit", false) instanceof Boolean b2 ? b2 : false;
            return new EnergyBlastItemBehavior(
                    delayMs,
                    blastLength,
                    blastSpacing,
                    blastDamage,
                    particleConfig,
                    damageBoxX,
                    damageBoxY,
                    damageBoxZ,
                    damagePlayersOnly,
                    stopOnFirstHit
            );

        }
    }
}