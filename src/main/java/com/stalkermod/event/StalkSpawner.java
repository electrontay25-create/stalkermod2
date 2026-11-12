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
 * Evento "acechar" (cuerpo asomado). Solo aparece si TODO esto se cumple:
 *  1. Hay una cobertura (tronco, piedra, madera, hojas...) de la altura suficiente:
 *       forma A (figura completa) = 4+ bloques de alto;  forma B (cabeza + brazo) = 2+ bloques.
 *  2. El borde recto del PNG queda TAPADO por la cobertura. Se comprueba con rayos desde tus ojos
 *     a varios puntos de ese borde: si alguno se ve "en el aire", NO aparece.
 *  3. Hay espacio libre al costado para que el sprite no quede metido en otro bloque.
 *  4. Su cabeza se ve (rayo libre) y, salvo que se ignore, no estas mirando hacia alla.
 */
public final class StalkSpawner {

    private static final int ATTEMPTS = 500;
    /** Que tan adentro de la cobertura queda el borde recto del PNG (0.5 = justo en el borde). */
    private static final double ANCHOR_LATERAL = 0.36D;

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

            BlockPos base = findCoverBase(level, cx, cz, player.getBlockY());
            if (base == null) {
                continue;
            }
            int coverHeight = coverHeight(level, base);
            if (coverHeight < 2) {
                continue;
            }

            // Direccion jugador -> cobertura (horizontal, normalizada).
            double ax = base.getX() + 0.5D - player.getX();
            double az = base.getZ() + 0.5D - player.getZ();
            double len = Math.sqrt(ax * ax + az * az);
            if (len < 4.0D) {
                continue;
            }
            ax /= len;
            az /= len;

            int firstSign = rnd.nextBoolean() ? 1 : -1;
            for (int k = 0; k < 2; k++) {
                int sign = (k == 0) ? firstSign : -firstSign;
                double px = -az * sign;   // perpendicular: hacia ese lado sobresale
                double pz = ax * sign;

                // Posicion del borde recto del PNG: dentro de la cobertura (lateral) y un poco detras.
                double sx = base.getX() + 0.5D + ax * 0.62D + px * ANCHOR_LATERAL;
                double sz = base.getZ() + 0.5D + az * 0.62D + pz * ANCHOR_LATERAL;

                int gy = findGroundY(level, Mth.floor(sx), Mth.floor(sz), base.getY());
                if (gy == Integer.MIN_VALUE || Math.abs(gy - player.getBlockY()) > 7) {
                    continue;
                }

                // Forma A solo si la cobertura es alta; si no, forma B.
                int sprite;
                if (coverHeight >= 4) {
                    sprite = rnd.nextBoolean() ? StalkerEntity.SPRITE_A : StalkerEntity.SPRITE_B;
                } else {
                    sprite = StalkerEntity.SPRITE_B;
                }
                SpriteInfo info = SpriteInfo.get(sprite);

                if (!lateralFree(level, sx, sz, px, pz, gy, info)) {
                    continue;
                }
                if (!edgeHidden(level, eye, sx, sz, gy, sprite)) {
                    continue;   // el borde recto se veria "recortado" en el aire
                }

                double lat = info.headLateral();
                Vec3 headPoint = new Vec3(sx + px * lat, gy + info.headHeight(), sz + pz * lat);
                if (Vis.blocked(level, eye, headPoint)) {
                    continue;   // la cabeza no se veria
                }
                if (!ignoreView) {
                    Vec3 toHead = headPoint.subtract(eye).normalize();
                    if (toHead.dot(look) > 0.30D) {
                        continue; // el jugador lo veria aparecer
                    }
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

    /** Todos los puntos del borde recto del sprite deben quedar tapados desde los ojos del jugador. */
    private static boolean edgeHidden(ServerLevel level, Vec3 eye, double sx, double sz, int gy, int sprite) {
        double[] heights = (sprite == StalkerEntity.SPRITE_A)
                ? new double[]{0.4D, 1.2D, 2.0D, 2.7D, 3.05D}
                : new double[]{0.3D, 0.8D, 1.05D};
        for (double h : heights) {
            if (!Vis.blocked(level, eye, new Vec3(sx, gy + h, sz))) {
                return false;
            }
        }
        return true;
    }

    /** El sprite se extiende hacia un lado: no debe quedar metido en otros bloques. */
    private static boolean lateralFree(ServerLevel level, double sx, double sz, double px, double pz,
                                       int gy, SpriteInfo info) {
        double reach = info.width * (1.0F - info.anchor);
        for (double t = 0.5D; t <= reach; t += 0.5D) {
            BlockPos c = BlockPos.containing(sx + px * t, gy, sz + pz * t);
            if (!level.hasChunkAt(c) || !isPassable(level, c) || !isPassable(level, c.above())) {
                return false;
            }
        }
        return true;
    }

    /** Busca (cerca de la altura del jugador) una cobertura con un lado libre y baja hasta su base. */
    private static BlockPos findCoverBase(ServerLevel level, int x, int z, int py) {
        for (int i = 0; i <= 16; i++) {
            int dy = ((i + 1) / 2) * ((i % 2 == 1) ? -1 : 1); // 0, -1, +1, -2, +2 ...
            BlockPos c = new BlockPos(x, py + dy, z);
            if (!level.hasChunkAt(c)) {
                return null; // no cargamos chunks nuevos
            }
            if (isCover(level, c) && isCover(level, c.above()) && isExposed(level, c)) {
                int guard = 0;
                while (guard++ < 8 && isCover(level, c.below())) {
                    c = c.below();
                }
                return c;
            }
        }
        return null;
    }

    /** Cuantos bloques de cobertura seguidos hay hacia arriba (maximo 6). */
    private static int coverHeight(ServerLevel level, BlockPos base) {
        int h = 0;
        while (h < 6 && isCover(level, base.above(h))) {
            h++;
        }
        return h;
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

    /** Suelo para el Stalker: prueba la altura de la base, uno abajo y uno arriba. */
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
}
