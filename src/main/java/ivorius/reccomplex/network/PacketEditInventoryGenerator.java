/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import ivorius.reccomplex.worldgen.inventory.GenericItemCollection.Component;
import ivorius.reccomplex.worldgen.inventory.GenericItemCollectionRegistry;
import ivorius.reccomplex.worldgen.inventory.InventoryLoadException;

/**
 * Created by lukas on 03.08.14.
 */
public class PacketEditInventoryGenerator implements IMessage
{
    private String key;
    private Component inventoryGenerator;

    public PacketEditInventoryGenerator()
    {
    }

    public PacketEditInventoryGenerator(String key, Component inventoryGenerator)
    {
        this.key = key;
        this.inventoryGenerator = inventoryGenerator;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public Component getInventoryGenerator()
    {
        return inventoryGenerator;
    }

    public void setInventoryGenerator(Component inventoryGenerator)
    {
        this.inventoryGenerator = inventoryGenerator;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        key = ByteBufUtils.readUTF8String(buf);
        String json = ByteBufUtils.readUTF8String(buf);

        try
        {
            inventoryGenerator = GenericItemCollectionRegistry.createComponentFromJSON(json);
        }
        catch (InventoryLoadException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, key);
        String json = GenericItemCollectionRegistry.createJSONFromComponent(inventoryGenerator);
        ByteBufUtils.writeUTF8String(buf, json);
    }
}
