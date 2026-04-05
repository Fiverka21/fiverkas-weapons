package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LivingIncomingDamageEvent {
    private final LivingEntity entity;
    private final DamageSource source;
    private final DamageContainer container = new DamageContainer();
    private final Map<DamageContainer.Reduction, List<ReductionModifier>> reductionModifiers =
            new EnumMap<>(DamageContainer.Reduction.class);
    private float amount;

    public LivingIncomingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
        this.entity = entity;
        this.source = source;
        this.amount = amount;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public DamageContainer getContainer() {
        return container;
    }

    public void addReductionModifier(DamageContainer.Reduction reduction, ReductionModifier modifier) {
        reductionModifiers.computeIfAbsent(reduction, ignored -> new ArrayList<>()).add(modifier);
    }

    public float applyReductionModifiers(DamageContainer.Reduction reduction, float value) {
        float modified = value;
        List<ReductionModifier> modifiers = reductionModifiers.get(reduction);
        if (modifiers == null) {
            return modified;
        }
        for (ReductionModifier modifier : modifiers) {
            modified = modifier.apply(container, modified);
        }
        return modified;
    }

    @FunctionalInterface
    public interface ReductionModifier {
        float apply(DamageContainer container, float currentReduction);
    }
}
