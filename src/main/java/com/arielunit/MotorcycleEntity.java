package com.arielunit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Moto dirigivel.
 *  - W acelera, S freia (e depois da marcha a re devagar), A/D viram.
 *  - Precisa de combustivel (gasolina) para acelerar. Abasteca clicando com um galao.
 *  - Bater (ataque) na moto recolhe ela de volta para o inventario, guardando a gasolina.
 *  - Sons: motor, aceleracao, freio, colisao e abastecimento (tocados pelo servidor, todos ouvem).
 */
public class MotorcycleEntity extends Entity {
    public static final float MAX_FUEL = 20.0f;          // litros no tanque

    private static final double MAX_SPEED = 1.0;          // blocos por tick (~72 km/h)
    private static final double MAX_REVERSE = 0.15;
    private static final double ACCEL = 0.022;
    private static final double BRAKE = 0.06;
    private static final double DRAG = 0.008;
    private static final float TURN_RATE = 5.0f;          // graus por tick

    private static final double IDLE_USE = 0.0002;        // litros por tick com alguem pilotando
    private static final double USE_PER_BLOCK = 0.003;    // litros por bloco percorrido

    // bits do estado que o cliente do piloto manda para o servidor (para os sons)
    public static final int FLAG_ACCEL = 1;
    public static final int FLAG_BRAKE = 2;
    public static final int FLAG_CRASH = 4;

    private static final TrackedData<Float> FUEL =
            DataTracker.registerData(MotorcycleEntity.class, TrackedDataHandlerRegistry.FLOAT);

    // --- lado do piloto (cliente) ---
    private double speed = 0.0;
    private boolean ctrlAccel, ctrlBrake, ctrlCrash;

    // --- lado do servidor ---
    private double lastX, lastZ;
    private boolean tracking = false;
    private double netSpeed = 0.0;
    private int netFlags = 0;
    private boolean pendingCrash = false;
    private int lastNetAge = -100;
    private int revCooldown, brakeCooldown, crashCooldown;

    public MotorcycleEntity(EntityType<? extends MotorcycleEntity> type, World world) {
        super(type, world);
    }

