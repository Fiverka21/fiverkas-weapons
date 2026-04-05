package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class LivingChangeTargetEvent {
    private final Mob entity;
    private LivingEntity newAboutToBeSetTarget;

    public LivingChangeTargetEvent(Mob entity, LivingEntity newAboutToBeSetTarget) {
        this.entity = entity;
        this.newAboutToBeSetTarget = newAboutToBeSetTarget;
    }

    public Mob getEntity() {
        return entity;
    }

    public LivingEntity getNewAboutToBeSetTarget() {
        return newAboutToBeSetTarget;
    }

    public void setNewAboutToBeSetTarget(LivingEntity newAboutToBeSetTarget) {
        this.newAboutToBeSetTarget = newAboutToBeSetTarget;
    }
}
