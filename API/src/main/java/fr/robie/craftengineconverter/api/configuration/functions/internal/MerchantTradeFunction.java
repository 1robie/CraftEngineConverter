package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MerchantTradeFunction extends AbstractEventFunction {
    private String title;
    private final List<Offer> offers = new ArrayList<>();

    public MerchantTradeFunction() {
        super("merchant_trade");
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addOffer(Offer offer) {
        this.offers.add(offer);
    }

    public record Offer(TradeItem cost1, TradeItem cost2, TradeItem result, Integer experience) {
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            map.put("cost-1", this.cost1.serialize());
            if (this.cost2 != null) {
                map.put("cost-2", this.cost2.serialize());
            }
            map.put("result", this.result.serialize());
            if (this.experience != null) {
                map.put("experience", this.experience);
            }
            return map;
        }
    }

    public record TradeItem(String item, Integer count, Map<String, Object> components) {
        public Object serialize() {
            if (this.count == null && this.components == null) {
                return this.item;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("item", this.item);
            if (this.count != null) {
                map.put("count", this.count);
            }
            if (this.components != null) {
                map.put("components", this.components);
            }
            return map;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.title != null) {
            map.put("title", this.title);
        }
        List<Map<String, Object>> serializedOffers = new ArrayList<>();
        for (Offer offer : this.offers) serializedOffers.add(offer.serialize());
        map.put("offers", serializedOffers);
        return map;
    }
}
