package com.fiv.fiverkas_weapons;

import com.fiv.fiverkas_weapons.event.client.ModCombatClientEvents;
import com.fiv.fiverkas_weapons.network.ModNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

public class FiverkasWeaponsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(context ->
                context.addModels(FiverkasWeapons.id("item/blue_katana_held"))
        );
        ModNetwork.registerClientPayloads();
        ModCombatClientEvents.onClientSetup();
    }
}
