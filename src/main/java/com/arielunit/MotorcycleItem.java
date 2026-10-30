package com.arielunit;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MotorcycleItem extends Item {
    private static final float STARTING_FUEL = 5.0f;   // moto nova ja vem com um pouco de gasolina

    public MotorcycleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;

        BlockPos pos = ctx.getBlockPos().offset(ctx.getSide());
        MotorcycleEntity bike = ArielUnit.MOTORCYCLE.create(world);
        if (bike == null) return ActionResult.FAIL;

        bike.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ctx.getPlayerYaw(), 0.0f);
        if (!world.isSpaceEmpty(bike)) return ActionResult.FAIL;

        ItemStack stack = ctx.getStack();
        NbtCompound nbt = stack.getNbt();
        bike.setFuel(nbt != null && nbt.contains("Fuel") ? nbt.getFloat("Fuel") : STARTING_FUEL);

        world.spawnEntity(bike);
        PlayerEntity player = ctx.getPlayer();
        if (player == null || !player.isCreative()) stack.decrement(1);
        return ActionResult.CONSUME;
    }
}
