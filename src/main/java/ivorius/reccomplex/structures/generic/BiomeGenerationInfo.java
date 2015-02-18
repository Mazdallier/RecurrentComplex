/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.structures.generic;

import com.google.gson.*;
import ivorius.reccomplex.json.JsonUtils;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.biome.BiomeGenBase;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lukas on 24.05.14.
 */
public class BiomeGenerationInfo
{
    private BiomeSelector biomeSelector;
    private Double generationWeight;

    public BiomeGenerationInfo(String biomeID, Double generationWeight)
    {
        this.biomeSelector = new BiomeSelector(biomeID);
        this.generationWeight = generationWeight;
    }

    public static List<BiomeGenerationInfo> overworldBiomeGenerationList()
    {
        return Arrays.asList(new BiomeGenerationInfo("Type:WATER", 0.0),
                new BiomeGenerationInfo("Type:PLAINS", null),
                new BiomeGenerationInfo("Type:FOREST", null),
                new BiomeGenerationInfo("Type:MOUNTAIN", null),
                new BiomeGenerationInfo("Type:HILLS", null),
                new BiomeGenerationInfo("Type:SWAMP", null),
                new BiomeGenerationInfo("Type:SANDY", null),
                new BiomeGenerationInfo("Type:MESA", null),
                new BiomeGenerationInfo("Type:SAVANNA", null),
                new BiomeGenerationInfo("Type:WASTELAND", null),
                new BiomeGenerationInfo("Type:MUSHROOM", null),
                new BiomeGenerationInfo("Type:JUNGLE", null));
    }

    public static List<BiomeGenerationInfo> undergroundBiomeGenerationList()
    {
        return Arrays.asList(new BiomeGenerationInfo("Type:PLAINS", null),
                new BiomeGenerationInfo("Type:FOREST", null),
                new BiomeGenerationInfo("Type:MOUNTAIN", null),
                new BiomeGenerationInfo("Type:HILLS", null),
                new BiomeGenerationInfo("Type:SWAMP", null),
                new BiomeGenerationInfo("Type:SANDY", null),
                new BiomeGenerationInfo("Type:MESA", null),
                new BiomeGenerationInfo("Type:SAVANNA", null),
                new BiomeGenerationInfo("Type:RIVER", null),
                new BiomeGenerationInfo("Type:OCEAN", null),
                new BiomeGenerationInfo("Type:WASTELAND", null),
                new BiomeGenerationInfo("Type:MUSHROOM", null),
                new BiomeGenerationInfo("Type:JUNGLE", null));
    }

    public static List<BiomeGenerationInfo> oceanBiomeGenerationList()
    {
        return Arrays.asList(new BiomeGenerationInfo("Type:OCEAN,SNOWY", 0.0),
                new BiomeGenerationInfo("Type:OCEAN", null));
    }

    public String getBiomeID()
    {
        return biomeSelector.getBiomeID();
    }

    public void setBiomeID(String biomeID)
    {
        biomeSelector.setBiomeID(biomeID);
    }

    public Double getGenerationWeight()
    {
        return generationWeight;
    }

    public void setGenerationWeight(Double generationWeight)
    {
        this.generationWeight = generationWeight;
    }

    public double getActiveGenerationWeight()
    {
        return generationWeight != null ? generationWeight : 1.0;
    }

    public boolean hasDefaultWeight()
    {
        return generationWeight == null;
    }

    public boolean matches(BiomeGenBase biome)
    {
        return biomeSelector.matches(biome);
    }

    public boolean isTypeList()
    {
        return biomeSelector.isTypeList();
    }

    public String getDisplayString()
    {
        String biomeID = biomeSelector.getBiomeID();
        if (biomeSelector.isTypeList())
            return EnumChatFormatting.AQUA + biomeID.substring(5) + EnumChatFormatting.RESET;
        else
            return biomeID;
    }

    public static class Serializer implements JsonDeserializer<BiomeGenerationInfo>, JsonSerializer<BiomeGenerationInfo>
    {
        @Override
        public BiomeGenerationInfo deserialize(JsonElement jsonElement, Type par2Type, JsonDeserializationContext context)
        {
            JsonObject jsonobject = JsonUtils.getJsonElementAsJsonObject(jsonElement, "generationInfo");

            String biomeID = JsonUtils.getJsonObjectStringFieldValue(jsonobject, "biome");
            Double weight = jsonobject.has("weight") ? JsonUtils.getJsonObjectDoubleFieldValue(jsonobject, "weight") : null;

            return new BiomeGenerationInfo(biomeID, weight);
        }

        @Override
        public JsonElement serialize(BiomeGenerationInfo generationInfo, Type par2Type, JsonSerializationContext context)
        {
            JsonObject jsonobject = new JsonObject();

            jsonobject.addProperty("biome", generationInfo.getBiomeID());
            if (generationInfo.generationWeight != null)
                jsonobject.addProperty("weight", generationInfo.generationWeight);

            return jsonobject;
        }
    }
}