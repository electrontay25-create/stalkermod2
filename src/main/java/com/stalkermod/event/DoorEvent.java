package com.stalkermod.event;

import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.ModEntities;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/**
 * Evento "tocar la puerta" (estilo Towi). Funciona de dia y de noche.
 *
 *  KNOCK   -> 3 toques, pausa, 3 toques (sonido original de Minecraft de zombi golpeando la puerta).
 *  SILENCE -> 5 a 10 segundos de silencio.
 *  WINDOW  -> (si hay ventana) la cara aparece en el vidrio junto a la puerta.
 *  ARMS    -> (si hay ventana) el brazo intenta abrir la puerta.
 *  POUND   -> golpes cada vez mas fuertes y rapidos, con astillas.
 *  AFTER   -> la puerta se rompe, ceguera III por 10 s, el Stalker se desvanece. No ataca.
 *
 * Si la puerta NO tiene ventana cerca, el evento igual ocurre pero solo con sonidos
 * (toques, golpes y la puerta rota): sin Stalker visible.
 */
public class DoorEvent {

    private enum Phase { KNOCK, SILENCE, WINDOW, ARMS, POUND, AFTER }

    private final ServerPlayer player;
    private final ServerLevel level;
    private final BlockPos door;       // mitad inferior de la puerta
    private final BlockPos window;     // bloque de vidrio (puede ser null)
    private final Direction outward;   // hacia afuera de la casa

    private Phase phase = Phase.KNOCK;
    private int ticks = 0;
    private int phaseLength = 0;
    private int nextSound = 0;
    private StalkerEntity stalker;

    private DoorEvent(ServerPlayer player, ServerLevel level, BlockPos door, BlockPos window, Direction outward) {
        this.player = player;
        this.level = level;
        this.door = door;
        this.window = window;
        this.outward = outward;
    }

    // =====================================================================
    //  Busqueda de puerta (+ ventana opcional)
    // =====================================================================

    /**
     * @param force true = viene del comando: no exige estar adentro ni respeta la hora.
     * @return el evento listo, o null si no hay una puerta de madera cerrada cerca de ti.
     */
    public static DoorEvent tryCreate(ServerPlayer player, boolean force) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pp = player.blockPosition();

        if (!force && StalkerConfig.DOOR_ONLY_AT_NIGHT.get() && !level.isNight()) {
            return null;
        }

        boolean roofed = !level.canSeeSky(pp.above());
        int r = StalkerConfig.DOOR_SEARCH_RADIUS.get();
        DoorEvent best = null;
        double bestScore = Double.MAX_VALUE;

