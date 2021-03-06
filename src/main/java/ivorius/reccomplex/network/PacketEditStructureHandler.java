/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.entities.StructureEntityInfo;
import ivorius.reccomplex.gui.editstructure.GuiEditGenericStructure;
import ivorius.reccomplex.structures.generic.StructureSaveHandler;
import ivorius.reccomplex.structures.generic.GenericStructureInfo;
import ivorius.reccomplex.utils.ServerTranslations;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;

/**
 * Created by lukas on 03.08.14.
 */
public class PacketEditStructureHandler implements IMessageHandler<PacketEditStructure, IMessage>
{
    public static void openEditStructure(GenericStructureInfo structureInfo, String structureID, boolean saveAsActive, EntityPlayerMP player)
    {
        StructureEntityInfo structureEntityInfo = StructureEntityInfo.getStructureEntityInfo(player);

        if (structureEntityInfo != null)
            structureEntityInfo.setCachedExportStructureBlockDataNBT(structureInfo.worldDataCompound);

        RecurrentComplex.network.sendTo(new PacketEditStructure(structureInfo, structureID, saveAsActive,
                StructureSaveHandler.hasGenericStructure(structureID, true),
                StructureSaveHandler.hasGenericStructure(structureID, false)), player);
    }

    public static void finishEditStructure(GenericStructureInfo structureInfo, String structureID, boolean saveAsActive, boolean deleteOther)
    {
        RecurrentComplex.network.sendToServer(new PacketEditStructure(structureInfo, structureID, saveAsActive, deleteOther));
    }

    @Override
    public IMessage onMessage(PacketEditStructure message, MessageContext ctx)
    {
        if (ctx.side == Side.CLIENT)
        {
            onMessageClient(message, ctx);
        }
        else
        {
            NetHandlerPlayServer netHandlerPlayServer = ctx.getServerHandler();
            EntityPlayerMP player = netHandlerPlayServer.playerEntity;
            StructureEntityInfo structureEntityInfo = StructureEntityInfo.getStructureEntityInfo(player);

            GenericStructureInfo genericStructureInfo = message.getStructureInfo();

            if (structureEntityInfo != null)
                genericStructureInfo.worldDataCompound = structureEntityInfo.getCachedExportStructureBlockDataNBT();

            String path = StructureSaveHandler.getStructuresDirectoryName(message.isSaveAsActive()) + "/";
            String structureID = message.getStructureID();

            if (!StructureSaveHandler.saveGenericStructure(genericStructureInfo, structureID, message.isSaveAsActive()))
            {
                player.addChatMessage(ServerTranslations.format("structure.save.failure", path + structureID));
            }
            else
            {
                player.addChatMessage(ServerTranslations.format("structure.save.success", path + structureID));

                if (message.isDeleteOther() && StructureSaveHandler.hasGenericStructure(structureID, !message.isSaveAsActive()))
                {
                    String otherPath = StructureSaveHandler.getStructuresDirectoryName(!message.isSaveAsActive()) + "/";

                    if (StructureSaveHandler.deleteGenericStructure(structureID, !message.isSaveAsActive()))
                        player.addChatMessage(ServerTranslations.format("structure.delete.success", otherPath + structureID));
                    else
                        player.addChatMessage(ServerTranslations.format("structure.delete.failure", otherPath + structureID));
                }

                StructureSaveHandler.reloadAllCustomStructures();
            }
        }

        return null;
    }

    @SideOnly(Side.CLIENT)
    private void onMessageClient(PacketEditStructure message, MessageContext ctx)
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiEditGenericStructure(message.getStructureID(), message.getStructureInfo(), message.isSaveAsActive(), message.isStructureInActive(), message.isStructureInInactive()));
    }
}
