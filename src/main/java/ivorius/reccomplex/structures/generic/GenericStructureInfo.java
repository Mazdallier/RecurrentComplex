/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.structures.generic;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.gson.*;
import ivorius.ivtoolkit.blocks.BlockCoord;
import ivorius.ivtoolkit.blocks.IvBlockCollection;
import ivorius.ivtoolkit.tools.IvWorldData;
import ivorius.ivtoolkit.tools.NBTTagLists;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.blocks.GeneratingTileEntity;
import ivorius.reccomplex.blocks.RCBlocks;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.json.NbtToJson;
import ivorius.reccomplex.structures.*;
import ivorius.reccomplex.structures.generic.gentypes.MazeGenerationInfo;
import ivorius.reccomplex.structures.generic.gentypes.NaturalGenerationInfo;
import ivorius.reccomplex.structures.generic.gentypes.StructureGenerationInfo;
import ivorius.reccomplex.structures.generic.matchers.BlockMatcher;
import ivorius.reccomplex.structures.generic.matchers.DependencyMatcher;
import ivorius.reccomplex.structures.generic.transformers.*;
import ivorius.reccomplex.utils.NBTStorable;
import ivorius.ivtoolkit.tools.Pairs;
import ivorius.reccomplex.utils.RCAccessorEntity;
import ivorius.reccomplex.utils.RCAccessorWorldServer;
import ivorius.reccomplex.worldgen.inventory.InventoryGenerationHandler;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by lukas on 24.05.14.
 */
public class GenericStructureInfo implements StructureInfo<GenericStructureInfo.InstanceData>, Cloneable
{
    public static final int LATEST_VERSION = 3;
    public static final int MAX_GENERATING_LAYERS = 30;
    public final List<StructureGenerationInfo> generationInfos = new ArrayList<>();
    public final List<Transformer> transformers = new ArrayList<>();
    public final DependencyMatcher dependencies = new DependencyMatcher("");
    public NBTTagCompound worldDataCompound;
    public boolean rotatable;
    public boolean mirrorable;
    public Metadata metadata = new Metadata();
    public JsonObject customData;

    public static GenericStructureInfo createDefaultStructure()
    {
        GenericStructureInfo genericStructureInfo = new GenericStructureInfo();
        genericStructureInfo.rotatable = false;
        genericStructureInfo.mirrorable = false;

        genericStructureInfo.transformers.add(new TransformerNaturalAir(BlockMatcher.of(MCRegistrySpecial.INSTANCE, RCBlocks.negativeSpace, 1)));
        genericStructureInfo.transformers.add(new TransformerNegativeSpace(BlockMatcher.of(MCRegistrySpecial.INSTANCE, RCBlocks.negativeSpace, 0)));
        genericStructureInfo.transformers.add(new TransformerNatural(BlockMatcher.of(MCRegistrySpecial.INSTANCE, RCBlocks.naturalFloor, 0)));
        genericStructureInfo.transformers.add(new TransformerReplace(BlockMatcher.of(MCRegistrySpecial.INSTANCE, RCBlocks.naturalFloor, 1)).replaceWith(new WeightedBlockState(null, Blocks.air, 0, "")));

        genericStructureInfo.generationInfos.add(new NaturalGenerationInfo());

        return genericStructureInfo;
    }

    private static boolean isBiomeAllTypes(BiomeGenBase biomeGenBase, List<BiomeDictionary.Type> types)
    {
        for (BiomeDictionary.Type type : types)
        {
            if (!BiomeDictionary.isBiomeOfType(biomeGenBase, type))
                return false;
        }

        return true;
    }

    @Override
    public int[] structureBoundingBox()
    {
        if (worldDataCompound == null)
            return new int[]{0, 0, 0};

        NBTTagCompound compound = worldDataCompound.getCompoundTag("blockCollection");
        return new int[]{compound.getInteger("width"), compound.getInteger("height"), compound.getInteger("length")};
    }

    @Override
    public boolean isRotatable()
    {
        return rotatable;
    }

