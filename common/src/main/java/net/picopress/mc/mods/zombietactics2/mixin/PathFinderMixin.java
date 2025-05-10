package net.picopress.mc.mods.zombietactics2.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.level.pathfinder.*;

import org.spongepowered.asm.mixin.*;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.Nullable;

import java.util.*;


@Mixin(PathFinder.class)
public abstract class PathFinderMixin {
    @Final @Shadow private final BinaryHeap openSet = new BinaryHeap();
    @Final @Shadow private final Node[] neighbors = new Node[32];
    @Mutable @Final @Shadow private final NodeEvaluator nodeEvaluator;
    @Mutable @Final @Shadow private final int maxVisitedNodes;

    @Shadow
    private Path reconstructPath(Node node, BlockPos pos, boolean bl) {
        return null;
    }
    
    public PathFinderMixin(NodeEvaluator nodeEvaluator, int maxVisitedNodes) {
        this.nodeEvaluator = nodeEvaluator;
        this.maxVisitedNodes = maxVisitedNodes;
    }

    /**
     * @author picopress
     * @reason testing h function
     */
    @Overwrite
    private float getBestH(Node node, Set<Target> targets) {
        float f = Float.MAX_VALUE;
        
        for(Target target: targets) {
            float g = node.distanceToSqr(target);
            target.updateBest(g, node);
            f = Math.min(g, f);
        }
        return f * 3 / 2;
    }
    
    /**
     * @author picopress
     * @reason optimization
     */
    @Overwrite
    private @Nullable Path findPath(ProfilerFiller profiler, Node node, Map<Target, BlockPos> targetPos, float maxRange, int accuracy, float searchDepthMultiplier) {
        profiler.push("find_path");
        profiler.markForCharting(MetricCategory.PATH_FINDING);
        Set<Target> set = targetPos.keySet();
        List<Target> list = Lists.newArrayListWithExpectedSize(set.size());
        int j = (int)(this.maxVisitedNodes * searchDepthMultiplier);
        int i = 0;

        float range2 = maxRange * maxRange;

        node.g = 0;
        node.h = this.getBestH(node, set);
        node.f = node.h;
        this.openSet.clear();
        this.openSet.insert(node);

        while(!this.openSet.isEmpty()) {
            ++ i;
            if (i >= j) {
                break;
            }

            Node node2 = this.openSet.pop();
            node2.closed = true;

            for(Target target: set) {
                if (node2.distanceManhattan(target) <= accuracy) {
                    target.setReached();
                    list.add(target);
                }
            }

            if (!list.isEmpty()) break;
            if (node2.distanceToSqr(node) < range2) {
                int k = this.nodeEvaluator.getNeighbors(this.neighbors, node2);

                for(int l = 0; l < k; ++ l) {
                    Node node3 = this.neighbors[l];
                    float f = node2.distanceToSqr(node3);
                    float g = node2.g + f + node3.costMalus;
                    node3.walkedDistance = node2.walkedDistance + f;
                    if (node3.walkedDistance < maxRange && (!node3.inOpenSet() || g < node3.g)) {
                        node3.cameFrom = node2;
                        node3.g = g;
                        node3.h = this.getBestH(node3, set);
                        if (node3.inOpenSet()) {
                            this.openSet.changeCost(node3, node3.g + node3.h);
                        } else {
                            node3.f = node3.g + node3.h;
                            this.openSet.insert(node3);
                        }
                    }
                }
            }
        }

        Optional<Path> optional = !list.isEmpty()? list.stream().map((tg) -> this.reconstructPath(tg.getBestNode(), targetPos.get(tg), true)).min(Comparator.comparingInt(path -> path != null? path.getNodeCount(): 0)): set.stream().map((x) -> this.reconstructPath(x.getBestNode(), targetPos.get(x), false)).min(Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount));
        profiler.pop();
        return optional.orElse(null);
    }
}
