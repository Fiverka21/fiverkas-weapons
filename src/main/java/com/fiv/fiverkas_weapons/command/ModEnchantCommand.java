package com.fiv.fiverkas_weapons.command;

import com.fiv.fiverkas_weapons.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Collection;
import java.util.Set;

public class ModEnchantCommand {
    private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
            p_304205_ -> Component.translatableEscape("commands.enchant.failed.entity", p_304205_)
    );
    private static final DynamicCommandExceptionType ERROR_NO_ITEM = new DynamicCommandExceptionType(
            p_304207_ -> Component.translatableEscape("commands.enchant.failed.itemless", p_304207_)
    );
    private static final DynamicCommandExceptionType ERROR_INCOMPATIBLE = new DynamicCommandExceptionType(
            p_304206_ -> Component.translatableEscape("commands.enchant.failed.incompatible", p_304206_)
    );
    private static final Dynamic2CommandExceptionType ERROR_LEVEL_TOO_HIGH = new Dynamic2CommandExceptionType(
            (p_304208_, p_304209_) -> Component.translatableEscape("commands.enchant.failed.level", p_304208_, p_304209_)
    );
    private static final SimpleCommandExceptionType ERROR_NOTHING_HAPPENED = new SimpleCommandExceptionType(Component.translatable("commands.enchant.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
                Commands.literal("enchant")
                        .requires(p_137013_ -> p_137013_.hasPermission(2))
                        .then(
                                Commands.argument("targets", EntityArgument.entities())
                                        .then(
                                                Commands.argument("enchantment", ResourceArgument.resource(context, Registries.ENCHANTMENT))
                                                        .executes(
                                                                p_248131_ -> enchant(
                                                                        p_248131_.getSource(),
                                                                        EntityArgument.getEntities(p_248131_, "targets"),
                                                                        ResourceArgument.getEnchantment(p_248131_, "enchantment"),
                                                                        1
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument("level", IntegerArgumentType.integer(0))
                                                                        .executes(
                                                                                p_248132_ -> enchant(
                                                                                        p_248132_.getSource(),
                                                                                        EntityArgument.getEntities(p_248132_, "targets"),
                                                                                        ResourceArgument.getEnchantment(p_248132_, "enchantment"),
                                                                                        IntegerArgumentType.getInteger(p_248132_, "level")
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int enchant(
            CommandSourceStack source,
            Collection<? extends Entity> targets,
            Holder<Enchantment> enchantmentHolder,
            int level
    ) throws CommandSyntaxException {
        Enchantment enchantment = enchantmentHolder.value();
        if (level > enchantment.getMaxLevel()) {
            throw ERROR_LEVEL_TOO_HIGH.create(level, enchantment.getMaxLevel());
        }

        int applied = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity livingentity) {
                ItemStack itemstack = livingentity.getMainHandItem();
                if (!itemstack.isEmpty()) {
                    boolean supports = itemstack.supportsEnchantment(enchantmentHolder);
                    boolean compatible = EnchantmentHelper.isEnchantmentCompatible(
                            EnchantmentHelper.getEnchantmentsForCrafting(itemstack).keySet(),
                            enchantmentHolder
                    );
                    if (supports && (compatible || isAirmaceBreachDensityOverride(itemstack, enchantmentHolder))) {
                        itemstack.enchant(enchantmentHolder, level);
                        applied++;
                    } else if (targets.size() == 1) {
                        throw ERROR_INCOMPATIBLE.create(itemstack.getItem().getName(itemstack).getString());
                    }
                } else if (targets.size() == 1) {
                    throw ERROR_NO_ITEM.create(livingentity.getName().getString());
                }
            } else if (targets.size() == 1) {
                throw ERROR_NOT_LIVING_ENTITY.create(entity.getName().getString());
            }
        }

        if (applied == 0) {
            throw ERROR_NOTHING_HAPPENED.create();
        }

        if (targets.size() == 1) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.enchant.success.single",
                            Enchantment.getFullname(enchantmentHolder, level),
                            targets.iterator().next().getDisplayName()
                    ),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.enchant.success.multiple",
                            Enchantment.getFullname(enchantmentHolder, level),
                            targets.size()
                    ),
                    true
            );
        }

        return applied;
    }

    private static boolean isAirmaceBreachDensityOverride(ItemStack stack, Holder<Enchantment> enchantment) {
        if (!stack.is(ModItems.AIRMACE.get())) {
            return false;
        }
        if (!enchantment.is(Enchantments.BREACH) && !enchantment.is(Enchantments.DENSITY)) {
            return false;
        }

        Set<Holder<Enchantment>> existing = EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet();
        boolean hasPair = false;
        for (Holder<Enchantment> holder : existing) {
            if (Enchantment.areCompatible(holder, enchantment)) {
                continue;
            }
            if (isBreachDensityPair(holder, enchantment)) {
                hasPair = true;
                continue;
            }
            return false;
        }
        return hasPair;
    }

    private static boolean isBreachDensityPair(Holder<Enchantment> first, Holder<Enchantment> second) {
        return (first.is(Enchantments.BREACH) && second.is(Enchantments.DENSITY))
                || (first.is(Enchantments.DENSITY) && second.is(Enchantments.BREACH));
    }
}
