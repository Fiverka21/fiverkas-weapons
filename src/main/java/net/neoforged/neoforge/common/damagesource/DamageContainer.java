package net.neoforged.neoforge.common.damagesource;

import java.util.EnumMap;
import java.util.Map;

public class DamageContainer {
    private final Map<Reduction, Float> reductions = new EnumMap<>(Reduction.class);

    public float getReduction(Reduction reduction) {
        return reductions.getOrDefault(reduction, 0.0F);
    }

    public void setReduction(Reduction reduction, float value) {
        reductions.put(reduction, value);
    }

    public enum Reduction {
        ARMOR,
        ENCHANTMENTS
    }
}