    // ---------- Combustivel ----------
    public float getFuel() { return this.dataTracker.get(FUEL); }
    public void setFuel(float liters) {
        this.dataTracker.set(FUEL, Math.max(0.0f, Math.min(MAX_FUEL, liters)));
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(FUEL, 0.0f);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setFuel(nbt.getFloat("Fuel"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("Fuel", getFuel());
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }

    // ---------- Comunicacao piloto -> servidor (usada pelo ArielUnitClient / ArielUnit) ----------
    public double getSpeed() { return speed; }

    /** Estado do ultimo tick do piloto. O bit de colisao e de uso unico. */
    public int consumeControlFlags() {
        int flags = (ctrlAccel ? FLAG_ACCEL : 0) | (ctrlBrake ? FLAG_BRAKE : 0) | (ctrlCrash ? FLAG_CRASH : 0);
        ctrlCrash = false;
        return flags;
    }

    /** Servidor: recebe a velocidade e as teclas do piloto. */
    public void applyClientState(float reportedSpeed, int flags) {
        this.netSpeed = reportedSpeed;
        this.netFlags = flags;
        this.lastNetAge = this.age;
        if ((flags & FLAG_CRASH) != 0) this.pendingCrash = true;
    }

    // ---------- Interacao ----------
    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // Abastecer com galao
        if (stack.getItem() instanceof GasCanItem can) {
            if (!this.getWorld().isClient) {
                if (getFuel() >= MAX_FUEL - 0.05f) {
                    player.sendMessage(Text.literal("Tanque cheio."), true);
                } else {
                    setFuel(getFuel() + can.getLiters());
                    if (!player.isCreative()) stack.decrement(1);
                    playSfx(ArielSounds.REFUEL, 1.0f, 1.0f);
                    player.sendMessage(Text.literal(String.format("Combustível: %.1f / %.0f L", getFuel(), MAX_FUEL)), true);
                }
            }
            return ActionResult.success(this.getWorld().isClient);
        }

        // Montar
        if (player.isSneaking()) return ActionResult.PASS;
        if (!this.getWorld().isClient) {
            return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (!this.getWorld().isClient && !this.isRemoved()
                && source.getAttacker() instanceof PlayerEntity player) {
            if (!player.isCreative()) {
                ItemStack drop = new ItemStack(ArielUnit.MOTORCYCLE_ITEM);
                drop.getOrCreateNbt().putFloat("Fuel", getFuel());
                this.dropStack(drop);
            }
            this.discard();
        }
        return true;
    }

    // ---------- Passageiros ----------
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().size() < 2;   // piloto + garupa
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof PlayerEntity p ? p : null;
    }

    @Override
    public double getMountedHeightOffset() {
        return 0.59;
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        if (!this.hasPassenger(passenger)) return;
        int index = this.getPassengerList().indexOf(passenger);
        double back = index == 0 ? -0.26 : -0.53;   // negativo = para tras da moto
        Vec3d offset = new Vec3d(0.0, 0.0, back).rotateY(-this.getYaw() * ((float) Math.PI / 180.0f));
        positionUpdater.accept(passenger,
                this.getX() + offset.x,
                this.getY() + this.getMountedHeightOffset() + passenger.getHeightOffset(),
                this.getZ() + offset.z);
    }

    // ---------- Fisica e controle ----------
    @Override
    public void tick() {
        super.tick();

        LivingEntity driver = this.getControllingPassenger();

        // Quem esta pilotando (cliente do jogador) calcula o movimento; o servidor confia nele.
        // Sem piloto, o servidor cuida da moto (ela desacelera e para).
        if (this.isLogicalSideForUpdatingMovement()) {
            controlTick(driver);
        }

        if (!this.getWorld().isClient) {
            serverTick(driver);
        }
    }

    private void controlTick(LivingEntity driver) {
        boolean engine = driver != null && getFuel() > 0.0f;
        float throttle = 0.0f;
        float steer = 0.0f;
        if (driver instanceof PlayerEntity p) {
            throttle = p.forwardSpeed;    // W = positivo, S = negativo
            steer = p.sidewaysSpeed;      // A (esquerda) = positivo
        }

        ctrlAccel = engine && throttle > 0.0f;
        ctrlBrake = false;

        if (ctrlAccel) {
            speed = Math.min(MAX_SPEED, speed + ACCEL);                 // acelerar
        } else if (driver != null && throttle < 0.0f) {
            if (speed > 0.02) {
                speed = Math.max(0.0, speed - BRAKE);                    // freio
                ctrlBrake = true;
            } else if (engine) {
                speed = Math.max(-MAX_REVERSE, speed - 0.015);           // re
            } else {
                speed = 0.0;
            }
        } else if (speed > 0.0) {
            speed = Math.max(0.0, speed - DRAG);                         // solta o acelerador
        } else if (speed < 0.0) {
            speed = Math.min(0.0, speed + DRAG);
        }

        // Curva: precisa de um pouco de velocidade e fica mais suave em velocidade alta
        float yaw = this.getYaw();
        double speedFactor = Math.min(1.0, Math.abs(speed) / 0.25);
        double turn = TURN_RATE * (1.0 - 0.55 * Math.min(1.0, Math.abs(speed) / MAX_SPEED)) * speedFactor;
        yaw -= (float) (steer * turn * (speed >= 0.0 ? 1.0 : -1.0));
        this.setYaw(yaw);

        double rad = Math.toRadians(yaw);
        double vx = -Math.sin(rad) * speed;
        double vz = Math.cos(rad) * speed;
        double vy = this.getVelocity().y;
        if (!this.hasNoGravity()) vy -= 0.08;
        vy *= 0.98;

        // Sobe degraus de 1 bloco; se nao der, e uma batida
        if (this.horizontalCollision && Math.abs(speed) > 0.05) {
            Box ahead = this.getBoundingBox().offset(vx * 2.0, 1.05, vz * 2.0);
            if (this.isOnGround() && this.getWorld().isSpaceEmpty(this, ahead)) {
                vy = 0.45;
            } else {
                if (Math.abs(speed) > 0.3) ctrlCrash = true;   // bateu forte: toca o som de colisao
                speed *= 0.4;
            }
        }

        this.setVelocity(vx, vy, vz);
        this.move(MovementType.SELF, this.getVelocity());
    }

    // ---------- Servidor: consumo, painel e sons ----------
    private void playSfx(SoundEvent sound, float volume, float pitch) {
        this.getWorld().playSound(null, getX(), getY(), getZ(), sound, SoundCategory.NEUTRAL, volume, pitch);
    }

    private void serverTick(LivingEntity driver) {
        double dx = getX() - lastX;
        double dz = getZ() - lastZ;
        double dist = tracking ? Math.sqrt(dx * dx + dz * dz) : 0.0;
        tracking = true;
        lastX = getX();
        lastZ = getZ();

        if (revCooldown > 0) revCooldown--;
        if (brakeCooldown > 0) brakeCooldown--;
        if (crashCooldown > 0) crashCooldown--;

        // se o cliente do piloto parou de mandar estado (lag, saiu da moto), zera
        if (this.age - lastNetAge > 5) {
            netSpeed = 0.0;
            netFlags = 0;
        }

        if (driver == null) {
            pendingCrash = false;
            return;
        }

        boolean hasFuel = getFuel() > 0.0f;
        if (hasFuel) {
            double used = IDLE_USE + dist * USE_PER_BLOCK;
            setFuel((float) (getFuel() - used));
        }

        double ratio = Math.min(1.0, Math.abs(netSpeed) / MAX_SPEED);
        boolean accelerating = (netFlags & FLAG_ACCEL) != 0;
        boolean braking = (netFlags & FLAG_BRAKE) != 0;

        // Motor: um pulso curto a cada 4 ticks; mais agudo quanto mais rapido
        if (hasFuel && this.age % 4 == 0) {
            float pitch = (float) (0.7 + 0.9 * ratio + (accelerating ? 0.1 : 0.0));
            playSfx(ArielSounds.ENGINE, (float) (0.45 + 0.3 * ratio), pitch);
        }
        // Aceleracao: "rev" enquanto acelera em velocidades baixas/medias
        if (hasFuel && accelerating && ratio < 0.85 && revCooldown <= 0) {
            playSfx(ArielSounds.REV, 0.7f, (float) (0.85 + 0.5 * ratio));
            revCooldown = 16;
        }
        // Freio: pneu cantando
        if (braking && netSpeed > 0.25 && brakeCooldown <= 0) {
            playSfx(ArielSounds.BRAKE, (float) (0.5 + 0.4 * ratio), 0.9f + this.random.nextFloat() * 0.2f);
            brakeCooldown = 14;
        }
        // Colisao
        if (pendingCrash) {
            pendingCrash = false;
            if (crashCooldown <= 0) {
                playSfx(ArielSounds.CRASH, 1.0f, 0.85f + this.random.nextFloat() * 0.3f);
                crashCooldown = 10;
            }
        }

        if (driver instanceof PlayerEntity p && this.age % 5 == 0) {
            String text = hasFuel
                    ? String.format("Combustível: %.1f L   |   %d km/h", getFuel(), (int) Math.round(Math.abs(netSpeed) * 72.0))
                    : "Sem combustível! Abasteça com um galão.";
            p.sendMessage(Text.literal(text), true);
        }
    }
}
