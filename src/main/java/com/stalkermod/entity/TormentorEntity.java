package com.stalkermod.entity;

import com.stalkermod.event.TormentorSpawner;
import com.stalkermod.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * El Tormentor: golpea la ventana cada vez mas fuerte hasta romperla. Ignora el material de la ventana
 * (vidrio, paneles, vallas, barrotes de hierro): la destruye igual.
 *
 * La animacion son 108 cuadros (12 por segundo = 9 segundos) sacados del video; el renderer elige el cuadro
 * segun el tiempo de vida. El audio del video suena al aparecer y esta sincronizado con los golpes.
 * A los 9.0 s (tick 180) se rompe la ventana, que es justo cuando el vidrio se rompe en el video.
 */
public class TormentorEntity extends Mob {

    public static final int FPS = 12;
    public static final int FRAME_COUNT = 108;
    /** Tick en que se rompe la ventana (9.0 s). */
    public static final int BREAK_TICK = 180;

    private static final EntityDataAccessor<Float> DATA_REVEAL =
            SynchedEntityData.defineId(TormentorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BODY_YAW =
            SynchedEntityData.defineId(TormentorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TARGET =
            SynchedEntityData.defineId(TormentorEntity.class, EntityDataSerializers.INT);

    // ---- servidor ----
    private UUID targetUuid;
    private int life;
    private boolean hiding;
    private float hideStep = 0.125F;
    private float serverReveal = 1.0F;
    private final List<BlockPos> windows = new ArrayList<>();

    // ---- cliente ----
    private float reveal = 1.0F;
    private float prevReveal = 1.0F;

    public TormentorEntity(EntityType<? extends Mob> type, Level level) {
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
        this.entityData.define(DATA_BODY_YAW, 0.0F);
        this.entityData.define(DATA_TARGET, -1);
    }

    /**
     * @param bodyYaw yaw mirando hacia ADENTRO de la casa (el cartel queda paralelo al muro)
     * @param windows bloques de ventana que se romperan al final
     */
    public void init(ServerPlayer player, float bodyYaw, List<BlockPos> windows) {
        this.targetUuid = player.getUUID();
        this.entityData.set(DATA_TARGET, player.getId());
        this.entityData.set(DATA_BODY_YAW, bodyYaw);
        this.windows.clear();
        this.windows.addAll(windows);
    }

    // =====================================================================
    //  Tick
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.prevReveal = this.reveal;
            this.reveal = this.entityData.get(DATA_REVEAL);
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

        if (this.life == 1) {
            // El audio del video (golpes, rugidos, vidrio) empieza junto con la animacion.
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.TORMENTOR_ATTACK.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        }
        if (this.life == BREAK_TICK) {
            this.breakWindows();
        }
        if (this.life == BREAK_TICK + 2) {
            this.hiding = true;
        }
        if (this.life > 20 * 30) {
            this.discard();   // seguro
            return;
        }

        if (this.hiding) {
            this.serverReveal -= this.hideStep;
        }
        this.serverReveal = Mth.clamp(this.serverReveal, 0.0F, 1.0F);
        this.entityData.set(DATA_REVEAL, this.serverReveal);
        if (this.hiding && this.serverReveal <= 0.0F) {
            this.discard();
        }
    }

    /** Destruye la ventana (y las ventanas pegadas), sea de vidrio, valla o barrotes. */
    private void breakWindows() {
        ServerLevel level = (ServerLevel) this.level();
        for (BlockPos pos : this.windows) {
            BlockState st = level.getBlockState(pos);
            if (TormentorSpawner.isWindowBlock(st)) {
                level.destroyBlock(pos, false);   // con sonido y particulas de rotura
            }
        }
    }

    // =====================================================================
    //  Getters
    // =====================================================================

    public float getBodyYawData() { return this.entityData.get(DATA_BODY_YAW); }
    public int getTargetId() { return this.entityData.get(DATA_TARGET); }
    public float getReveal() { return this.reveal; }
    public float getPrevReveal() { return this.prevReveal; }

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
