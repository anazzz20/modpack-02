package com.arielunit;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public enum ArielArmorMaterial implements ArmorMaterial {
    INSTANCE;

    @Override
    public int getDurability(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 13 * 30;
            case CHESTPLATE -> 15 * 30;
            case LEGGINGS -> 16 * 30;
            case BOOTS -> 11 * 30;
        };
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
        };
    }

    @Override public int getEnchantability() { return 15; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.ofItems(Items.IRON_INGOT); }
    // Define quais arquivos de textura do traje o jogo procura: ariel_suit_layer_1.png e _layer_2.png
    @Override public String getName() { return "ariel_unit:ariel_suit"; }
    @Override public float getToughness() { return 2.0f; }
    @Override public float getKnockbackResistance() { return 0.0f; }
}
