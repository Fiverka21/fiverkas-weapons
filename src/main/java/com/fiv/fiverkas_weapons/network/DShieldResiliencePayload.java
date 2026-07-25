package com.fiv.fiverkas_weapons.network;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DShieldResiliencePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DShieldResiliencePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(FiverkasWeapons.MODID, "dshield_resilience")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, DShieldResiliencePayload> STREAM_CODEC =
            StreamCodec.unit(new DShieldResiliencePayload());

    @Override
    public CustomPacketPayload.Type<DShieldResiliencePayload> type() {
        return TYPE;
    }
}
