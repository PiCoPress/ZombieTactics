package n643064.zombie_tactics.fabric.mining;

import static n643064.zombie_tactics.common.mining.MiningRoutines.*;

import n643064.zombie_tactics.fabric.Main;
import n643064.zombie_tactics.fabric.attachments.MiningData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Block;

import org.jetbrains.annotations.NotNull;

public class ZombieMineGoal<T extends HostileEntity> extends Goal {
    private final T zombie;
    private final World world;
    private double progress, hardness = Double.MAX_VALUE;
    private final MiningData mine;

    // These are constant unless a target is changed
    private double X, Y, Z;

    public ZombieMineGoal(T zombie) {
        this.zombie = zombie;
        world = zombie.getWorld();
        mine = new MiningData();
    }

    @Override
    public void start() {
        hardness = world.getBlockState(mine.bp).getBlock().getHardness() * Main.config.hardnessMultiplier;
        mine.doMining = true;
        progress = 0;
        zombie.setAttached(Main.ZOMBIE_MINING, mine);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    // get deltaY between me and target
    // then return a proper set of positions
    public byte[][] getCandidate(@NotNull LivingEntity liv) {
        double deltaY = liv.getY() - zombie.getY();
        if(deltaY > -2 && deltaY < 2)
            return routineFlat;
        else if(deltaY <= -2)
            return routineDown;
        else // deltaY >= 2
            return routineUp;
    }

    // is valid to mine?
    protected boolean checkBlock(BlockPos pos) {
        final BlockState state = world.getBlockState(pos);
        final Block b = state.getBlock();
        float destroying = b.getHardness();

        // exclude unbreakable blocks
        if(!b.canMobSpawnInside(state) &&
                destroying >= 0 && destroying <= Main.config.maxHardness) {
            X = pos.getX();
            Y = pos.getY();
            Z = pos.getZ();
            mine.bp = pos;
            return true;
        }
        return false;
    }

    @Override
    public void stop() {
        // reset all progress and find path again
        world.setBlockBreakingInfo(zombie.getId(), mine.bp, -1);
        zombie.getNavigation().recalculatePath();
        hardness = Double.MAX_VALUE;
        mine.doMining = false;
        progress = 0;
        mine.bp = null;
    }

    @Override
    public void tick() {
        if (!mine.doMining) return;
        double dist = zombie.squaredDistanceTo(X, Y, Z);
        if (dist <= Main.config.minDist ||
            dist > Main.config.maxDist) {
            mine.doMining = false;
            return;
        }

        // if the target block has been broken by others
        if(world.getBlockState(mine.bp).isAir()) {
            world.setBlockBreakingInfo(zombie.getId(), mine.bp, -1);
            progress = 0;
            mine.doMining = false;
            return;
        }
        if (progress >= hardness) {
            world.breakBlock(mine.bp, Main.config.dropBlocks, zombie);
            world.setBlockBreakingInfo(zombie.getId(), mine.bp, -1);
            mine.doMining = false;
        } else {
            world.setBlockBreakingInfo(zombie.getId(), mine.bp, (int) ((progress / hardness) * 10));
            zombie.stopMovement();
            zombie.getLookControl().lookAt(X, Y, Z);
            progress += Main.config.increment;
            zombie.swingHand(Hand.MAIN_HAND);
        }
    }

    @Override
    public boolean shouldContinue() {
        return mine.doMining && zombie.squaredDistanceTo(mine.bp.toCenterPos()) <= Main.config.maxDist;
    }

    @Override
    public boolean canStart() {
        // a zombie should be stuck
        // check availability of the mining
        double x, y, z;
        x = zombie.getX();
        y = zombie.getY();
        z = zombie.getZ();
        if(X != x || Y != y || Z != z) {
            X = x; Y = y; Z = z;
            return false;
        }

        // found path but a zombie stuck
        LivingEntity liv = zombie.getTarget();
        EntityNavigation nav = zombie.getNavigation();

        if(zombie.isAlive() && !zombie.isAiDisabled() &&
                nav.isIdle() && liv != null) {
            if(zombie.isInAttackRange(liv)) return false;
            // go once more
            // Issue: moveTo sometimes return false while a zombie can go to the target.
            // It can solve by using the method `hasLineOfSight` but this causes a problem
            //   about fences that have 1.5 meters tall.
            // TODO: fix this
            boolean eval = nav.startMovingTo(liv, zombie.getMovementSpeed());
            if(eval) return false;

            byte[][] set = getCandidate(liv);
            for(byte[] pos: set) {
                // checkBlock method is able to change 'zombie' variable
                // So 'temp' cannot be determined as valid object
                BlockPos temp = zombie.getBlockPos().add(pos[0], pos[1], pos[2]);
                if(checkBlock(temp)) return true;
            }
        }
        // zombie cannot escape
        return false;
    }
}
