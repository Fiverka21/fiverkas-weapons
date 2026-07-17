package com.fiv.fiverkas_weapons.event;

import com.fiv.fiverkas_weapons.command.ModEnchantCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class ModCommandEvents {
    private ModCommandEvents() {
    }

    public static void onRegisterCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection selection
    ) {
        ModEnchantCommand.register(dispatcher, buildContext);
    }
}
