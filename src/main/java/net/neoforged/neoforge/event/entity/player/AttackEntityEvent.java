package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class AttackEntityEvent {
    private final LivingEntity entity;
    private final Entity target;

    public AttackEntityEvent(LivingEntity entity, Entity target) {
        this.entity = entity;
        this.target = target;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public Entity getTarget() {
        return target;
    }
}
