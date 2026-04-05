package net.neoforged.neoforge.network.registration;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.BiConsumer;

public class PayloadRegistrar {
    private final String protocolVersion;

    public PayloadRegistrar(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    public <P extends CustomPacketPayload> void playToServer(
            CustomPacketPayload.Type<P> type,
            StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            BiConsumer<P, IPayloadContext> handler
    ) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                handler.accept(payload, new ServerContext(context))
        );
    }

    public <P extends CustomPacketPayload> void playToClient(
            CustomPacketPayload.Type<P> type,
            StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            BiConsumer<P, IPayloadContext> handler
    ) {
        PayloadTypeRegistry.playS2C().register(type, codec);
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientRegistration.register(type, handler);
        }
    }

    private record ServerContext(ServerPlayNetworking.Context context) implements IPayloadContext {
        @Override
        public net.minecraft.world.entity.player.Player player() {
            return context.player();
        }

        @Override
        public void enqueueWork(Runnable runnable) {
            context.server().execute(runnable);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientRegistration {
        private ClientRegistration() {
        }

        private static <P extends CustomPacketPayload> void register(
                CustomPacketPayload.Type<P> type,
                BiConsumer<P, IPayloadContext> handler
        ) {
            ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                    handler.accept(payload, new ClientContext())
            );
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientContext implements IPayloadContext {
        @Override
        public net.minecraft.world.entity.player.Player player() {
            return Minecraft.getInstance().player;
        }

        @Override
        public void enqueueWork(Runnable runnable) {
            Minecraft.getInstance().execute(runnable);
        }
    }
}
