package fr.robie.craftengineconverter.api.configuration.item;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractItemConfiguration implements ItemConfigurationSerializable {

    protected final boolean disableDefaultConfiguration;

    protected AbstractItemConfiguration(boolean disableDefaultConfiguration) {
        this.disableDefaultConfiguration = disableDefaultConfiguration;
    }

    protected String applyNoItalic(String text) {
        return (this.disableDefaultConfiguration ? "<!i>" : "") + text;
    }

    protected List<String> applyNoItalic(List<String> lines) {
        if (!this.disableDefaultConfiguration) {
            return lines;
        }
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add("<!i>" + line);
        }
        return result;
    }
}
