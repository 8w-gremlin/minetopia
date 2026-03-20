package com.minetopia.village.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * BFS flood-fill starting from the interior side of an ItemFrame to find
 * all chests contained within the structure.
 */
public final class StructureInteriorScanner {

    /** Maximum blocks to visit before giving up — prevents runaway scans. */
    private static final int MAX_FLOOD = 512;

    private StructureInteriorScanner() {}

    /** Use when the interior start position is already known (e.g. from door detection). */
    public static List<BlockPos> findChests(ServerLevel level, BlockPos start) {
        return doScan(level, start);
    }

    public static List<BlockPos> findChests(ServerLevel level, BlockPos framePos, Direction frameDirection) {
        BlockPos start = pickStart(level, framePos, frameDirection);
        return doScan(level, start);
    }

    private static List<BlockPos> doScan(ServerLevel level, BlockPos start) {

        List<BlockPos> chests = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty() && visited.size() <= MAX_FLOOD) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);

            if (level.getBlockEntity(pos) instanceof ChestBlockEntity) {
                if (!chests.contains(pos)) {
                    chests.add(pos);
                    // Include the partner half of a double chest
                    if (state.getBlock() instanceof ChestBlock) {
                        net.minecraft.core.Direction connectedDir = ChestBlock.getConnectedDirection(state);
                        if (connectedDir != null) {
                            BlockPos partner = pos.relative(connectedDir);
                            if (!chests.contains(partner)) {
                                chests.add(partner);
                                visited.add(partner);
                            }
                        }
                    }
                }
                // Chests block motion — do not flood-fill through them
                continue;
            }

            if (!state.getCollisionShape(level, pos, CollisionContext.empty()).isEmpty()) continue;

            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return chests;
    }

    private static BlockPos pickStart(ServerLevel level, BlockPos framePos, Direction frameDirection) {
        Direction inward = frameDirection.getOpposite();

        // 1. Block directly behind the frame (interior side for interior-placed frames)
        BlockPos behind = framePos.relative(inward);
        if (!level.getBlockState(behind).blocksMotion()) return behind;

        // 2. One block further in — punches through the wall for exterior-placed frames
        BlockPos throughWall = framePos.relative(inward, 2);
        if (!level.getBlockState(throughWall).blocksMotion()) return throughWall;

        // 3. Frame block itself
        if (!level.getBlockState(framePos).blocksMotion()) return framePos;

        // 4. Last resort: in front of the frame
        return framePos.relative(frameDirection);
    }
}
