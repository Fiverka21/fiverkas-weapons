package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

public class LivingDamageEvent {
    public static class Pre extends LivingDamageEvent {
        private final LivingEntity entity;
        private final DamageSource source;
        private final DamageContainer container;
        private float newDamage;

        public Pre(LivingEntity entity, DamageSource source, float newDamage, DamageContainer container) {
            this.entity = entity;
            this.source = source;
            this.newDamage = newDamage;
            this.container = container;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getNewDamage() {
            return newDamage;
        }

        public void setNewDamage(float newDamage) {
            this.newDamage = newDamage;
        }

        public DamageContainer getContainer() {
            return container;
        }
    }

    public static class Post extends LivingDamageEvent {
        private final LivingEntity entity;
        private final DamageSource source;
        private final float newDamage;

        public Post(LivingEntity entity, DamageSource source, float newDamage) {
            this.entity = entity;
            this.source = source;
            this.newDamage = newDamage;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getNewDamage() {
            return newDamage;
        }
    }
}