    @Override
    public boolean isMirrorable()
    {
        return mirrorable;
    }

    @Override
    public void generate(final StructureSpawnContext context, InstanceData instanceData)
    {
        World world = context.world;
        Random random = context.random;
        IvWorldData worldData = constructWorldData(world);

        // The world initializes the block event array after it generates the world - in the constructor
        // This hackily sets the field to a temporary value. Yay.
        if (world instanceof WorldServer)
            RCAccessorWorldServer.ensureBlockEventArray((WorldServer) world); // Hax

        IvBlockCollection blockCollection = worldData.blockCollection;
        int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
        BlockCoord origin = context.lowerCoord();

        Map<BlockCoord, TileEntity> tileEntities = new HashMap<>();
        for (TileEntity tileEntity : worldData.tileEntities)
        {
            BlockCoord key = new BlockCoord(tileEntity);
            tileEntities.put(key, tileEntity);
            IvWorldData.setTileEntityPosForGeneration(tileEntity, context.transform.apply(key, areaSize).add(origin));
        }

        List<Pair<Transformer, NBTStorable>> transformers = Pairs.of(this.transformers, instanceData.transformers);

        if (!context.generateAsSource)
        {
            for (Pair<Transformer, NBTStorable> pair : transformers)
            {
                Transformer transformer = pair.getLeft();
                NBTStorable transformerData = pair.getRight();
                if (transformer.generatesInPhase(transformerData, Transformer.Phase.BEFORE))
                    transformer.transform(transformerData, Transformer.Phase.BEFORE, context, worldData, transformers);
            }
        }

        for (int pass = 0; pass < 2; pass++)
        {
            for (BlockCoord sourceCoord : blockCollection)
            {
                Block block = blockCollection.getBlock(sourceCoord);
                int meta = blockCollection.getMetadata(sourceCoord);

                BlockCoord worldPos = context.transform.apply(sourceCoord, areaSize).add(origin);
                if (context.includes(worldPos) && MCRegistrySpecial.INSTANCE.isSafe(block))
                {
                    TileEntity tileEntity = tileEntities.get(sourceCoord);

                    if (pass == getPass(block, meta) && (context.generateAsSource || !skips(transformers, block, meta)))
                    {
                        if (context.generateAsSource || !(tileEntity instanceof GeneratingTileEntity) || ((GeneratingTileEntity) tileEntity).shouldPlaceInWorld(context, instanceData.tileEntities.get(sourceCoord)))
                        {
                            if (context.setBlock(worldPos, block, meta))
                            {
                                if (tileEntity != null && MCRegistrySpecial.INSTANCE.isSafe(tileEntity))
                                {
                                    world.setBlockMetadataWithNotify(worldPos.x, worldPos.y, worldPos.z, meta, 2); // TODO Figure out why some blocks (chests, furnace) need this

                                    world.setTileEntity(worldPos.x, worldPos.y, worldPos.z, tileEntity);
                                    tileEntity.updateContainingBlockInfo();

                                    if (!context.generateAsSource)
                                    {
                                        if (tileEntity instanceof IInventory)
                                        {
                                            IInventory inventory = (IInventory) tileEntity;
                                            InventoryGenerationHandler.generateAllTags(inventory, MCRegistrySpecial.INSTANCE.itemHidingMode(), random);
                                        }
                                    }
                                }
                                context.transform.rotateBlock(world, worldPos, block);
                            }
                        }
                        else
                            context.setBlock(worldPos, Blocks.air, 0); // Replace with air
                    }
                }
            }
        }

        if (!context.generateAsSource)
        {
            for (Pair<Transformer, NBTStorable> pair : transformers)
            {
                Transformer transformer = pair.getLeft();
                NBTStorable transformerData = pair.getRight();
                if (transformer.generatesInPhase(transformerData, Transformer.Phase.AFTER))
                    transformer.transform(transformerData, Transformer.Phase.AFTER, context, worldData, transformers);
            }
        }

        for (Entity entity : worldData.entities)
        {
            IvWorldData.transformEntityPosForGeneration(entity, context.transform, areaSize);
            IvWorldData.moveEntityForGeneration(entity, origin);

            if (context.includes(entity.posX, entity.posY, entity.posZ))
            {
                RCAccessorEntity.setEntityUniqueID(entity, UUID.randomUUID());
                world.spawnEntityInWorld(entity);
            }
        }

        if (!context.generateAsSource && context.generationLayer < MAX_GENERATING_LAYERS)
        {
            for (Map.Entry<BlockCoord, TileEntity> entry : tileEntities.entrySet())
                if (entry.getValue() instanceof GeneratingTileEntity)
                    ((GeneratingTileEntity) entry.getValue()).generate(context, instanceData.tileEntities.get(entry.getKey()));
        }
        else
        {
            RecurrentComplex.logger.warn("Structure generated with over " + MAX_GENERATING_LAYERS + " layers; most likely infinite loop!");
        }
    }

