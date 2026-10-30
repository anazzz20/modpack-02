package com.arielunit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ArielUnit implements ModInitializer {
    public static final String MOD_ID = "ariel_unit";

    /** Canal em que o cliente do piloto manda velocidade e teclas da moto para o servidor. */
    public static final Identifier BIKE_STATE = new Identifier(MOD_ID, "bike_state");

    // Armas
    public static final Item KNIFE = register("ariel_knife",
            new SwordItem(ArielToolMaterial.INSTANCE, 3, -1.0f, new FabricItemSettings()));
    public static final Item BOW = register("ariel_bow",
            new ArielBow(new FabricItemSettings().maxDamage(800)));
    public static final Item GLOCK = register("ariel_glock",
            new GlockItem(new FabricItemSettings().maxCount(1)));

    // Traje (a máscara é o capacete)
    public static final Item MASK = register("ariel_mask",
            new ArmorItem(ArielArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new FabricItemSettings()));
    public static final Item CHESTPLATE = register("ariel_chestplate",
            new ArmorItem(ArielArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new FabricItemSettings()));
    public static final Item LEGGINGS = register("ariel_leggings",
            new ArmorItem(ArielArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new FabricItemSettings()));
    public static final Item BOOTS = register("ariel_boots",
            new ArmorItem(ArielArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new FabricItemSettings()));

    // Galões de gasolina
    public static final Item GAS_CAN_5L = register("gas_can_5l",
            new GasCanItem(new FabricItemSettings().maxCount(16), 5.0f));
    public static final Item GAS_CAN_10L = register("gas_can_10l",
            new GasCanItem(new FabricItemSettings().maxCount(16), 10.0f));
    public static final Item GAS_CAN_20L = register("gas_can_20l",
            new GasCanItem(new FabricItemSettings().maxCount(8), 20.0f));

    // Moto (item para colocar no mundo + entidade que se dirige)
    public static final Item MOTORCYCLE_ITEM = register("ariel_motorcycle",
            new MotorcycleItem(new FabricItemSettings().maxCount(1)));
    public static final EntityType<MotorcycleEntity> MOTORCYCLE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MOD_ID, "ariel_motorcycle"),
            FabricEntityTypeBuilder.<MotorcycleEntity>create(SpawnGroup.MISC, MotorcycleEntity::new)
                    .dimensions(EntityDimensions.fixed(1.4f, 1.2f))
                    .trackRangeBlocks(96)
                    .trackedUpdateRate(1)
                    .build());

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), item);
    }

    @Override
    public void onInitialize() {
        ArielSounds.init();

        ServerPlayNetworking.registerGlobalReceiver(BIKE_STATE, (server, player, handler, buf, responseSender) -> {
            float reportedSpeed = buf.readFloat();
            int flags = buf.readByte();
            server.execute(() -> {
                if (player.getVehicle() instanceof MotorcycleEntity bike && bike.getControllingPassenger() == player) {
                    bike.applyClientState(reportedSpeed, flags);
                }
            });
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(KNIFE);
            entries.add(BOW);
            entries.add(GLOCK);
            entries.add(MASK);
            entries.add(CHESTPLATE);
            entries.add(LEGGINGS);
            entries.add(BOOTS);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(MOTORCYCLE_ITEM);
            entries.add(GAS_CAN_5L);
            entries.add(GAS_CAN_10L);
            entries.add(GAS_CAN_20L);
        });
    }
}
