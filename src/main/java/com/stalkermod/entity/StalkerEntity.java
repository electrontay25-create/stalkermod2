package com.stalkermod.entity;

import com.stalkermod.client.StalkerClientHooks;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.UUID;

/**
 * El Stalker. No ataca, no se puede golpear, no empuja, no tiene colision.
 * Solo existe para asustar: el cuerpo queda fijo y la cabeza sigue al jugador.
 *
 * Hay dos modos:
 *  - MODE_STALK: se asoma detras de un arbol/pared y desaparece si te acercas o lo miras mucho.
 *  - MODE_DOOR : aparece en la ventana junto a la puerta. Su secuencia la dirige DoorEvent.
 */
public class StalkerEntity extends Mob {

    public static final int MODE_STALK = 0;
    public static final int MODE_DOOR = 1;

    public static final int POSE_PEEK = 0;    // asomado detras de un arbol/pared
    public static final int POSE_WINDOW = 1;  // solo la cara en la ventana
    public static final int POSE_REACH = 2;   // brazo intentando abrir la puerta
    public static final int POSE_POUND = 3;   // golpeando la puerta con fuerza
    public static final int POSE_BROKE = 4;   // despues de romperla, estirando el brazo

    private static final EntityDataAccessor<Float> DATA_ALPHA =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POSE =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LEAN =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ARM_LEFT =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_BODY_YAW =
            SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.FLOAT);

    // ---- estado del servidor ----
    private UUID targetUuid;
    private int life;
    private int seenTicks;
    private boolean fadingOut;
    private float fadeOutStep = 0.05F;
    private float serverAlpha = 0.0F;

    // ---- estado del cliente (solo visual) ----
    private float alpha = 0.0F;
    private float prevAlpha = 0.0F;
    private float headYaw;
    private boolean headInit;
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
        this.entityData.define(DATA_ALPHA, 0.0F);
        this.entityData.define(DATA_MODE, MODE_STALK);
        this.entityData.define(DATA_POSE, POSE_PEEK);
        this.entityData.define(DATA_TARGET, -1);
        this.entityData.define(DATA_LEAN, 1);
        this.entityData.define(DATA_ARM_LEFT, false);
        this.entityData.define(DATA_BODY_YAW, 0.0F);
    }

    // =====================================================================
    //  Inicializacion (la llaman StalkSpawner y DoorEvent antes de añadirlo al mundo)
    // =====================================================================

    /** Modo acechar: cuerpo fijo mirando al jugador, inclinado hacia el lado por el que se asoma. */
    public void initStalk(ServerPlayer player, float bodyYaw, int lean) {
        this.targetUuid = player.getUUID();
        this.entityData.set(DATA_TARGET, player.getId());
        this.entityData.set(DATA_MODE, MODE_STALK);
        this.entityData.set(DATA_POSE, POSE_PEEK);
        this.entityData.set(DATA_LEAN, lean);
        this.entityData.set(DATA_BODY_YAW, bodyYaw);
        applyYaw(bodyYaw);
    }

    /** Modo puerta: de frente a la pared (mirando hacia adentro de la casa). */
    public void initDoor(ServerPlayer player, float bodyYaw, boolean armLeft) {
        this.targetUuid = player.getUUID();
        this.entityData.set(DATA_TARGET, player.getId());
        this.entityData.set(DATA_MODE, MODE_DOOR);
        this.entityData.set(DATA_POSE, POSE_WINDOW);
        this.entityData.set(DATA_LEAN, 0);
        this.entityData.set(DATA_ARM_LEFT, armLeft);
        this.entityData.set(DATA_BODY_YAW, bodyYaw);
        applyYaw(bodyYaw);
    }

    private void applyYaw(float yaw) {
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRot = yaw;
        this.yHeadRotO = yaw;
        this.headYaw = yaw;
        this.headInit = true;
    }

    public void setPoseState(int pose) {
        this.entityData.set(DATA_POSE, pose);
    }

    /** Empieza a desvanecerse; al llegar a transparente total se elimina. */
    public void startFadeOut(int ticks) {
        if (!this.fadingOut) {
            this.fadingOut = true;
            this.fadeOutStep = 1.0F / Math.max(1, ticks);
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

        // Buscar al jugador objetivo. Si se creo con el spawn egg no tiene objetivo: adopta al mas cercano.
        ServerPlayer player = null;
        if (this.targetUuid == null) {
            Player nearest = this.level().getNearestPlayer(this, 64.0D);
            if (nearest instanceof ServerPlayer sp) {
                this.targetUuid = sp.getUUID();
                this.entityData.set(DATA_TARGET, sp.getId());
                this.entityData.set(DATA_MODE, MODE_STALK);
                this.entityData.set(DATA_BODY_YAW, this.getYRot());
                this.applyYaw(this.getYRot());
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

        // Transparencia: aparece rapido, se desvanece segun fadeOutStep.
        if (this.fadingOut) {
            this.serverAlpha -= this.fadeOutStep;
        } else if (this.serverAlpha < 1.0F) {
            this.serverAlpha += 0.2F;
        }
        this.serverAlpha = Mth.clamp(this.serverAlpha, 0.0F, 1.0F);
        this.entityData.set(DATA_ALPHA, this.serverAlpha);
        if (this.fadingOut && this.serverAlpha <= 0.0F) {
            this.discard();
            return;
        }

        this.updateRotations(player);

        if (this.getMode() == MODE_STALK) {
            this.stalkLogic(player);
        } else if (this.life > 20 * 120) {
            // Seguro: si por alguna razon el evento de puerta murio, no dejamos el Stalker colgado.
            this.discard();
        }
    }

    /** Reglas del evento "acechar". */
    private void stalkLogic(ServerPlayer player) {
        if (this.fadingOut) {
            return;
        }
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        // ¿El jugador lo esta mirando de frente?
        Vec3 look = player.getLookAngle();
        Vec3 toStalker = new Vec3(this.getX() - player.getX(),
                this.getEyeY() - player.getEyeY(),
                this.getZ() - player.getZ()).normalize();
        if (look.dot(toStalker) > 0.80D) {
            this.seenTicks++;
        }

        int fadeTicks = Math.max(1, (int) (StalkerConfig.STALK_FADE_SECONDS.get() * 20.0D));

        if (dist < StalkerConfig.STALK_APPROACH_DISTANCE.get()) {
            this.startFadeOut(fadeTicks);            // te acercaste: se desvanece
        } else if (this.seenTicks > StalkerConfig.STALK_STARE_SECONDS.get() * 20) {
            this.startFadeOut(fadeTicks);            // lo miraste mucho rato
        } else if (this.life > StalkerConfig.STALK_MAX_LIFE_SECONDS.get() * 20 || dist > 70.0D) {
            this.startFadeOut(fadeTicks * 2);        // te aburriste / te alejaste: se despawnea solo
        }
    }

    private void clientTick() {
        this.prevAlpha = this.alpha;
        this.alpha = this.entityData.get(DATA_ALPHA);

        Entity target = this.level().getEntity(this.entityData.get(DATA_TARGET));
        if (target != null) {
            this.updateRotations(target);
        }

        // Musica de terror (solo en el modo acechar). Se inicia una sola vez, en el cliente.
        if (!this.soundStarted && this.getMode() == MODE_STALK) {
            this.soundStarted = true;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> StalkerClientHooks.startAmbient(this));
        }
    }

    /**
     * Cuerpo FIJO (no camina ni gira); solo la cabeza sigue al jugador, con giro suave y limitado.
     * Se ejecuta igual en cliente y servidor para que se vea fluido.
     */
    private void updateRotations(Entity target) {
        float bodyYaw = this.entityData.get(DATA_BODY_YAW);
        if (!this.headInit) {
            this.headYaw = bodyYaw;
            this.headInit = true;
        }
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float wanted = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float rel = Mth.clamp(Mth.wrapDegrees(wanted - bodyYaw), -75.0F, 75.0F);
        float goal = bodyYaw + rel;

        float oldHead = this.headYaw;
        this.headYaw = Mth.approachDegrees(this.headYaw, goal, 12.0F);

        this.setYRot(bodyYaw);
        this.yRotO = bodyYaw;
        this.yBodyRot = bodyYaw;
        this.yBodyRotO = bodyYaw;
        this.yHeadRotO = oldHead;
        this.yHeadRot = this.headYaw;

        double dy = target.getEyeY() - this.getEyeY();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) (-(Mth.atan2(dy, horiz) * (180.0D / Math.PI)));
        this.xRotO = this.getXRot();
        this.setXRot(Mth.clamp(pitch, -35.0F, 35.0F));
    }

    // =====================================================================
    //  Getters para el modelo / renderer / sonido
    // =====================================================================

    public int getMode() { return this.entityData.get(DATA_MODE); }
    public int getPoseState() { return this.entityData.get(DATA_POSE); }
    public int getLean() { return this.entityData.get(DATA_LEAN); }
    public boolean isArmLeft() { return this.entityData.get(DATA_ARM_LEFT); }
    public int getTargetId() { return this.entityData.get(DATA_TARGET); }
    public float getAlpha() { return this.alpha; }
    public float getPrevAlpha() { return this.prevAlpha; }

    // =====================================================================
    //  Comportamiento "fantasma": sin colision, sin dano, sin empujar
    // =====================================================================

    @Override
    protected BodyRotationControl createBodyControl() {
        // Evita que Minecraft gire el cuerpo solo: el cuerpo se queda fijo.
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                // intencionalmente vacio
            }
        };
    }

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

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 2.64F;
    }

    /** Los brazos extendidos sobresalen del hitbox; esto evita que desaparezcan al borde de la pantalla. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(2.5D);
    }
}
