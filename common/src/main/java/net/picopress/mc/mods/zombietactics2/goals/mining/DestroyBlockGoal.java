package net.picopress.mc.mods.zombietactics2.goals.mining;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.picopress.mc.mods.zombietactics2.util.Tactics;
import net.picopress.mc.mods.zombietactics2.goals.BreakBlockGoal;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


// destroy specific block
public class DestroyBlockGoal extends BreakBlockGoal {
    private final List<BlockPos> positions;
    private ChunkAccess my_chunk;
    private final Block block;
    private int delay = 0;

    public DestroyBlockGoal(Mob mob, Block block) {
        super(mob, 5, 0.2, false);
        positions = new ArrayList<>();
        this.block = block;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if(!super.canUse()) return false;
        ++ delay;
        if(delay < 20) return false;
        delay = 0;
        double dist = Double.MAX_VALUE;
        // NO!!! WHY IS THIS NOT WORKING ON NEOFORGE?
        // check if zombie's chunk does not change
        //if(level.getChunk(mob.blockPosition()) != my_chunk) {
            var chunks = Tactics.getNearbyChunks(level, mob.blockPosition());
            //my_chunk = level.getChunk(mob.blockPosition());
            positions.clear();
            // get all chunks
            for(LevelChunk ca: chunks) {
                ca.findBlocks((bs) -> bs.is(block), (bp, bs) -> positions.add(bp));
            }
        //}
        // find the closest chest
        if(positions.isEmpty()) return false;
        for(BlockPos bp: positions) {
            double d = mob.distanceToSqr(bp.getCenter());
            if(d < dist) {
                dist = d;
                mine.bp = bp;
                mine.bp_vec3 = bp.getCenter();
            }
        }

        // this cannot be null but the ide warns
        if(mine.bp_vec3 == null) return false;
        // move and check distance
        mob.getNavigation().moveTo(mine.bp_vec3.x, mine.bp_vec3.y, mine.bp_vec3.z, 1.0);
        return mob.distanceToSqr(mine.bp_vec3) < 4;
    }
}
