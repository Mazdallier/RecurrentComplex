/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.commands;

import ivorius.ivtoolkit.blocks.BlockArea;
import ivorius.ivtoolkit.blocks.BlockCoord;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.entities.StructureEntityInfo;
import ivorius.reccomplex.utils.ServerTranslations;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import java.util.List;
import java.util.Set;

/**
 * Created by lukas on 09.06.14.
 */
public class CommandSelectReplace extends CommandSelectModify
{
    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "replace";
    }

    @Override
    public String getCommandUsage(ICommandSender var1)
    {
        return ServerTranslations.usage("commands.selectReplace.usage");
    }

    @Override
    public void processCommandSelection(EntityPlayerMP player, StructureEntityInfo structureEntityInfo, BlockCoord point1, BlockCoord point2, String[] args)
    {
        if (args.length >= 2)
        {
            World world = player.getEntityWorld();

            Block src = getBlockByText(player, args[0]);
            Block dst = getBlockByText(player, args[1]);
            int[] srcMeta = args.length >= 3 ? getMetadatas(args[2]) : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
            int[] dstMeta = args.length >= 4 ? getMetadatas(args[3]) : new int[]{0};

            for (BlockCoord coord : new BlockArea(point1, point2))
            {
                Block block = world.getBlock(coord.x, coord.y, coord.z);
                int meta = world.getBlockMetadata(coord.x, coord.y, coord.z);

                boolean correctMeta = false;
                for (int aMeta : srcMeta)
                {
                    if (aMeta == meta)
                        correctMeta = true;
                }

                if (correctMeta && src == block)
                {
                    world.setBlock(coord.x, coord.y, coord.z, dst, dstMeta[player.getRNG().nextInt(dstMeta.length)], 3);
                }
            }
        }
        else
        {
            throw ServerTranslations.wrongUsageException("commands.selectReplace.usage");
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender commandSender, String[] args)
    {
        if (args.length == 1 || args.length == 2)
            return getListOfStringsFromIterableMatchingLastWord(args, Block.blockRegistry.getKeys());
        else if (args.length == 3 || args.length == 4)
        {
            return getListOfStringsMatchingLastWord(args, "0");
        }

        return null;
    }
}
