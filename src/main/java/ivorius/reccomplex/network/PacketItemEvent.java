/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import ivorius.ivtoolkit.network.IvPacketHelper;
import ivorius.reccomplex.items.ItemSyncable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Created by lukas on 17.01.15.
 */
public class PacketItemEvent extends PacketEditInventoryItem
{
    public String context;
    public ByteBuf payload;

    public PacketItemEvent()
    {
    }

    public PacketItemEvent(int inventorySlot, ByteBuf payload, String context)
    {
        super(inventorySlot);
        this.payload = payload;
        this.context = context;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        super.fromBytes(buf);
        context = ByteBufUtils.readUTF8String(buf);
        payload = IvPacketHelper.readByteBuffer(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        super.toBytes(buf);
        ByteBufUtils.writeUTF8String(buf, context);
        IvPacketHelper.writeByteBuffer(buf, payload);
    }
}
