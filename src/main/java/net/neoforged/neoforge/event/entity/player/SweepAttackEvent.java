package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.entity.player.Player;

public class SweepAttackEvent {
    private final Player entity;
    private boolean sweeping;
    private boolean canceled;

    public SweepAttackEvent(Player entity, boolean sweeping) {
        this.entity = entity;
        this.sweeping = sweeping;
    }

    public Player getEntity() {
        return entity;
    }

    public boolean isSweeping() {
        return sweeping;
    }

    public void setSweeping(boolean sweeping) {
        this.sweeping = sweeping;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
