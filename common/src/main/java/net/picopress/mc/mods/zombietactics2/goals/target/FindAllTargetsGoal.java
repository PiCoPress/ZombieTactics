package net.picopress.mc.mods.zombietactics2.goals.target;

import net.picopress.mc.mods.zombietactics2.attachments.FindTargetType;
import net.picopress.mc.mods.zombietactics2.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


// the new improved target finding goal
public class FindAllTargetsGoal extends TargetGoal {
    public static final List<Pair<LivingEntity, Path>> cache_path = new ArrayList<>();
    private final List<Pair<Class<? extends LivingEntity>, Integer>> list;
    private final List<LivingEntity> imposters = new ArrayList<>();
    private TargetingConditions targetingConditions;
    private int delay;
    private int idx;
    private Task task;

    /**
     * @param targets Pairs of class and priority
     */
    public FindAllTargetsGoal(List<Pair<Class<? extends LivingEntity>, Integer>> targets, Mob mob, boolean mustSee) {
        super(mob, mustSee);
        setFlags(EnumSet.of(Flag.TARGET));
        list = targets;
        targetingConditions = TargetingConditions.forCombat().range(Config.followRange).selector(null);
        if(Config.attackInvisible) targetingConditions = targetingConditions.ignoreLineOfSight();
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() == null;
    }

    @Override
    public void start() {
        idx = 0;
        delay = 0;
        task = Task.IDLE;
    }

    @Override
    public void tick() {
        if(task == Task.IDLE) {
            ++ delay;
            if(Config.findTargetType == FindTargetType.SIMPLE && delay > 4) task = Task.SEARCH;
            else if(delay > 6) task = Task.SEARCH;
        } else if(task == Task.SEARCH) {
            // simple target finding a target of the specific class per 1 tick
            if(Config.findTargetType == FindTargetType.SIMPLE) {
                LivingEntity target;
                var clazz = list.get(idx);
                if (clazz.getA() != Player.class && clazz.getA() != ServerPlayer.class) {
                    target = mob.level().getNearestEntity(clazz.getA(), targetingConditions, mob, mob.getX(), mob.getEyeY(), mob.getZ(), followBox());
                } else {
                    target = mob.level().getNearestPlayer(targetingConditions, mob, mob.getX(), mob.getEyeY(), mob.getZ());
                }
                if(mob.getTarget() == null || target != null && mob.getTarget() != null && mob.distanceToSqr(target) < mob.distanceToSqr(mob.getTarget())) {
                    mob.setTarget(target);
                }
                ++ idx;
                idx %= list.size();
                task = Task.IDLE;
            } else {
                // query targets
                var imposter2 = mob.level().getEntitiesOfClass(LivingEntity.class, followBox(), (t) -> {
                    for(var sus: list)
                        if(sus.getA().isAssignableFrom(t.getClass()) && targetingConditions.test(mob, t))
                            return true;

                    return false;
                });
                for(var imposter: imposter2) {
                    if(imposter != null) imposters.add(imposter);
                }
                task = Task.PRIORITIZE;
            }
            delay = 0;
        } else if(task == Task.PRIORITIZE) { // distribute loads with tasks, but it is similar to the brain system
            BlockPos me = mob.blockPosition();
            LivingEntity target = null;
            int minimumCost = Integer.MAX_VALUE;

            // calculate the cost for each of imposters
            for(var amogus: imposters) {
                BlockPos delta = me.subtract(amogus.blockPosition());
                int score = 0;
                int idx = 0;

                if(Config.findTargetType == FindTargetType.INTENSIVE) {
                    boolean found = false;
                    Path path = null;
                    for(var p: cache_path) {
                        if(p.getA() == amogus) {
                            path = p.getB();
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        // use cache to prevent overloading
                        path = mob.getNavigation().createPath(amogus, Config.accuracy);
                        cache_path.add(new Pair<>(amogus, path));
                    }
                    if(path != null) {
                        score += path.getNodeCount();
                        if(!path.canReach()) score *= 128;
                    }
                } else if(Config.findTargetType == FindTargetType.LINEAR) {
                    // using linear function
                    BlockPos.MutableBlockPos bp = mob.blockPosition().mutable();
                    double len = mob.distanceTo(amogus);

                    for(int i = 0; i <= len; ++ i) {
                        double cache = i / len;
                        if(!mob.level().getBlockState(bp.set(delta.getX() * cache, delta.getY() * cache, delta.getZ() * cache)).isAir())
                            score += Config.blockCost;
                        else ++ score;
                    }
                } else if(Config.findTargetType == FindTargetType.OVERLOAD) { // no one can endure this overload
                    Path path = mob.getNavigation().createPath(amogus, Config.accuracy);
                    if(path != null) {
                        score += path.getNodeCount();
                        if(!path.canReach()) score *= 128;
                    }
                }

                // apply priority
                for(var p: list) {
                    if(p.getA().isAssignableFrom(amogus.getClass())) break;
                    ++ idx;
                }
                // idx must match the target list unless priorities are invalid
                score *= list.get(idx).getB();

                // getting insane
                if(mob.hasLineOfSight(amogus)) score /= 2;
                if(delta.getY() >= - 2) score /= 2;

                // select minimum score
                if(score < minimumCost) {
                    minimumCost = score;
                    target = amogus;
                }
            }
            //System.out.println(mob.getId() + ": " + mob.canPickUpLoot());
            // set target
            if(target != null) mob.setTarget(target);
            imposters.clear();
            task = Task.IDLE;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || super.canContinueToUse();
    }

    private AABB followBox() {
        return mob.getBoundingBox().inflate(getFollowDistance());
    }

    // brain rot
    public enum Task {
        SEARCH,
        PRIORITIZE,
        IDLE,
    }
}
