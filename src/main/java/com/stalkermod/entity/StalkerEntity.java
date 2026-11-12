package com.stalkermod.entity;

import com.stalkermod.client.StalkerClientHooks;
import com.stalkermod.event.Vis;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * El Stalker (ahora 2D: el renderer dibuja un PNG recortado que mira siempre a la camara).
 * No ataca, no se puede golpear, no empuja, no tiene colision.
 *
 * Modos:
 *  - MODE_STALK: se asoma por el borde de un arbol/pared. Si lo miras ~4 s, o te acercas, se da cuenta
 *                y se esconde DETRAS de la cobertura (desliza hacia atras y desaparece).
 *  - MODE_DOOR : cara en la ventana junto a la puerta. La secuencia la dirige DoorEvent.
 */
public class StalkerEntity extends Mob {

    public static final int MODE_STALK = 0;
    public static final int MODE_DOOR = 1;
    public static final int MODE_WATCH = 2;   // media cabeza observandote desde detras de un muro

    public static final int SPRITE_A = 0;     // figura completa de lado
    public static final int SPRITE_B = 1;     // cabeza + brazo levantado
    public static final int SPRITE_HEAD = 2;  // solo la cara

    public static final int POSE_PEEK = 0;
    public static final int POSE_WINDOW = 1;
    public static final int POSE_REACH = 2;
    public static final int POSE_POUND = 3;
    public static final int POSE_BROKE = 4;

    /** Subtipos del modo MODE_WATCH (se guardan en DATA_POSE). */
    public static final int WATCH_SIDE = 0;   // la cabeza se asoma por el costado de un muro/esquina
    public static final int WATCH_TOP = 1;    // la cabeza se asoma por arriba de un muro

