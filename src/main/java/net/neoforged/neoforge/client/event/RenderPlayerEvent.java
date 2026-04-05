package net.neoforged.neoforge.client.event;

import net.minecraft.world.entity.player.Player;

public class RenderPlayerEvent {
    public static class Pre extends RenderPlayerEvent {
        private final Player entity;
        private boolean canceled;

        public Pre(Player entity) {
            this.entity = entity;
        }

        public Player getEntity() {
            return entity;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }
}
