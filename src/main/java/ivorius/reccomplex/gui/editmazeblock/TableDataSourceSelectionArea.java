/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editmazeblock;

import ivorius.ivtoolkit.gui.IntegerRange;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.structures.generic.Selection;
import net.minecraft.util.EnumChatFormatting;

/**
* Created by lukas on 08.10.14.
*/
public class TableDataSourceSelectionArea extends TableDataSourceSegmented implements TableCellPropertyListener
{
    private Selection.Area area;

    private int[] dimensions;

    public TableDataSourceSelectionArea(Selection.Area area, int[] dimensions)
    {
        this.area = area;
        this.dimensions = dimensions;
    }

    @Override
    public int numberOfSegments()
    {
        return 2;
    }

    @Override
    public int sizeOfSegment(int segment)
    {
        return segment == 1 ? 3 : 1;
    }

    @Override
    public TableElement elementForIndexInSegment(GuiTable table, int index, int segment)
    {
        if (segment == 0)
        {
            TableCellBoolean cell = new TableCellBoolean("additive", area.isAdditive(), EnumChatFormatting.GREEN + "Additive", EnumChatFormatting.GOLD + "Subtractive");
            cell.addPropertyListener(this);
            return new TableElementCell(cell);
        }
        else if (segment == 1)
        {
            String title = String.format("Range: %s", index == 0 ? "X" : index == 1 ? "Y" : index == 2 ? "Z" : "" + index);
            IntegerRange intRange = new IntegerRange(area.getMinCoord()[index], area.getMaxCoord()[index]);
            TableCellIntegerRange cell = new TableCellIntegerRange("area" + index, intRange, 0, dimensions[index] - 1);
            cell.addPropertyListener(this);
            return new TableElementCell(title, cell);
        }

        return null;
    }

    @Override
    public void valueChanged(TableCellPropertyDefault cell)
    {
        if (cell.getID() != null)
        {
            if (cell.getID().startsWith("area"))
            {
                int dim = Integer.valueOf(cell.getID().substring(4));
                IntegerRange range = (IntegerRange) cell.getPropertyValue();
                area.setCoord(dim, range.getMin(), range.getMax());
            }
            else if ("additive".equals(cell.getID()))
            {
                area.setAdditive((Boolean) cell.getPropertyValue());
            }
        }
    }
}
