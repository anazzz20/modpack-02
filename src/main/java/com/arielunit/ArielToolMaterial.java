package com.arielunit;

import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public enum ArielToolMaterial implements ToolMaterial {
    INSTANCE;

    @Override public int getDurability() { return 800; }
    @Override public float getMiningSpeedMultiplier() { return 1.0f; }
    @Override public float getAttackDamage() { return 1.0f; }
    @Override public int getMiningLevel() { return 2; }
    @Override public int getEnchantability() { return 15; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.ofItems(Items.IRON_INGOT); }
}
