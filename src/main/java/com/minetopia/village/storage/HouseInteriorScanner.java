package com.minetopia.village.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * BFS flood-fill that counts usable floor space and beds across all floors
 * of a house structure. Starting point is the block directly behind the
 * ItemFrame marker (i.e. inside the building).
 *
 * <p>Floor space: every position where the block below is solid, the
 * position itself is passable, and the block above is passable — a
 * "standing spot". This naturally captures ground floor, first floor,
 * second floor, etc.
 *
 * <p>Beds: counted once per bed (HEAD part only).
 */
public final class HouseInteriorScanner {

    /** Larger than the chest scanner to handle multi-storey buildings. */
    private static final int MAX_FLOOD = 2048;

    private HouseInteriorScanner() {}

    /** Use when the interior start position is already known (e.g. from door detection). */
    public static HouseScanResult scan(ServerLevel level, BlockPos start) {
        return doScan(level, start);
    }

    public static HouseScanResult scan(ServerLevel level, BlockPos framePos, Direction frameDirection) {
        BlockPos start = pickStart(level, framePos, frameDirection);
        return doScan(level, start);
    }

    private static HouseScanResult doScan(ServerLevel level, BlockPos start) {

        int floorSpace = 0;
        int bedCount   = 0;

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty() && visited.size() <= MAX_FLOOD) {
            BlockPos pos   = queue.poll();
            BlockState state = level.getBlockState(pos);

            // Count beds before the collision check — beds are solid but still inside the room.
            if (state.getBlock() instanceof BedBlock
                    && state.getValue(BedBlock.PART) == BedPart.HEAD) {
                bedCount++;
            }

            // Blocks that have a non-empty collision shape halt the fill.
            // This catches glass panes, bars, thin walls etc. that blocksMotion()
            // misses (those return false for isSolid() but still block passage).
            if (!state.getCollisionShape(level, pos, CollisionContext.empty()).isEmpty()) continue;

            // Count floor positions: solid collision below, current pos passable (already
            // confirmed above), AND the block above also passable — 2-block standing clearance.
            BlockState below = level.getBlockState(pos.below());
            BlockState above = level.getBlockState(pos.above());
            if (!below.getCollisionShape(level, pos.below(), CollisionContext.empty()).isEmpty()
                    && above.getCollisionShape(level, pos.above(), CollisionContext.empty()).isEmpty()) {
                floorSpace++;
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return new HouseScanResult(floorSpace, bedCount);
    }

    /**
     * Picks the best passable starting block near the frame.
     * Tries behind the frame first (works when placed from outside),
     * then in front (works when placed from inside), then the frame block itself.
     */
    private static BlockPos pickStart(ServerLevel level, BlockPos framePos, Direction frameDirection) {
        Direction inward = frameDirection.getOpposite();

        // 1. Block directly behind the frame
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
