package com.fiv.fiverkas_weapons.network;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BayonetMuzzleFlashPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BayonetMuzzleFlashPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "bayonet_muzzle_flash")
            );
    public static final StreamCodec<RegistryFriendlyByteBuf, BayonetMuzzleFlashPayload> STREAM_CODEC =
            StreamCodec.unit(new BayonetMuzzleFlashPayload());

    @Override
    public CustomPacketPayload.Type<BayonetMuzzleFlashPayload> type() {
        return TYPE;
    }
}
