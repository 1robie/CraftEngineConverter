package fr.robie.craftengineconverter.utils.loots;

public class CraftEngineItemLoot extends ItemLoot {
    private final String itemId;

    public CraftEngineItemLoot(String itemId,  int minAmount, int maxAmount, float probability) {
        super(minAmount, maxAmount, probability);
        this.itemId = itemId;
    }

    @Override
    public String getItemName() {
        return this.itemId;
    }
}