    @Override
    public InstanceData prepareInstanceData(StructurePrepareContext context)
    {
        InstanceData instanceData = new InstanceData();

        if (!context.generateAsSource)
        {
            IvWorldData worldData = constructWorldData(null);
            IvBlockCollection blockCollection = worldData.blockCollection;

            int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
            BlockCoord origin = context.lowerCoord();

            for (Transformer transformer : transformers)
                instanceData.transformers.add(transformer.prepareInstanceData(context));

            for (TileEntity tileEntity : worldData.tileEntities)
                if (tileEntity instanceof GeneratingTileEntity)
                {
                    BlockCoord key = new BlockCoord(tileEntity);
                    IvWorldData.setTileEntityPosForGeneration(tileEntity, context.transform.apply(key, areaSize).add(origin));
                    instanceData.tileEntities.put(key, (NBTStorable) ((GeneratingTileEntity) tileEntity).prepareInstanceData(context));
                }
        }

        return instanceData;
    }

    @Override
    public InstanceData loadInstanceData(StructureLoadContext context, final NBTBase nbt)
    {
        InstanceData instanceData = new InstanceData();
        instanceData.readFromNBT(context, nbt, transformers, constructWorldData(null));
        return instanceData;
    }

    private boolean skips(List<Pair<Transformer, NBTStorable>> transformers, final Block block, final int metadata)
    {
        return Iterables.any(transformers, new Predicate<Pair<Transformer, NBTStorable>>()
        {
            @Override
            public boolean apply(@Nullable Pair<Transformer, NBTStorable> input)
            {
                return input.getLeft().skipGeneration(input.getRight(), block, metadata);
            }
        });
    }

    public IvWorldData constructWorldData(World world)
    {
        return new IvWorldData(worldDataCompound, world, MCRegistrySpecial.INSTANCE.itemHidingMode());
    }

    @Override
    public <I extends StructureGenerationInfo> List<I> generationInfos(Class<I> clazz)
    {
        List<I> list = new ArrayList<>();
        for (StructureGenerationInfo info : generationInfos)
        {
            if (clazz.isAssignableFrom(info.getClass()))
                list.add((I) info);
        }

        return list;
    }

    @Override
    public StructureGenerationInfo generationInfo(String id)
    {
        for (StructureGenerationInfo info : generationInfos)
        {
            if (Objects.equals(info.id(), id))
                return info;
        }

        return null;
    }

    private int getPass(Block block, int metadata)
    {
        return (block.isNormalCube() || block.getMaterial() == Material.air) ? 0 : 1;
    }

    @Override
    public GenericStructureInfo copyAsGenericStructureInfo()
    {
        return (GenericStructureInfo) clone();
    }

    @Override
    public boolean areDependenciesResolved()
    {
        return dependencies.apply();
    }

    @Override
    public Object clone()
    {
        GenericStructureInfo genericStructureInfo = StructureRegistry.createStructureFromJSON(StructureRegistry.createJSONFromStructure(this));
        genericStructureInfo.worldDataCompound = (NBTTagCompound) worldDataCompound.copy();

        return genericStructureInfo;
    }