        for (BlockPos p : BlockPos.betweenClosed(pp.offset(-r, -3, -r), pp.offset(r, 3, r))) {
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof DoorBlock) || !st.is(BlockTags.WOODEN_DOORS)) {
                continue;
            }
            if (st.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER || st.getValue(DoorBlock.OPEN)) {
                continue;
            }
            BlockPos doorPos = p.immutable();
            Vec3 dc = Vec3.atCenterOf(doorPos);

            // "Afuera" = el lado de la puerta que ve el cielo; si no se puede saber, el mas lejano al jugador.
            Direction axis = st.getValue(DoorBlock.FACING);
            Direction other = axis.getOpposite();
            boolean skyA = level.canSeeSky(doorPos.relative(axis));
            boolean skyB = level.canSeeSky(doorPos.relative(other));
            Direction out;
            if (skyA != skyB) {
                out = skyA ? axis : other;
            } else {
                double dA = dc.add(axis.getStepX(), 0.0D, axis.getStepZ()).distanceToSqr(player.position());
                double dB = dc.subtract(axis.getStepX(), 0.0D, axis.getStepZ()).distanceToSqr(player.position());
                out = dB > dA ? other : axis;
            }

            if (!force) {
                // El jugador debe estar del lado de ADENTRO, bajo techo o muy cerca de la puerta.
                double toOut = (player.getX() - dc.x) * out.getStepX() + (player.getZ() - dc.z) * out.getStepZ();
                double horiz = Math.sqrt((player.getX() - dc.x) * (player.getX() - dc.x)
                        + (player.getZ() - dc.z) * (player.getZ() - dc.z));
                if (toOut > 0.3D) {
                    continue; // estas afuera
                }
                if (!roofed && horiz > 6.0D) {
                    continue;
                }
            }

            BlockPos window = findWindow(level, doorPos, out);
            double d = dc.distanceToSqr(player.position());
            double score = d - (window != null ? 9.0D : 0.0D); // prefiere puertas con ventana
            if (score < bestScore) {
                bestScore = score;
                best = new DoorEvent(player, level, doorPos, window, out);
            }
        }
        return best;
    }

    /** Vidrio a 1-4 bloques de la puerta (en el mismo muro) y con espacio libre afuera; si no, null. */
    private static BlockPos findWindow(ServerLevel level, BlockPos doorPos, Direction out) {
        Direction perp = out.getClockWise();
        for (int k = 1; k <= 4; k++) {
            for (Direction dir : new Direction[]{perp, perp.getOpposite()}) {
                for (int h : new int[]{1, 2, 0, 3}) {
                    BlockPos w = doorPos.relative(dir, k).above(h);
                    if (!isGlass(level.getBlockState(w))) {
                        continue;
                    }
                    if (!StalkSpawner.isPassable(level, w.relative(out))) {
                        continue;
                    }
                    return w;
                }
            }
        }
        return null;
    }

    private static boolean isGlass(BlockState st) {
        return st.is(Tags.Blocks.GLASS) || st.is(Tags.Blocks.GLASS_PANES);
    }

    // =====================================================================
    //  Ejecucion (se llama una vez por tick)
    // =====================================================================

    /** @return true cuando el evento termino (y hay que olvidarlo). */
    public boolean tick() {
        if (!player.isAlive() || player.level() != level || player.isSpectator()) {
            abort();
            return true;
        }
        ticks++;

        switch (phase) {
            case KNOCK -> {
                if (ticks == 10 || ticks == 18 || ticks == 26 || ticks == 70 || ticks == 78 || ticks == 86) {
                    knock(2.6F, 1.25F + level.random.nextFloat() * 0.12F);
                }
                if (ticks >= 110) {
                    next(Phase.SILENCE, 100 + level.random.nextInt(101)); // 5 a 10 s
                }
            }
            case SILENCE -> {
                if (ticks >= phaseLength) {
                    if (window != null) {
                        spawnStalker();
                        next(Phase.WINDOW, 100 + level.random.nextInt(60));
                    } else {
                        // Sin ventana: pasa directo a los golpes (solo sonido).
                        next(Phase.POUND, 100);
                        nextSound = 5;
                    }
                }
            }
            case WINDOW -> {
                if (stalkerGone()) {
                    return true;
                }
                if (ticks >= phaseLength) {
                    stalker.setSprite(StalkerEntity.SPRITE_B);
                    stalker.setPoseState(StalkerEntity.POSE_REACH);
                    play(SoundEvents.WOODEN_DOOR_OPEN, 0.7F, 0.6F); // la manija "se mueve"
                    next(Phase.ARMS, 80);
                    nextSound = 12;
                }
            }
            case ARMS -> {
                if (stalkerGone()) {
                    return true;
                }
                if (ticks == nextSound) {
                    knock(1.0F, 1.7F);
                    nextSound += 10 + level.random.nextInt(8);
                }
                if (ticks >= phaseLength) {
                    stalker.setPoseState(StalkerEntity.POSE_POUND);
                    next(Phase.POUND, 100);
                    nextSound = 5;
                }
            }
            case POUND -> {
                if (window != null && stalkerGone()) {
                    return true;
                }
                if (ticks == nextSound) {
                    knock(4.0F, 0.72F + level.random.nextFloat() * 0.12F);
                    play(SoundEvents.WOODEN_DOOR_CLOSE, 2.5F, 0.5F); // golpe grave
                    splinters(12);
                    nextSound += Math.max(5, 14 - ticks / 10); // cada vez mas rapido
                }
                if (ticks >= phaseLength) {
                    breakDoor();
                    next(Phase.AFTER, 70);
                }
            }
            case AFTER -> {
                if (ticks == 45 && stalker != null && !stalker.isRemoved()) {
                    stalker.startHide(30); // se desvanece, no ataca
                }
                if (ticks >= phaseLength) {
                    abort();
                    return true;
                }
            }
        }
        return false;
    }

    /** Limpia el Stalker si el evento se corta por la mitad. */
    public void abort() {
        if (stalker != null && !stalker.isRemoved()) {
            stalker.discard();
        }
    }

    private boolean stalkerGone() {
        return stalker == null || stalker.isRemoved();
    }

    private void next(Phase p, int length) {
        this.phase = p;
        this.ticks = 0;
        this.phaseLength = length;
    }

    // =====================================================================
    //  Acciones
    // =====================================================================

    private void spawnStalker() {
        StalkerEntity st = ModEntities.STALKER.get().create(level);
        if (st == null) {
            return;
        }
        // Afuera, pegado al vidrio. En 2D la posicion de la entidad es el centro de la cabeza,
        // asi que va justo al centro del bloque de la ventana.
        double x = window.getX() + 0.5D + outward.getStepX() * 0.75D;
        double z = window.getZ() + 0.5D + outward.getStepZ() * 0.75D;
        double y = window.getY() + 0.5D;
        float yaw = outward.getOpposite().toYRot(); // mirando hacia adentro de la casa

        float leftX = Mth.cos(yaw * ((float) Math.PI / 180.0F));
        float leftZ = Mth.sin(yaw * ((float) Math.PI / 180.0F));
        Vec3 dc = Vec3.atCenterOf(door);
        int lean = ((dc.x - x) * leftX + (dc.z - z) * leftZ) > 0.0D ? 1 : -1; // lado de la puerta

        st.moveTo(x, y, z, yaw, 0.0F);
        st.initDoor(player, yaw, lean);
        level.addFreshEntity(st);
        this.stalker = st;
    }

    private void breakDoor() {
        BlockState st = level.getBlockState(door);
        splinters(40);                                    // astillas ANTES de que el bloque desaparezca
        if (st.getBlock() instanceof DoorBlock) {
            level.destroyBlock(door, false);              // mitad de abajo
            BlockPos up = door.above();
            if (level.getBlockState(up).getBlock() instanceof DoorBlock) {
                level.destroyBlock(up, false);            // mitad de arriba
            }
        }
        play(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 4.0F, 1.0F);

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                StalkerConfig.BLINDNESS_SECONDS.get() * 20,
                StalkerConfig.BLINDNESS_AMPLIFIER.get()));

        if (stalker != null && !stalker.isRemoved()) {
            stalker.setPoseState(StalkerEntity.POSE_BROKE);
        }
    }

    /** Toque en la puerta: el sonido original de Minecraft de un zombi golpeando la puerta de madera. */
    private void knock(float volume, float pitch) {
        play(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, volume, pitch);
    }

    private void play(SoundEvent sound, float volume, float pitch) {
        Vec3 c = Vec3.atCenterOf(door);
        level.playSound(null, c.x, c.y, c.z, sound, SoundSource.BLOCKS, volume, pitch);
    }

    private void splinters(int count) {
        Vec3 c = Vec3.atCenterOf(door);
        BlockState st = level.getBlockState(door);
        if (st.isAir()) {
            return;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st),
                c.x, c.y + 0.4D, c.z, count, 0.3D, 0.5D, 0.3D, 0.05D);
    }
}
