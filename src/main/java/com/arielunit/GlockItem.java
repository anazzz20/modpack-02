package com.arielunit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Pistola de tiro instantâneo (sem queda de projétil): onde você mira, acerta.
 * É a "excelente pontaria" do Ariel.
 */
public class GlockItem extends Item {
    private static final double RANGE = 64.0;
    private static final float DAMAGE = 8.0f;   // 4 corações
    private static final int COOLDOWN_TICKS = 8;

    public GlockItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            Vec3d start = user.getEyePos();
            Vec3d look = user.getRotationVec(1.0f);
            Vec3d end = start.add(look.multiply(RANGE));

            // Primeiro vê se há parede no caminho
            BlockHitResult blockHit = world.raycast(new RaycastContext(
                    start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
            double maxDistSq = blockHit.getType() == HitResult.Type.MISS
                    ? RANGE * RANGE
                    : blockHit.getPos().squaredDistanceTo(start);

            // Depois procura um alvo (mob ou jogador) antes da parede
            Box box = user.getBoundingBox().stretch(look.multiply(RANGE)).expand(1.0);
            EntityHitResult hit = ProjectileUtil.raycast(
                    user, start, end, box, e -> !e.isSpectator() && e.canHit(), maxDistSq);

            double length = Math.sqrt(maxDistSq);
            if (hit != null) {
                Entity target = hit.getEntity();
                target.damage(world.getDamageSources().playerAttack(user), DAMAGE);
                length = hit.getPos().distanceTo(start);
            }

            // Rastro azul do tiro
            if (world instanceof ServerWorld serverWorld) {
                for (double d = 1.0; d < length; d += 0.7) {
                    Vec3d p = start.add(look.multiply(d));
                    serverWorld.spawnParticles(ParticleTypes.END_ROD, p.x, p.y - 0.15, p.z, 1, 0, 0, 0, 0);
                }
            }

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.0f, 1.6f);
        }

        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        return TypedActionResult.success(stack, world.isClient());
    }
}
