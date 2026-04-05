package com.fiv.fiverkas_weapons.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class FiverkasWeaponsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FiverkasWeaponsFabric.MOD_EVENT_BUS.post(new FMLClientSetupEvent());
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) ->
                NeoForge.EVENT_BUS.post(new RenderGuiEvent.Post(guiGraphics))
        );
    }
}
