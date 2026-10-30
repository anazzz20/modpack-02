package com.arielunit;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class GasCanItem extends Item {
    private final float liters;

    public GasCanItem(Settings settings, float liters) {
        super(settings);
        this.liters = liters;
    }

    public float getLiters() {
        return liters;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(String.format("Gasolina: %.0f L", liters)).formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Clique direito na moto para abastecer").formatted(Formatting.DARK_GRAY));
    }
}
