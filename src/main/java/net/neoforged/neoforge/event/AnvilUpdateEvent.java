package net.neoforged.neoforge.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AnvilUpdateEvent {
    private final Player player;
    private final ItemStack left;
    private final ItemStack right;
    private final String name;
    private ItemStack output = ItemStack.EMPTY;
    private long cost;
    private int materialCost;

    public AnvilUpdateEvent(Player player, ItemStack left, ItemStack right, String name) {
        this.player = player;
        this.left = left;
        this.right = right;
        this.name = name;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getLeft() {
        return left;
    }

    public ItemStack getRight() {
        return right;
    }

    public String getName() {
        return name;
    }

    public ItemStack getOutput() {
        return output;
    }

    public void setOutput(ItemStack output) {
        this.output = output;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public int getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(int materialCost) {
        this.materialCost = materialCost;
    }
}
