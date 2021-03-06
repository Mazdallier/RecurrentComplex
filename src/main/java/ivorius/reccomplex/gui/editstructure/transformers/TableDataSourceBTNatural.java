/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editstructure.transformers;

import ivorius.reccomplex.gui.GuiValidityStateIndicator;
import ivorius.reccomplex.gui.TableDataSourceExpression;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.structures.generic.transformers.TransformerNatural;
import net.minecraft.block.Block;

/**
 * Created by lukas on 05.06.14.
 */
public class TableDataSourceBTNatural extends TableDataSourceSegmented
{
    private TransformerNatural transformer;

    public TableDataSourceBTNatural(TransformerNatural transformer)
    {
        this.transformer = transformer;

        addManagedSection(0, TableDataSourceExpression.constructDefault("Sources", transformer.sourceMatcher));
    }

    public TransformerNatural getTransformer()
    {
        return transformer;
    }

    public void setTransformer(TransformerNatural transformer)
    {
        this.transformer = transformer;
    }

    public static TableCellString elementForBlock(String id, Block block)
    {
        TableCellString element = new TableCellString(id, Block.blockRegistry.getNameForObject(block));
        element.setShowsValidityState(true);
        setStateForBlockTextfield(element);
        return element;
    }

    public static void setStateForBlockTextfield(TableCellString elementString)
    {
        elementString.setValidityState(stateForBlock(elementString.getPropertyValue()));
    }

    public static GuiValidityStateIndicator.State stateForBlock(String blockID)
    {
        return Block.blockRegistry.containsKey(blockID) ? GuiValidityStateIndicator.State.VALID : GuiValidityStateIndicator.State.INVALID;
    }
}
