package com.fiv.fiverkas_weapons.event;

import com.fiv.fiverkas_weapons.command.ModEnchantCommand;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ModCommandEvents {
    private ModCommandEvents() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModEnchantCommand.register(event.getDispatcher(), event.getBuildContext());
    }
}
