package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.hitbox;

import java.util.Map;

public abstract class CollidableHitbox extends BaseHitbox {
    protected boolean canUseItemOn;
    protected boolean canBeHitByProjectile;
    protected boolean blocksBuilding;
    protected boolean interactive = true;
    protected boolean invisible = false;

    protected CollidableHitbox(boolean canUseItemOnDefault, boolean canBeHitByProjectileDefault, boolean blocksBuildingDefault) {
        this.canUseItemOn = canUseItemOnDefault;
        this.canBeHitByProjectile = canBeHitByProjectileDefault;
        this.blocksBuilding = blocksBuildingDefault;
    }

    public void setCanUseItemOn(boolean canUseItemOn) {
        this.canUseItemOn = canUseItemOn;
    }

    public void setCanBeHitByProjectile(boolean canBeHitByProjectile) {
        this.canBeHitByProjectile = canBeHitByProjectile;
    }

    public void setBlocksBuilding(boolean blocksBuilding) {
        this.blocksBuilding = blocksBuilding;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    @Override
    protected void serialize(Map<String, Object> data) {
        super.serialize(data);
        if (this.canUseItemOn != this.getDefaultCanUseItemOn()) {
            data.put("can-use-item-on", this.canUseItemOn);
        }
        if (this.canBeHitByProjectile != this.getDefaultCanBeHitByProjectile()) {
            data.put("can-be-hit-by-projectile", this.canBeHitByProjectile);
        }
        if (this.blocksBuilding != this.getDefaultBlocksBuilding()) {
            data.put("blocks-building", this.blocksBuilding);
        }
        if (!this.interactive) {
            data.put("interactive", false);
        }
        if (this.invisible) {
            data.put("invisible", true);
        }
    }

    protected abstract boolean getDefaultCanUseItemOn();

    protected abstract boolean getDefaultCanBeHitByProjectile();

    protected abstract boolean getDefaultBlocksBuilding();
}
