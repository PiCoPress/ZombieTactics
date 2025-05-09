package net.picopress.mc.mods.zombietactics2.mining;

import static net.picopress.mc.mods.zombietactics2.mining.MiningRoutines.*;
import static net.picopress.mc.mods.zombietactics2.util.Tactics.getRelativeRotation;
import net.picopress.mc.mods.zombietactics2.Config;
import net.picopress.mc.mods.zombietactics2.goals.BreakBlockGoal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;

import org.jetbrains.annotations.NotNull;


public class MonsterBreakBlockGoal<T extends Monster> extends BreakBlockGoal {
    private final T zombie;

    public MonsterBreakBlockGoal(T zombie) {
        super(zombie, Config.hardnessMultiplier, Config.break_speed, Config.dropBlocks);
        this.zombie = zombie;
    }

    // get deltaY between me and target
    // then return a proper set of positions
    protected BlockPos[] getCandidate(@NotNull LivingEntity liv) {
        double deltaY = liv.getY() - zombie.getY();
        if(deltaY > -2 && deltaY < 2)
            return routineFlat;
        else if(deltaY <= -2)
            return routineDown;
        else // deltaY >= 2
            return routineUp;
    }

    // is valid to mine?
    @Override
    protected boolean checkBlock(BlockPos pos) {
        float destroying = level.getBlockState(pos).getBlock().defaultDestroyTime();
        boolean ret = super.checkBlock(pos) && destroying <= Config.maxHardness;
        if(ret) {
            mine.bp = pos;
            mine.bp_vec3 = pos.getCenter();
            mine.doMining = true;
        }
        return ret;
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && zombie.distanceToSqr(mine.bp_vec3) <= Config.maxDist * Config.maxDist;
    }

    @Override
    public void tick() {
        double dist = zombie.distanceToSqr(mine.bp_vec3);
        if (dist <= Config.minDist ||
                dist > Config.maxDist) {
            mine.doMining = false;
            return;
        }
        super.tick();
    }

    @Override
    public boolean canUse() {
        if(!super.canUse()) return false;

        // a zombie should be stuck
        // check availability of the mining
        if(zombie.getDeltaMovement().length() > 0.8) {
            // relaxed for flying zombies
            if(!Config.canFly) return false;
        }

        // found path but a zombie stuck
        LivingEntity liv = zombie.getTarget();
        PathNavigation nav = zombie.getNavigation();
        if(nav.isDone() && liv != null && nav.getPath() != null && !nav.getPath().canReach()) {
            if(zombie.isWithinMeleeAttackRange(liv) && zombie.hasLineOfSight(liv)) return false;

            // why is the path null even though it can reach a target?
            // the sucks

            // go once more
            // Issue: moveTo sometimes return false while a zombie can go to the target.
            // It can solve by using the method `hasLineOfSight` but this causes a problem
            // about fences that have 1.5 meters tall.
            if(nav.moveTo(liv, zombie.getSpeed())) return false;
            final BlockPos[] set = getCandidate(liv);
            int airStack = 0;

            for(BlockPos pos: set) {
                // checkBlock method is able to change 'zombie' variable
                // So 'temp' cannot be determined as valid object
                // select relative block position
                BlockPos temp = zombie.blockPosition().offset(pos.rotate(getRelativeRotation(zombie)));

                // prevent that they are not stuck but zombie digs under their foot
                // It may fix the described issue in specific cases
                if(level.getBlockState(temp).isAir()) ++ airStack;
                if(airStack == set.length - 1) break;
                if(checkBlock(temp))
                    return true;
            }

            // zombie is in the wall
        } else if(zombie.isInWall()) {
            for(BlockPos p: routineWall) {
                BlockPos temp = zombie.blockPosition().offset(p);
                if(checkBlock(temp)) return true;
            }
        }
        // zombie cannot escape
        return false;
    }
}
