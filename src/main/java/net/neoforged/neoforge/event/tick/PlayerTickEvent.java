package net.neoforged.neoforge.event.tick;

import net.minecraft.world.entity.player.Player;

public class PlayerTickEvent {
    public static class Post extends PlayerTickEvent {
        private final Player entity;

        public Post(Player entity) {
            this.entity = entity;
        }

        public Player getEntity() {
            return entity;
        }
    }
}
