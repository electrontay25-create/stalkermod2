package com.stalkermod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Utilidades de visibilidad "a mano" (rayos que avanzan de a 0.2 bloques).
 * Bloquean los bloques completos y opacos Y las hojas (visualmente tapan). El vidrio y las plantas no.
 */
public final class Vis {

    private static final double STEP = 0.2D;

    private Vis() {}

    public static boolean occludes(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.is(BlockTags.LEAVES)) {
            return true;
        }
        return st.canOcclude() && st.isCollisionShapeFullBlock(level, pos);
    }

    /** true si entre 'from' y 'to' (incluido el punto final) hay algo que tape la vista. */
    public static boolean blocked(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int steps = Math.max(1, (int) (d.length() / STEP));
        for (int i = 1; i <= steps; i++) {
            Vec3 p = from.add(d.scale(i / (double) steps));
            BlockPos bp = BlockPos.containing(p.x, p.y, p.z);
            if (!level.hasChunkAt(bp) || occludes(level, bp)) {
                return true;
            }
        }
        return false;
    }

    public static boolean clear(ServerLevel level, Vec3 from, Vec3 to) {
        return !blocked(level, from, to);
    }

    /** Distancia hasta el primer bloque que tapa en esa direccion (o 'max' si no hay ninguno). */
    public static double distance(ServerLevel level, Vec3 from, Vec3 dir, double max) {
        int steps = (int) (max / STEP);
        for (int i = 1; i <= steps; i++) {
            Vec3 p = from.add(dir.scale(i * STEP));
            BlockPos bp = BlockPos.containing(p.x, p.y, p.z);
            if (!level.hasChunkAt(bp) || occludes(level, bp)) {
                return i * STEP;
            }
        }
        return max;
    }
}
