package com.stalkermod.event;

import com.stalkermod.entity.SpriteInfo;
import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.ModEntities;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Busca donde puede asomarse el Stalker.
 *
 * Ahora funciona con CUALQUIER cobertura: troncos, hojas, piedra, madera de casas, paredes de cueva...
 * Para cada intento:
 *  1. Elige un punto al azar a 12-26 bloques (configurable) alrededor del jugador.
 *  2. Busca ahi una "cobertura": 2 bloques solidos apilados, con al menos un lado libre.
 *  3. Coloca al Stalker DETRAS de la cobertura (visto desde el jugador), con su borde pegado al borde
 *     de la cobertura, para que se asome por el costado.
 *  4. Comprueba que haya suelo, que la cabeza sea visible desde los ojos del jugador y (si se pide)
 *     que el jugador NO este mirando hacia ahi.
 */
public final class StalkSpawner {

    private static final int ATTEMPTS = 500;

    private StalkSpawner() {}

    public static boolean trySpawn(ServerPlayer player, boolean ignoreView) {
        ServerLevel level = (ServerLevel) player.level();
        RandomSource rnd = level.random;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        int min = StalkerConfig.STALK_MIN_DISTANCE.get();
        int max = Math.max(min + 1, StalkerConfig.STALK_MAX_DISTANCE.get());

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double dist = min + rnd.nextDouble() * (max - min);
            double ang = rnd.nextDouble() * Math.PI * 2.0D;
            int cx = Mth.floor(player.getX() + Math.cos(ang) * dist);
            int cz = Mth.floor(player.getZ() + Math.sin(ang) * dist);

            BlockPos cover = findCoverBase(level, cx, cz, player.getBlockY());
            if (cover == null) {
                continue;
            }

            // Direccion jugador -> cobertura (horizontal, normalizada).
            double ax = cover.getX() + 0.5D - player.getX();
            double az = cover.getZ() + 0.5D - player.getZ();
            double len = Math.sqrt(ax * ax + az * az);
            if (len < 4.0D) {
                continue;
            }
            ax /= len;
            az /= len;

            int firstSign = rnd.nextBoolean() ? 1 : -1;
            for (int k = 0; k < 2; k++) {
                int sign = (k == 0) ? firstSign : -firstSign;
                // Perpendicular: hacia ese lado sobresale el Stalker.
                double px = -az * sign;
                double pz = ax * sign;

                // Borde de la cobertura (0.5 al costado) y un poco por detras (0.62).
                double sx = cover.getX() + 0.5D + ax * 0.62D + px * 0.5D;
                double sz = cover.getZ() + 0.5D + az * 0.62D + pz * 0.5D;

                int gy = findGroundY(level, Mth.floor(sx), Mth.floor(sz), cover.getY());
                if (gy == Integer.MIN_VALUE) {
                    continue;
                }

                int sprite = rnd.nextBoolean() ? StalkerEntity.SPRITE_A : StalkerEntity.SPRITE_B;
                SpriteInfo info = SpriteInfo.get(sprite);
                double lat = info.headLateral();
                Vec3 headPoint = new Vec3(sx + px * lat, gy + info.headHeight(), sz + pz * lat);

                if (!ignoreView) {
                    Vec3 toHead = headPoint.subtract(eye).normalize();
                    if (toHead.dot(look) > 0.30D) {
                        continue; // el jugador lo veria aparecer
                    }
                }
                if (!hasClearView(level, eye, headPoint)) {
                    continue;
                }

                // ---- Valido: crear la entidad ----
                StalkerEntity stalker = ModEntities.STALKER.get().create(level);
                if (stalker == null) {
                    return false;
                }
                float yaw = (float) (Mth.atan2(player.getZ() - sz, player.getX() - sx) * (180.0D / Math.PI)) - 90.0F;
                int noticeTicks = (int) ((StalkerConfig.STALK_NOTICE_SECONDS.get() - 1 + rnd.nextFloat()) * 20.0F);

                stalker.moveTo(sx, gy, sz, yaw, 0.0F);
                stalker.initStalk(player, yaw, sign, sprite, noticeTicks);
                level.addFreshEntity(stalker);
                return true;
            }
        }
        return false;
    }

    /** Busca (cerca de la altura del jugador) 2 bloques de cobertura apilados con un lado libre. */
    private static BlockPos findCoverBase(ServerLevel level, int x, int z, int py) {
        for (int i = 0; i <= 16; i++) {
            int dy = ((i + 1) / 2) * ((i % 2 == 1) ? -1 : 1); // 0, -1, +1, -2, +2 ...
            BlockPos c = new BlockPos(x, py + dy, z);
            if (!level.hasChunkAt(c)) {
                return null; // no cargamos chunks nuevos
            }
            if (isCover(level, c) && isCover(level, c.above()) && isExposed(level, c)) {
                return c;
            }
        }
        return null;
    }

    private static boolean isExposed(ServerLevel level, BlockPos c) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = c.relative(d);
            if (isPassable(level, n) && isPassable(level, n.above())) {
                return true;
            }
        }
        return false;
    }

    /** Suelo para el Stalker: prueba la altura de la cobertura, uno abajo y uno arriba. */
    private static int findGroundY(ServerLevel level, int x, int z, int baseY) {
        int[] tries = {baseY, baseY - 1, baseY + 1};
        for (int y : tries) {
            BlockPos feet = new BlockPos(x, y, z);
            if (level.hasChunkAt(feet) && isStandable(level, feet)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isStandable(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP)
                || ground.is(BlockTags.LEAVES)
                || !ground.getFluidState().isEmpty()) {
            return false;
        }
        return isPassable(level, feet) && isPassable(level, feet.above());
    }

    static boolean isPassable(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        return st.getCollisionShape(level, pos).isEmpty() && st.getFluidState().isEmpty();
    }

    /** Cobertura: bloque completo y opaco (troncos, piedra, madera, paredes...) o follaje (hojas). */
    private static boolean isCover(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (!st.isCollisionShapeFullBlock(level, pos)) {
            return false;
        }
        return st.canOcclude() || st.is(BlockTags.LEAVES);
    }

    /**
     * Linea de vision "a mano": camina de a 0.35 bloques entre los dos puntos y falla si cruza un
     * bloque opaco. (Las hojas y el vidrio se dejan pasar para que el arbol no bloquee todo.)
     */
    public static boolean hasClearView(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        int steps = (int) (len / 0.35D);
        for (int i = 1; i < steps; i++) {
            Vec3 p = from.add(d.scale(i / (double) steps));
            BlockPos bp = BlockPos.containing(p.x, p.y, p.z);
            if (!level.hasChunkAt(bp)) {
                return false;
            }
            BlockState st = level.getBlockState(bp);
            if (st.canOcclude() && st.isCollisionShapeFullBlock(level, bp)) {
                return false;
            }
        }
        return true;
    }
}
