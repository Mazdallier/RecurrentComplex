/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editspawncommandblock;

import ivorius.reccomplex.blocks.TileEntitySpawnCommand;
import ivorius.reccomplex.gui.table.*;

/**
 * Created by lukas on 05.06.14.
 */
public class TableDataSourceSpawnCommandEntry extends TableDataSourceSegmented implements TableCellPropertyListener, TableCellActionListener
{
    private TileEntitySpawnCommand.Entry entry;

    private TableDelegate tableDelegate;

    public TableDataSourceSpawnCommandEntry(TileEntitySpawnCommand.Entry entry, TableDelegate tableDelegate)
    {
        this.entry = entry;
        this.tableDelegate = tableDelegate;
    }

    @Override
    public int numberOfSegments()
    {
        return 1;
    }

    @Override
    public int sizeOfSegment(int segment)
    {
        return 3;
    }

    @Override
    public TableElement elementForIndexInSegment(GuiTable table, int index, int segment)
    {
        if (index == 0)
        {
            TableCellPresetAction cell = new TableCellPresetAction("default", "Apply", new TableCellButton.Action("spawner", "Mob Spawner"), new TableCellButton.Action("entity", "Spawn Entity"));
            cell.addListener(this);
            return new TableElementCell("Preset", cell);
        }
        else if (index == 1)
        {
            TableCellString cell = new TableCellString("command", entry.command);
            cell.addPropertyListener(this);
            return new TableElementCell("Command", cell);
        }
        else if (index == 2)
        {
            TableCellFloatNullable cell = new TableCellFloatNullable("weight", TableElements.toFloat(entry.weight), 1.0f, 0, 10, "D", "C");
            cell.addPropertyListener(this);
            return new TableElementCell("Weight", cell);
        }

        return null;
    }

    @Override
    public void valueChanged(TableCellPropertyDefault cell)
    {
        if ("command".equals(cell.getID()))
        {
            entry.command = (String) cell.getPropertyValue();
        }
        else if ("weight".equals(cell.getID()))
        {
            entry.weight = TableElements.toDouble((Float) cell.getPropertyValue());
        }
    }

    @Override
    public void actionPerformed(TableCell cell, String actionID)
    {
        if ("default".equals(cell.getID()))
        {
            if ("spawner".equals(actionID))
                entry.command = "/setblock ~ ~ ~ mob_spawner 0 replace {EntityId:Zombie}";
            else if ("entity".equals(actionID))
                entry.command = "/summon Zombie ~ ~ ~";

            tableDelegate.reloadData();
        }
    }
}
