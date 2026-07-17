package com.fiv.fiverkas_weapons.network;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BayonetImpactFramePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BayonetImpactFramePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "bayonet_impact_frame")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, BayonetImpactFramePayload> STREAM_CODEC =
            StreamCodec.unit(new BayonetImpactFramePayload());

    @Override
    public CustomPacketPayload.Type<BayonetImpactFramePayload> type() {
        return TYPE;
    }
}
