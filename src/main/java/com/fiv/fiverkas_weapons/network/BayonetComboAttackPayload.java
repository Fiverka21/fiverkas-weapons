package com.fiv.fiverkas_weapons.network;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BayonetComboAttackPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BayonetComboAttackPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "bayonet_combo_attack")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, BayonetComboAttackPayload> STREAM_CODEC =
            StreamCodec.unit(new BayonetComboAttackPayload());

    @Override
    public CustomPacketPayload.Type<BayonetComboAttackPayload> type() {
        return TYPE;
    }
}
