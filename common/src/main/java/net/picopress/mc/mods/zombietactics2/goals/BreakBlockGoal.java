package net.picopress.mc.mods.zombietactics2.goals;

import net.picopress.mc.mods.zombietactics2.attachments.MiningData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;

import java.util.EnumSet;


// independent of Config
public abstract class BreakBlockGoal extends Goal {
    public final MiningData mine;
    protected final Level level;
    protected final Mob mob;
    private final double HardnessMultiplier;
    private final double break_speed;
    private final boolean dropBlock;
    protected double progress, hardness = Double.MAX_VALUE;

    public BreakBlockGoal(Mob mob, double HardnessMultiplier, double break_speed, boolean dropBlock) {
        this.mob = mob;
        mine = new MiningData();
        level = mob.level();
        setFlags(EnumSet.of(Flag.LOOK));
        this.HardnessMultiplier = HardnessMultiplier;
        this.break_speed = break_speed;
        this.dropBlock = dropBlock;
    }

    protected boolean checkBlock(BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        final Block b = state.getBlock();

        // exclude unbreakable blocks
        return !b.isPossibleToRespawnInThis(state) && b.defaultDestroyTime() >= 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return !mob.isNoAi() && mob.isAlive();
    }

    @Override
    public void start() {
        progress = 0;
        hardness = level.getBlockState(mine.bp).getBlock().defaultDestroyTime() * HardnessMultiplier;
        mine.doMining = true;
    }

    @Override
    public void stop() {
        // reset all progress and find path again
        level.destroyBlockProgress(mob.getId(), mine.bp, -1);
        mine.doMining = false;
        mine.bp = null;
        mob.getNavigation().recomputePath();
        progress = 0;
        hardness = Double.MAX_VALUE;
    }

    @Override
    public void tick() {
        if (!mine.doMining) return;

        // if the target block has been broken by others
        if(level.getBlockState(mine.bp).isAir()) {
            level.destroyBlockProgress(mob.getId(), mine.bp, -1);
            progress = 0;
            mine.doMining = false;
            return;
        }
        if (progress >= hardness) {
            level.destroyBlock(mine.bp, dropBlock, mob);
            level.destroyBlockProgress(mob.getId(), mine.bp, -1);
            mine.doMining = false;
        } else {
            level.destroyBlockProgress(mob.getId(), mine.bp, (int) ((progress / hardness) * 10));
            mob.stopInPlace();
            mob.getLookControl().setLookAt(mine.bp_vec3);
            progress += break_speed;
            mob.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return mine.doMining;
    }
}