    private static final EntityDataAccessor<Float> DATA_REVEAL =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPRITE =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POSE =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LEAN =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_BODY_YAW =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.FLOAT);

    // ---- servidor ----
    private UUID targetUuid;
    private int life;
    private int seenTicks;
    private int noticeTicks = 80;
    private boolean hiding;
    private float hideStep = 0.07F;
    private float serverReveal = 1.0F;

    // ---- cliente ----
    private float reveal = 1.0F;
    private float prevReveal = 1.0F;
    private boolean soundStarted;

    public StalkerEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
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
        this.entityData.define(DATA_REVEAL, 1.0F);
        this.entityData.define(DATA_MODE, MODE_STALK);
        this.entityData.define(DATA_SPRITE, SPRITE_A);
        this.entityData.define(DATA_POSE, POSE_PEEK);
        this.entityData.define(DATA_TARGET, -1);
        this.entityData.define(DATA_LEAN, 1);
        this.entityData.define(DATA_BODY_YAW, 0.0F);
    }

    // =====================================================================
    //  Inicializacion
    // =====================================================================

    /**
     * @param bodyYaw     yaw mirando al jugador (sirve para saber hacia que lado se asoma)
     * @param lean        +1 / -1: lado (respecto a la "izquierda" de bodyYaw) por el que sobresale
     * @param sprite      SPRITE_A o SPRITE_B
     * @param noticeTicks ticks que puedes mirarlo antes de que se esconda
     */
    public void initStalk(ServerPlayer player, float bodyYaw, int lean, int sprite, int noticeTicks) {
        this.targetUuid = player.getUUID();
        this.entityData.set(DATA_TARGET, player.getId());
        this.entityData.set(DATA_MODE, MODE_STALK);
        this.entityData.set(DATA_SPRITE, sprite);
        this.entityData.set(DATA_POSE, POSE_PEEK);
        this.entityData.set(DATA_LEAN, lean);
        this.entityData.set(DATA_BODY_YAW, bodyYaw);
        this.noticeTicks = Math.max(20, noticeTicks);
    }

    /** Modo puerta: lean = lado (+1/-1) donde esta la puerta respecto a la "izquierda" de bodyYaw. */
    public void initDoor(ServerPlayer player, float bodyYaw, int lean) {
        this.targetUuid = player.getUUID();
        this.entityData.set(DATA_TARGET, player.getId());
        this.entityData.set(DATA_MODE, MODE_DOOR);
        this.entityData.set(DATA_SPRITE, SPRITE_HEAD);
        this.entityData.set(DATA_POSE, POSE_WINDOW);
        this.entityData.set(DATA_LEAN, lean);
        this.entityData.set(DATA_BODY_YAW, bodyYaw);
    }

    /**
     * Modo media cabeza. La posicion de la entidad es el CENTRO de la cabeza.
     * @param lean    +1/-1: hacia que lado (respecto a la "izquierda" de bodyYaw) queda la mitad visible
     * @param variant WATCH_SIDE o WATCH_TOP
     */
    public void initWatch(ServerPlayer player, float bodyYaw, int lean, int variant, int noticeTicks) {
        this.targetUuid = player.getUUID();
        this.entityData.set(DATA_TARGET, player.getId());
        this.entityData.set(DATA_MODE, MODE_WATCH);
        this.entityData.set(DATA_SPRITE, SPRITE_HEAD);
        this.entityData.set(DATA_POSE, variant);
        this.entityData.set(DATA_LEAN, lean);
        this.entityData.set(DATA_BODY_YAW, bodyYaw);
        this.noticeTicks = Math.max(20, noticeTicks);
    }

    public void setPoseState(int pose) {
        this.entityData.set(DATA_POSE, pose);
    }

    public void setSprite(int sprite) {
        this.entityData.set(DATA_SPRITE, sprite);
    }

    /** Se esconde (modo acechar: desliza detras de la cobertura) y luego se elimina. */
    public void startHide(int ticks) {
        if (!this.hiding) {
            this.hiding = true;
            this.hideStep = 1.0F / Math.max(1, ticks);
            if (this.getMode() == MODE_STALK) {
                this.rustle(0.7F, 0.8F);
            }
        }
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

        ServerPlayer player = null;
        if (this.targetUuid == null) {
            // Creado con el huevo generador: adopta al jugador mas cercano.
            Player nearest = this.level().getNearestPlayer(this, 64.0D);
            if (nearest instanceof ServerPlayer sp) {
                this.targetUuid = sp.getUUID();
                this.entityData.set(DATA_TARGET, sp.getId());
                double dx = sp.getX() - this.getX();
                double dz = sp.getZ() - this.getZ();
                float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
                this.entityData.set(DATA_BODY_YAW, yaw);
                player = sp;
            }
        } else {
            Player p = this.level().getPlayerByUUID(this.targetUuid);
            if (p instanceof ServerPlayer sp) {
                player = sp;
            }
        }
        if (player == null || !player.isAlive() || player.isSpectator()) {
            this.discard();
            return;
        }

        if (this.hiding) {
            this.serverReveal -= this.hideStep;
        }
        this.serverReveal = Mth.clamp(this.serverReveal, 0.0F, 1.0F);
        this.entityData.set(DATA_REVEAL, this.serverReveal);
        if (this.hiding && this.serverReveal <= 0.0F) {
            this.discard();
            return;
        }

        if (this.getMode() == MODE_STALK) {
            if (this.life == 1) {
                this.rustle(1.0F, 0.7F);       // ruido de hojas al aparecer
            } else if (this.life == 18) {
                this.rustle(0.6F, 0.9F);
            }
            this.stalkLogic(player);
        } else if (this.getMode() == MODE_WATCH) {
            this.watchLogic(player);
        } else if (this.life > 20 * 120) {
            this.discard(); // seguro por si el evento de puerta murio
        }
    }

    /** Reglas del evento "acechar". */
    private void stalkLogic(ServerPlayer player) {
        if (this.hiding) {
            return;
        }
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        // ¿El jugador lo esta mirando (y lo ve de verdad)?
        Vec3 eye = player.getEyePosition();
        Vec3 head = this.getVisiblePoint();
        Vec3 to = head.subtract(eye);
        boolean seen = false;
        if (to.lengthSqr() > 1.0E-4D) {
            seen = player.getLookAngle().dot(to.normalize()) > 0.90D
                    && Vis.clear((ServerLevel) this.level(), eye, head);
        }
        if (seen) {
            this.seenTicks++;
        } else if (this.seenTicks > 0 && this.life % 4 == 0) {
            this.seenTicks--;
        }

        if (dist < StalkerConfig.STALK_APPROACH_DISTANCE.get()) {
            this.startHide(8);                 // te acercaste: se esconde rapido
        } else if (this.seenTicks >= this.noticeTicks) {
            this.startHide(14);                // se dio cuenta de que lo viste: se esconde
        } else if (this.life > StalkerConfig.STALK_MAX_LIFE_SECONDS.get() * 20) {
            this.startHide(14);
        } else if (dist > 80.0D) {
            this.discard();
        }
    }

    /** Reglas del evento "media cabeza": si la miras ~8 s desaparece LENTAMENTE. */
    private void watchLogic(ServerPlayer player) {
        if (this.hiding) {
            return;
        }
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        Vec3 eye = player.getEyePosition();
        Vec3 head = this.getVisiblePoint();
        Vec3 to = head.subtract(eye);
        boolean seen = false;
        if (to.lengthSqr() > 1.0E-4D) {
            seen = player.getLookAngle().dot(to.normalize()) > 0.93D
                    && Vis.clear((ServerLevel) this.level(), eye, head);
        }
        if (seen) {
            this.seenTicks++;
        } else if (this.seenTicks > 0 && this.life % 4 == 0) {
            this.seenTicks--;
        }

        if (dist < 3.0D) {
            this.startHide(20);                 // demasiado cerca: se va rapido
        } else if (this.seenTicks >= this.noticeTicks) {
            this.startHide(70);                 // lo miraste el tiempo suficiente: desaparece lentamente
        } else if (this.life > 20 * 60) {
            this.startHide(70);
        }
    }

    private void rustle(float volume, float pitch) {
        this.level().playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(),
                SoundEvents.GRASS_BREAK, SoundSource.AMBIENT, volume, pitch);
    }

    private void clientTick() {
        this.prevReveal = this.reveal;
        this.reveal = this.entityData.get(DATA_REVEAL);

        if (!this.soundStarted && this.getMode() != MODE_DOOR) {
            this.soundStarted = true;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> StalkerClientHooks.startAudio(this));
        }
    }

    // =====================================================================
    //  Getters
    // =====================================================================

    public int getMode() { return this.entityData.get(DATA_MODE); }
    public int getSprite() { return this.entityData.get(DATA_SPRITE); }
    public int getPoseState() { return this.entityData.get(DATA_POSE); }
    public int getLean() { return this.entityData.get(DATA_LEAN); }
    public int getTargetId() { return this.entityData.get(DATA_TARGET); }
    public float getBodyYawData() { return this.entityData.get(DATA_BODY_YAW); }
    public float getReveal() { return this.reveal; }
    public float getPrevReveal() { return this.prevReveal; }

    /** Punto del mundo donde esta el centro de la cabeza (modo acechar). */
    public Vec3 getVisiblePoint() {
        if (this.getMode() == MODE_WATCH) {
            // la mitad visible de la cabeza: arriba (WATCH_TOP) o hacia el lado libre (WATCH_SIDE)
            if (this.getPoseState() == WATCH_TOP) {
                return new Vec3(this.getX(), this.getY() + 0.22D, this.getZ());
            }
            float yawW = this.getBodyYawData() * ((float) Math.PI / 180.0F);
            return new Vec3(this.getX() + this.getLean() * Mth.cos(yawW) * 0.22D, this.getY(),
                    this.getZ() + this.getLean() * Mth.sin(yawW) * 0.22D);
        }
        SpriteInfo info = SpriteInfo.get(this.getSprite());
        float yaw = this.getBodyYawData() * ((float) Math.PI / 180.0F);
        double sx = this.getLean() * Mth.cos(yaw);
        double sz = this.getLean() * Mth.sin(yaw);
        double lat = info.headLateral();
        return new Vec3(this.getX() + sx * lat, this.getY() + info.headHeight(), this.getZ() + sz * lat);
    }

    // =====================================================================
    //  Comportamiento "fantasma": sin colision, sin dano, sin empujar
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
