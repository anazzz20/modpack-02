package com.arielunit.client;

import com.arielunit.ArielUnit;
import com.arielunit.MotorcycleEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;

public class ArielUnitClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ArielUnit.MOTORCYCLE, MotorcycleRenderer::new);

        // Enquanto pilota, manda a velocidade e as teclas (acelerar/freiar/bater) para o servidor tocar os sons
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player != null && player.getVehicle() instanceof MotorcycleEntity bike
                    && bike.getControllingPassenger() == player) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeFloat((float) bike.getSpeed());
                buf.writeByte(bike.consumeControlFlags());
                ClientPlayNetworking.send(ArielUnit.BIKE_STATE, buf);
            }
        });
    }
}
