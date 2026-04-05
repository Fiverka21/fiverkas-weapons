package net.neoforged.neoforge.client.event;

import net.minecraft.client.gui.GuiGraphics;

public class RenderGuiEvent {
    public static class Post extends RenderGuiEvent {
        private final GuiGraphics guiGraphics;

        public Post(GuiGraphics guiGraphics) {
            this.guiGraphics = guiGraphics;
        }

        public GuiGraphics getGuiGraphics() {
            return guiGraphics;
        }
    }
}
