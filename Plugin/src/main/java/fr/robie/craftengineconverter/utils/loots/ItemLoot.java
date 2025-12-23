package fr.robie.craftengineconverter.utils.loots;

public abstract class ItemLoot {
    private final int minAmount;
    private final int maxAmount;
    private final float probability;


    public ItemLoot(int minAmount, int maxAmount, float probability) {
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.probability = probability;
    }


    public float getProbability() {
        return this.probability;
    }

    public int getMaxAmount() {
        return this.maxAmount;
    }

    public int getMinAmount() {
        return this.minAmount;
    }

    public abstract String getItemName();
}
