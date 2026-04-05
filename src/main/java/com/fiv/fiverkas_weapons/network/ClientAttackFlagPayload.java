package com.fiv.fiverkas_weapons.network;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.util.CompatIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientAttackFlagPayload(ModCombatEvents.ClientAttackFlag flag) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientAttackFlagPayload> TYPE =
            CompatIds.payloadType(FiverkasWeapons.MODID, "client_attack_flag");

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientAttackFlagPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ClientAttackFlagPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new ClientAttackFlagPayload(ModCombatEvents.ClientAttackFlag.fromId(buffer.readByte()));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, ClientAttackFlagPayload payload) {
                    buffer.writeByte(payload.flag().id());
                }
            };

    @Override
    public CustomPacketPayload.Type<ClientAttackFlagPayload> type() {
        return TYPE;
    }
}
