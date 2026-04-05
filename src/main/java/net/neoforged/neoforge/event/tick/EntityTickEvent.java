package net.neoforged.neoforge.event.tick;

import net.minecraft.world.entity.Entity;

public class EntityTickEvent {
    public static class Post extends EntityTickEvent {
        private final Entity entity;

        public Post(Entity entity) {
            this.entity = entity;
        }

        public Entity getEntity() {
            return entity;
        }
    }
}
