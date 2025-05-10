package net.picopress.mc.mods.zombietactics2.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;

public class Tactics {
    public static final BlockPos UNIT_FRONT = new BlockPos(0, 0, 1);

    public static Rotation getRelativeRotation(Mob mob) {
        Vec3i norm = mob.getNearestViewDirection().getNormal();
        int x = norm.getX(), z = norm.getZ();
        if(x == 0 && z == 1) return Rotation.NONE;
        else if(x == 0 && z == -1) return Rotation.CLOCKWISE_180;
        else if(x == -1 && z == 0) return Rotation.CLOCKWISE_90;
        else return Rotation.COUNTERCLOCKWISE_90; // x = 1, z = 0
    }

    // chunk xz = 12*12
    public static ChunkAccess[] getNearbyChunks(Level level, BlockPos pos) {
        ChunkAccess[] list = new ChunkAccess[9];
        int idx = 0;
        for(int i = -1; i <= 1; ++ i) {
            for(int j = -1; j <= 1; ++ j) {
                list[idx] = level.getChunk(pos.offset(12 * i, 0, 12 * j));
                ++ idx;
            }
        }
        return list;
    }
}
