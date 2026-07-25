package com.fiv.fiverkas_weapons;

import com.fiv.fiverkas_weapons.event.client.ModCombatClientEvents;
import com.fiv.fiverkas_weapons.network.ModNetwork;
import net.fabricmc.api.ClientModInitializer;

public class FiverkasWeaponsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModNetwork.registerClientPayloads();
        ModCombatClientEvents.onClientSetup();
    }
}
