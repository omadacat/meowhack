package cat.omada.meowhack.util.world;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlastResistantBlocks {
    public static boolean isUnbreakable(BlockPos pos) {
        if (mc.world == null) return false;
        var block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.BEDROCK || block == Blocks.BARRIER
            || block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK
            || block == Blocks.REPEATING_COMMAND_BLOCK;
    }
}
