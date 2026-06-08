package com.fiv.fiverkas_weapons.network;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SacrilegiousSlamRequestPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SacrilegiousSlamRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "sacrilegious_slam_request")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, SacrilegiousSlamRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new SacrilegiousSlamRequestPayload());

    @Override
    public CustomPacketPayload.Type<SacrilegiousSlamRequestPayload> type() {
        return TYPE;
    }
}
