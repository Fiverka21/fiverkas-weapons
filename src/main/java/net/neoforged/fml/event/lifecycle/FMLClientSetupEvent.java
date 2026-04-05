package net.neoforged.fml.event.lifecycle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class FMLClientSetupEvent {
    public void enqueueWork(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }
}
