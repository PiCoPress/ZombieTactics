package net.picopress.mc.mods.zombietactics2.attachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;


/*
    This class contains a mining block's position
    and a condition of mine for each of zombie
 */
public class MiningData {
    public boolean doMining;
    @Nullable public BlockPos bp;
    @Nullable public Vec3 bp_vec3; // bp_vec3 = bp.getCenter()

    public MiningData() {
        doMining = false;
        // `bp` is Nullable but, if `doMining` is true, `bp` must not be null
        bp_vec3 = null;
        bp = null;
    }
}