    public static class Serializer implements JsonDeserializer<GenericStructureInfo>, JsonSerializer<GenericStructureInfo>
    {
        public GenericStructureInfo deserialize(JsonElement jsonElement, Type par2Type, JsonDeserializationContext context)
        {
            JsonObject jsonobject = JsonUtils.getJsonElementAsJsonObject(jsonElement, "status");
            GenericStructureInfo structureInfo = new GenericStructureInfo();

            Integer version;
            if (jsonobject.has("version"))
            {
                version = JsonUtils.getJsonObjectIntegerFieldValue(jsonobject, "version");
            }
            else
            {
                version = LATEST_VERSION;
                RecurrentComplex.logger.warn("Structure JSON missing 'version', using latest (" + LATEST_VERSION + ")");
            }

            if (jsonobject.has("generationInfos"))
                Collections.addAll(structureInfo.generationInfos, context.<StructureGenerationInfo[]>deserialize(jsonobject.get("generationInfos"), StructureGenerationInfo[].class));

            if (version == 1)
                structureInfo.generationInfos.add(NaturalGenerationInfo.deserializeFromVersion1(jsonobject, context));

            {
                // Legacy version 2
                if (jsonobject.has("naturalGenerationInfo"))
                    structureInfo.generationInfos.add(NaturalGenerationInfo.getGson().fromJson(jsonobject.get("naturalGenerationInfo"), NaturalGenerationInfo.class));

                if (jsonobject.has("mazeGenerationInfo"))
                    structureInfo.generationInfos.add(MazeGenerationInfo.getGson().fromJson(jsonobject.get("mazeGenerationInfo"), MazeGenerationInfo.class));
            }

            if (jsonobject.has("transformers"))
                Collections.addAll(structureInfo.transformers, context.<Transformer[]>deserialize(jsonobject.get("transformers"), Transformer[].class));
            if (jsonobject.has("blockTransformers")) // Legacy
                Collections.addAll(structureInfo.transformers, context.<Transformer[]>deserialize(jsonobject.get("blockTransformers"), Transformer[].class));

            structureInfo.rotatable = JsonUtils.getJsonObjectBooleanFieldValueOrDefault(jsonobject, "rotatable", false);
            structureInfo.mirrorable = JsonUtils.getJsonObjectBooleanFieldValueOrDefault(jsonobject, "mirrorable", false);

            if (jsonobject.has("dependencyExpression"))
                structureInfo.dependencies.setExpression(JsonUtils.getJsonObjectStringFieldValue(jsonobject, "dependencyExpression"));
            else if (jsonobject.has("dependencies")) // Legacy
                structureInfo.dependencies.setExpression(DependencyMatcher.ofMods(context.<String[]>deserialize(jsonobject.get("dependencies"), String[].class)));

            if (jsonobject.has("worldData"))
                structureInfo.worldDataCompound = context.deserialize(jsonobject.get("worldData"), NBTTagCompound.class);
            else if (jsonobject.has("worldDataBase64"))
                structureInfo.worldDataCompound = NbtToJson.getNBTFromBase64(JsonUtils.getJsonObjectStringFieldValue(jsonobject, "worldDataBase64"));
            // And else it is taken out for packet size, or stored in the zip

            if (jsonobject.has("metadata")) // Else, use default
                structureInfo.metadata = context.deserialize(jsonobject.get("metadata"), Metadata.class);

            structureInfo.customData = JsonUtils.getJsonObjectFieldOrDefault(jsonobject, "customData", new JsonObject());

            return structureInfo;
        }

