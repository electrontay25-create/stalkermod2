package com.stalkermod.entity;

import com.stalkermod.client.StalkerClientHooks;
import com.stalkermod.event.Vis;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.UUID;

/**
 * Anderdingus: aparece "dormido" (pose de pie). Si lo miras ~3 s se despierta (animacion del video + empieza la cancion)
 * y te PERSIGUE. Su velocidad sigue el ritmo de la cancion: cada vez mas rapido. Si te alcanza, te mata de un golpe y
 * desaparece; si termina la cancion sin alcanzarte, desaparece.
 *
 * Es una entidad con movimiento real (fisica, saltos, rodea obstaculos) pero invulnerable y sin colision con el jugador.
 */
public class AnderdingusEntity extends Mob {

    public static final int STATE_SLEEP = 0;
    public static final int STATE_WAKE = 1;
    public static final int STATE_CHASE = 2;

    /** Duracion de la animacion de despertar (5.0 s). Despues empieza la persecucion. */
    public static final int WAKE_TICKS = 100;
    /** Duracion total desde que despierta hasta que termina la cancion (56 s). */
    public static final int SONG_TICKS = (int) (SongData.LENGTH_SECONDS * 20.0F);

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(AnderdingusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET =
            SynchedEntityData.defineId(AnderdingusEntity.class, EntityDataSerializers.INT);

    // ---- servidor ----
    private UUID targetUuid;
    private ServerPlayer chaseTarget;
    private int life;
    private int seenTicks;
    private int noticeTicks = 60;
    private int wakeTicks;            // ticks desde que empezo a despertar (= tiempo de la cancion)
    private int stuckTicks;
    private int sidestepTicks;
    private int sidestepSign = 1;
    private double lastX;
    private double lastZ;

    // ---- cliente ----
    private int lastState = -1;
    private int stateTicks;
    private boolean songStarted;

    public AnderdingusEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STATE, STATE_SLEEP);
        this.entityData.define(DATA_TARGET, -1);
    }

    public void init(ServerPlayer player, int noticeTicks) {
        this.targetUuid = player.getUUID();
        this.entityData.set(DATA_TARGET, player.getId());
        this.noticeTicks = Math.max(20, noticeTicks);
    }

    // =====================================================================
    //  Tick
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.clientTick();
        } else {
            this.serverTick();
        }
    }

    private void serverTick() {
        this.life++;

        Player p = this.targetUuid == null ? null : this.level().getPlayerByUUID(this.targetUuid);
        if (!(p instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
            this.discard();
            return;
        }
        this.chaseTarget = player;

        int state = this.getState();
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (state == STATE_SLEEP) {
            // Dormido: si lo miras ~3 s (o te acercas mucho) se despierta.
            Vec3 eye = player.getEyePosition();
            Vec3 head = new Vec3(this.getX(), this.getY() + 1.8D, this.getZ());
            Vec3 to = head.subtract(eye);
            boolean seen = to.lengthSqr() > 1.0E-4D
                    && player.getLookAngle().dot(to.normalize()) > 0.88D
                    && Vis.clear((ServerLevel) this.level(), eye, head);
            if (seen) {
                this.seenTicks++;
            } else if (this.seenTicks > 0 && this.life % 4 == 0) {
                this.seenTicks--;
            }
            if (this.seenTicks >= this.noticeTicks || dist < 4.0D) {
                this.wakeTicks = 0;
                this.entityData.set(DATA_STATE, STATE_WAKE);
            } else if (this.life > 60 * 20 || dist > 90.0D) {
                this.discard();   // nunca lo miraste: se va
            }
            return;
        }

        this.wakeTicks++;
        if (state == STATE_WAKE && this.wakeTicks >= WAKE_TICKS) {
            this.entityData.set(DATA_STATE, STATE_CHASE);
            this.lastX = this.getX();
            this.lastZ = this.getZ();
        }
        if (state == STATE_CHASE) {
            // ¡Te alcanzo!
            if (dist < 1.6D && Math.abs(player.getY() - this.getY()) < 2.6D) {
                this.kill(player);
                return;
            }
            this.checkStuck();
        }
        if (this.wakeTicks >= SONG_TICKS || dist > 120.0D) {
            this.discard();       // termino la cancion sin alcanzarte
        }
    }

    /** Muerte de un solo golpe (tambien en creativo o con totem). Despues Anderdingus desaparece. */
    private void kill(ServerPlayer player) {
        DamageSource hit = this.damageSources().mobAttack(this);
        player.hurt(hit, 1000.0F);
        if (player.isAlive()) {
            player.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
        }
        this.discard();
    }

    /** Si avanza muy poco, rodea el obstaculo por un lado durante un rato. */
    private void checkStuck() {
        if (this.life % 10 != 0) {
            return;
        }
        double moved = Math.sqrt((this.getX() - this.lastX) * (this.getX() - this.lastX)
                + (this.getZ() - this.lastZ) * (this.getZ() - this.lastZ));
        this.lastX = this.getX();
        this.lastZ = this.getZ();
        if (moved < 0.5D) {
            this.stuckTicks++;
            if (this.stuckTicks >= 2) {
                this.stuckTicks = 0;
                this.sidestepTicks = 30;
                this.sidestepSign = -this.sidestepSign;
            }
        } else {
            this.stuckTicks = 0;
        }
    }

    /** Velocidad actual (bloques/segundo): sigue la intensidad de la cancion y salta con cada golpe de ritmo. */
    private double currentSpeed() {
        float t = this.wakeTicks / 20.0F;
        double min = StalkerConfig.ANDERDINGUS_MIN_SPEED.get();
        double max = StalkerConfig.ANDERDINGUS_MAX_SPEED.get();
        double base = min + (max - min) * SongData.curve(t);
        return base * (1.0D + 0.30D * SongData.pulse(t));
    }

    /**
     * El movimiento se fija aqui (antes de la fisica de Minecraft): velocidad exacta hacia el jugador,
     * saltando cuando choca con un bloque. Dormido o despertando se queda quieto.
     */
    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            Vec3 d = this.getDeltaMovement();
            if (this.getState() == STATE_CHASE && this.chaseTarget != null && this.chaseTarget.isAlive()) {
                double dx = this.chaseTarget.getX() - this.getX();
                double dz = this.chaseTarget.getZ() - this.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.01D) {
                    double ux = dx / len;
                    double uz = dz / len;
                    if (this.sidestepTicks > 0) {
                        this.sidestepTicks--;
                        double a = Math.toRadians(70.0D * this.sidestepSign);
                        double rx = ux * Math.cos(a) - uz * Math.sin(a);
                        double rz = ux * Math.sin(a) + uz * Math.cos(a);
                        ux = rx;
                        uz = rz;
                    }
                    double v = this.currentSpeed() / 20.0D;
                    double vy = this.isInWater() ? 0.08D : d.y;
                    this.setDeltaMovement(ux * v, vy, uz * v);
                    if (this.horizontalCollision) {
                        this.getJumpControl().jump();
                    }
                }
            } else {
                this.setDeltaMovement(0.0D, d.y, 0.0D);
            }
        }
        super.aiStep();
    }

    private void clientTick() {
        int s = this.entityData.get(DATA_STATE);
        if (s != this.lastState) {
            this.lastState = s;
            this.stateTicks = 0;
            if (s != STATE_SLEEP && !this.songStarted) {
                this.songStarted = true;   // la cancion suena desde que despierta
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> StalkerClientHooks.startAnderdingusSong(this));
            }
        } else {
            this.stateTicks++;
        }
    }

    // =====================================================================
    //  Getters
    // =====================================================================

    public int getState() { return this.entityData.get(DATA_STATE); }
    public int getTargetId() { return this.entityData.get(DATA_TARGET); }
    public int getStateTicks() { return this.stateTicks; }

    // =====================================================================
    //  Ni se puede golpear ni empuja al jugador
    // =====================================================================

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isAttackable() { return false; }

    @Override
    protected void pushEntities() {
        // no empuja a nadie
    }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public boolean fireImmune() { return true; }
}
