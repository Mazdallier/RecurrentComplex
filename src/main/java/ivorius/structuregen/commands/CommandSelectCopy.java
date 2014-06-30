/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.structuregen.commands;

import ivorius.ivtoolkit.blocks.BlockArea;
import ivorius.ivtoolkit.blocks.BlockCoord;
import ivorius.ivtoolkit.tools.IvWorldData;
import ivorius.structuregen.entities.StructureEntityInfo;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Created by lukas on 09.06.14.
 */
public class CommandSelectCopy extends CommandSelectModify
{
    @Override
    public String getCommandName()
    {
        return "selectCopy";
    }

    @Override
    public String getCommandUsage(ICommandSender var1)
    {
        return "commands.selectCopy.usage";
    }

    @Override
    public void processCommandSelection(EntityPlayerMP player, StructureEntityInfo structureEntityInfo, BlockCoord point1, BlockCoord point2, String[] args)
    {
        BlockArea area = new BlockArea(point1, point2);
        IvWorldData worldData = new IvWorldData(player.worldObj, area, true);
        structureEntityInfo.setWorldDataClipboard(worldData.createTagCompound(area.getLowerCorner()));
    }
}