package fr.robie.craftEngineConverter.utils;

import org.jetbrains.annotations.NotNull;

public class FloatsUtils {
    private final float[] values;
    private boolean updated;

    public FloatsUtils(int size,float[] defaultValues){
        this.values = new float[size];
        this.updated = false;
        System.arraycopy(defaultValues, 0, this.values, 0, Math.min(defaultValues.length, size));
    }

    public void setValue(int index, float value){
        this.values[index] = value;
        this.updated = true;
    }

    public boolean isUpdated(){
        return this.updated;
    }

    @Override
    public @NotNull String toString(){
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<values.length;i++){
            sb.append(values[i]);
            if (i < values.length -1){
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