        public JsonElement serialize(GenericStructureInfo structureInfo, Type par2Type, JsonSerializationContext context)
        {
            JsonObject jsonobject = new JsonObject();

            jsonobject.addProperty("version", LATEST_VERSION);

            jsonobject.add("generationInfos", context.serialize(structureInfo.generationInfos));
            jsonobject.add("transformers", context.serialize(structureInfo.transformers));

            jsonobject.addProperty("rotatable", structureInfo.rotatable);
            jsonobject.addProperty("mirrorable", structureInfo.mirrorable);

            jsonobject.add("dependencyExpression", context.serialize(structureInfo.dependencies.getExpression()));

            if (!RecurrentComplex.USE_ZIP_FOR_STRUCTURE_FILES && structureInfo.worldDataCompound != null)
            {
                if (RecurrentComplex.USE_JSON_FOR_NBT)
                    jsonobject.add("worldData", context.serialize(structureInfo.worldDataCompound));
                else
                    jsonobject.addProperty("worldDataBase64", NbtToJson.getBase64FromNBT(structureInfo.worldDataCompound));
            }

            jsonobject.add("metadata", context.serialize(structureInfo.metadata));
            jsonobject.add("customData", structureInfo.customData);

            return jsonobject;
        }
    }

    public static class InstanceData implements NBTStorable
    {
        public static final String KEY_TRANSFORMERS = "transformers";
        public static final String KEY_TILE_ENTITIES = "tileEntities";

        public final List<NBTStorable> transformers = new ArrayList<>();
        public final Map<BlockCoord, NBTStorable> tileEntities = new HashMap<>();

        protected static NBTBase getTileEntityTag(NBTTagCompound tileEntityCompound, BlockCoord coord)
        {
            return tileEntityCompound.getTag(getTileEntityKey(coord));
        }

        private static String getTileEntityKey(BlockCoord coord)
        {
            return String.format("%d,%d,%d", coord.x, coord.y, coord.z);
        }

        public void readFromNBT(StructureLoadContext context, NBTBase nbt, List<Transformer> transformers, IvWorldData worldData)
        {
            IvBlockCollection blockCollection = worldData.blockCollection;
            NBTTagCompound compound = nbt instanceof NBTTagCompound ? (NBTTagCompound) nbt : new NBTTagCompound();

            List<NBTTagCompound> transformerCompounds = NBTTagLists.compoundsFrom(compound, KEY_TRANSFORMERS);
            for (int i = 0; i < transformerCompounds.size(); i++)
            {
                NBTTagCompound transformerCompound = transformerCompounds.get(i);
                this.transformers.add(transformers.get(i).loadInstanceData(context, transformerCompound.getTag("data")));
            }

            int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
            BlockCoord origin = context.lowerCoord();

            NBTTagCompound tileEntityCompound = compound.getCompoundTag(InstanceData.KEY_TILE_ENTITIES);
            for (TileEntity tileEntity : worldData.tileEntities)
            {
                if (tileEntity instanceof GeneratingTileEntity)
                {
                    BlockCoord key = new BlockCoord(tileEntity);
                    IvWorldData.setTileEntityPosForGeneration(tileEntity, context.transform.apply(key, areaSize).add(origin));
                    tileEntities.put(key, (NBTStorable) ((GeneratingTileEntity) tileEntity).loadInstanceData(context, getTileEntityTag(tileEntityCompound, key)));
                }
            }
        }

        @Override
        public NBTBase writeToNBT()
        {
            NBTTagCompound compound = new NBTTagCompound();

            NBTTagList transformerDatas = new NBTTagList();
            for (NBTStorable transformerData : this.transformers)
            {
                NBTTagCompound transformerCompound = new NBTTagCompound();
                transformerCompound.setTag("data", transformerData.writeToNBT());
                transformerDatas.appendTag(transformerCompound);
            }
            compound.setTag(KEY_TRANSFORMERS, transformerDatas);

            NBTTagCompound tileEntityCompound = new NBTTagCompound();
            for (Map.Entry<BlockCoord, NBTStorable> entry : tileEntities.entrySet())
                tileEntityCompound.setTag(getTileEntityKey(entry.getKey()), entry.getValue().writeToNBT());
            compound.setTag(KEY_TILE_ENTITIES, tileEntityCompound);

            return compound;
        }
    }
}
