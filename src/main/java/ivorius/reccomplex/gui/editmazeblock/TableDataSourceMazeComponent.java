/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editmazeblock;

import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.structures.generic.maze.SavedMazeComponent;
import ivorius.reccomplex.utils.IvTranslations;

/**
 * Created by lukas on 26.04.15.
 */
public class TableDataSourceMazeComponent extends TableDataSourceSegmented implements TableCellPropertyListener, TableCellActionListener
{
    public static final int[] DEFAULT_MAX_COMPONENT_SIZE = {100, 100, 100};

    private SavedMazeComponent component;

    private TableNavigator navigator;
    private TableDelegate tableDelegate;

    public TableDataSourceMazeComponent(SavedMazeComponent component, TableNavigator navigator, TableDelegate tableDelegate)
    {
        this.component = component;
        this.navigator = navigator;
        this.tableDelegate = tableDelegate;
        addManagedSection(0, new TableDataSourceConnector(component.defaultConnector, IvTranslations.get("reccomplex.maze.connector.default")));
    }

    @Override
    public int numberOfSegments()
    {
        return 4;
    }

    @Override
    public int sizeOfSegment(int segment)
    {
        switch (segment)
        {
            case 1:
            case 2:
            case 3:
                return 1;
        }

        return super.sizeOfSegment(segment);
    }

    @Override
    public TableElement elementForIndexInSegment(GuiTable table, int index, int segment)
    {
        switch (segment)
        {
            case 1:
            {
                TableCellFloatNullable cell = new TableCellFloatNullable("weight", TableElements.toFloat(component.weight), 1.0f, 0, 10, "D", "C");
                cell.addPropertyListener(this);
                return new TableElementCell("Weight", cell);
            }
            case 2:
            {
                TableCellButton cell = new TableCellButton("rooms", new TableCellButton.Action("edit", "Edit"));
                cell.addListener(this);
                return new TableElementCell("Rooms", cell);
            }
            case 3:
            {
                TableCellButton cell = new TableCellButton("exits", new TableCellButton.Action("edit", "Edit"));
                cell.addListener(this);
                return new TableElementCell("Exits", cell);
            }
        }

        return super.elementForIndexInSegment(table, index, segment);
    }

    @Override
    public void actionPerformed(TableCell cell, String actionID)
    {
        if ("rooms".equals(cell.getID()))
        {
            navigator.pushTable(new GuiTable(tableDelegate, new TableDataSourceSelection(component.rooms, DEFAULT_MAX_COMPONENT_SIZE, tableDelegate, navigator)));
        }
        else if ("exits".equals(cell.getID()))
        {
            navigator.pushTable(new GuiTable(tableDelegate, new TableDataSourceMazePathList(component.exitPaths, tableDelegate, navigator, component.rooms.boundsLower(), component.rooms.boundsHigher())));
        }
    }

    @Override
    public void valueChanged(TableCellPropertyDefault cell)
    {
        if ("weight".equals(cell.getID()))
        {
            component.weight = TableElements.toDouble((Float) cell.getPropertyValue());
        }
    }
}
