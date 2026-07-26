package com.fiv.fiverkas_weapons.network;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SacrilegiousSlamPayload(int playerId, String animationName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SacrilegiousSlamPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "sacrilegious_slam")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, SacrilegiousSlamPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SacrilegiousSlamPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new SacrilegiousSlamPayload(buffer.readInt(), buffer.readUtf(Short.MAX_VALUE));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, SacrilegiousSlamPayload payload) {
                    buffer.writeInt(payload.playerId());
                    buffer.writeUtf(payload.animationName());
                }
            };

    @Override
    public CustomPacketPayload.Type<SacrilegiousSlamPayload> type() {
        return TYPE;
    }
}
