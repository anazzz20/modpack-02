package com.arielunit;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ArielSounds {
    public static final SoundEvent ENGINE = register("moto_engine");   // motor (pulso que se repete)
    public static final SoundEvent REV = register("moto_rev");         // aceleração
    public static final SoundEvent BRAKE = register("moto_brake");     // freio / pneu
    public static final SoundEvent CRASH = register("moto_crash");     // colisão
    public static final SoundEvent REFUEL = register("moto_refuel");   // abastecimento

    private static SoundEvent register(String name) {
        Identifier id = new Identifier(ArielUnit.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    /** Só serve para o jogo carregar esta classe (e registrar os sons) na hora certa. */
    public static void init() {}

    private ArielSounds() {}
}
