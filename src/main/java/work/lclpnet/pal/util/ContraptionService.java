package work.lclpnet.pal.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Predicate;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static net.minecraft.util.math.MathHelper.floor;

@Singleton
public class ContraptionService {

    @Inject
    public ContraptionService() {}

    public boolean isBoosterPlate(BlockView world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE) && world.getBlockState(pos.down()).isOf(Blocks.GOLD_BLOCK);
    }

    public boolean isElevator(BlockView world, BlockPos.Mutable pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        BlockState state = world.getBlockState(pos);

        if (!state.isOf(Blocks.BEACON) || !isSurroundedByPistons(world, pos)) {
            return false;
        }

        pos.set(x, y, z);

        return isCorneredBy(pos, p -> world.getBlockState(p).isOf(Blocks.DIAMOND_BLOCK));
    }

    public boolean isJumpPad(BlockView world, BlockPos.Mutable pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        BlockState state = world.getBlockState(pos);

        if (!state.isOf(Blocks.PISTON) || state.get(PistonBlock.FACING) != Direction.UP || !isSurroundedByPistons(world, pos)) {
            return false;
        }

        pos.set(x, y, z);

        return isCorneredBy(pos, p -> world.getBlockState(p).isOf(Blocks.IRON_BLOCK));
    }

    public boolean isTeleporter(World world, BlockPos blockPos) {
        BlockState state = world.getBlockState(blockPos);

        return state.isOf(Blocks.LAPIS_BLOCK) && world.isReceivingRedstonePower(blockPos);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isSurroundedByPistons(BlockView world, BlockPos.Mutable pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        for (Direction direction : Direction.Type.HORIZONTAL) {
            pos.set(x + direction.getOffsetX(), y, z + direction.getOffsetZ());

            BlockState state = world.getBlockState(pos);

            if (!state.isOf(Blocks.PISTON) || state.get(PistonBlock.FACING) != direction.getOpposite()) {
                return false;
            }
        }

        return true;
    }

    private boolean isCorneredBy(BlockPos.Mutable pos, Predicate<BlockPos> predicate) {
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

    public boolean findJumpPad(BlockView world, Vec3d pos, BlockPos.Mutable blockPos, double triggerMargin) {
        // if there is no valid block underneath, terminate early
        blockPos.set(floor(pos.x), floor(pos.y) - 1, floor(pos.z));
        BlockState state = world.getBlockState(blockPos);

        if (!state.isOf(Blocks.PISTON) && !state.isOf(Blocks.IRON_BLOCK)) {
            return false;
        }

        return find3x3(pos, blockPos, p -> isJumpPad(world, p), triggerMargin);
    }

    public boolean findElevator(BlockView world, Vec3d pos, BlockPos.Mutable blockPos, double triggerMargin) {
        // if there is no valid block underneath, terminate early
        blockPos.set(floor(pos.x), floor(pos.y) - 1, floor(pos.z));
        BlockState state = world.getBlockState(blockPos);

        if (!state.isOf(Blocks.PISTON) && !state.isOf(Blocks.DIAMOND_BLOCK) && !state.isOf(Blocks.BEACON)) {
            return false;
        }

        return find3x3(pos, blockPos, p -> isElevator(world, p), triggerMargin);
    }

    private boolean find3x3(Vec3d pos, BlockPos.Mutable blockPos, Predicate<BlockPos.Mutable> predicate, double triggerMargin) {
        int x = floor(pos.x);
        int y = floor(pos.y) - 1;
        int z = floor(pos.z);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                double distToCenter = max(abs(x + ox + 0.5 - pos.getX()), abs(z + oz + 0.5 - pos.getZ()));

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
