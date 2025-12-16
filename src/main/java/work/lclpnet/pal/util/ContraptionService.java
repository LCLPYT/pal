package work.lclpnet.pal.util;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Predicate;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static net.minecraft.util.Mth.floor;

@Singleton
public class ContraptionService {

    @Inject
    public ContraptionService() {}

    public boolean isBoosterPlate(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos).is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE) && world.getBlockState(pos.below()).is(Blocks.GOLD_BLOCK);
    }

    public boolean isElevator(BlockGetter world, BlockPos.MutableBlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        BlockState state = world.getBlockState(pos);

        if (!state.is(Blocks.BEACON) || !isSurroundedByPistons(world, pos)) {
            return false;
        }

        pos.set(x, y, z);

        return isCorneredBy(pos, p -> world.getBlockState(p).is(Blocks.DIAMOND_BLOCK));
    }

    public boolean isJumpPad(BlockGetter world, BlockPos.MutableBlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        BlockState state = world.getBlockState(pos);

        if (!state.is(Blocks.PISTON) || state.getValue(PistonBaseBlock.FACING) != Direction.UP || !isSurroundedByPistons(world, pos)) {
            return false;
        }

        pos.set(x, y, z);

        return isCorneredBy(pos, p -> world.getBlockState(p).is(Blocks.IRON_BLOCK));
    }

    public boolean isTeleporter(Level world, BlockPos blockPos) {
        BlockState state = world.getBlockState(blockPos);

        return state.is(Blocks.LAPIS_BLOCK) && world.hasNeighborSignal(blockPos);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isSurroundedByPistons(BlockGetter world, BlockPos.MutableBlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            pos.set(x + direction.getStepX(), y, z + direction.getStepZ());

            BlockState state = world.getBlockState(pos);

            if (!state.is(Blocks.PISTON) || state.getValue(PistonBaseBlock.FACING) != direction.getOpposite()) {
                return false;
            }
        }

        return true;
    }

    private boolean isCorneredBy(BlockPos.MutableBlockPos pos, Predicate<BlockPos> predicate) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        // check (-1, -1), (1, -1), (-1, 1), (1, 1)
        for (int i = 0; i < 4; i++) {
            int ox = ((i & 1) << 1) - 1;
            int oz = (i & 2) - 1;

            pos.set(x + ox, y, z + oz);

            if (!predicate.test(pos)) {
                return false;
            }
        }

        return true;
    }

    public boolean findJumpPad(BlockGetter world, Vec3 pos, BlockPos.MutableBlockPos blockPos, double triggerMargin) {
        // if there is no valid block underneath, terminate early
        blockPos.set(floor(pos.x), floor(pos.y) - 1, floor(pos.z));
        BlockState state = world.getBlockState(blockPos);

        if (!state.is(Blocks.PISTON) && !state.is(Blocks.IRON_BLOCK)) {
            return false;
        }

        return find3x3(pos, blockPos, p -> isJumpPad(world, p), triggerMargin);
    }

    public boolean findElevator(BlockGetter world, Vec3 pos, BlockPos.MutableBlockPos blockPos, double triggerMargin) {
        // if there is no valid block underneath, terminate early
        blockPos.set(floor(pos.x), floor(pos.y) - 1, floor(pos.z));
        BlockState state = world.getBlockState(blockPos);

        if (!state.is(Blocks.PISTON) && !state.is(Blocks.DIAMOND_BLOCK) && !state.is(Blocks.BEACON)) {
            return false;
        }

        return find3x3(pos, blockPos, p -> isElevator(world, p), triggerMargin);
    }

    private boolean find3x3(Vec3 pos, BlockPos.MutableBlockPos blockPos, Predicate<BlockPos.MutableBlockPos> predicate, double triggerMargin) {
        int x = floor(pos.x);
        int y = floor(pos.y) - 1;
        int z = floor(pos.z);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                double distToCenter = max(abs(x + ox + 0.5 - pos.x()), abs(z + oz + 0.5 - pos.z()));

                if (distToCenter > triggerMargin) continue;

                blockPos.set(x + ox, y, z + oz);

                if (predicate.test(blockPos)) {
                    blockPos.set(x + ox, y, z + oz);
                    return true;
                }
            }
        }

        return false;
    }
}
