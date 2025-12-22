package fr.robie.craftengineconverter.common.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ArmorConverter {
    COMPONENT,
    TRIM,
    BOTH(TRIM, COMPONENT),

    ;
    private final List<ArmorConverter> composition = new ArrayList<>();

    ArmorConverter(){
        composition.add(this);
    }

    ArmorConverter(ArmorConverter... components){
        composition.addAll(Arrays.asList(components));
    }

    public List<ArmorConverter> getComposition() {
        return composition;
    }
}
