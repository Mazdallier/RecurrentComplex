/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.table;

import net.minecraft.client.gui.GuiButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lukas on 02.06.14.
 */
public class TableCellButton extends TableCellDefault
{
    private GuiButton[] buttons = new GuiButton[0];
    private Action[] actions;

    private List<TableCellActionListener> listeners = new ArrayList<>();

    public TableCellButton(String id, Action... actions)
    {
        super(id);
        this.actions = actions;
    }

    public void addListener(TableCellActionListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(TableCellActionListener listener)
    {
        listeners.remove(listener);
    }

    public List<TableCellActionListener> listeners()
    {
        return Collections.unmodifiableList(listeners);
    }

    public Action[] getActions()
    {
        return actions;
    }

    @Override
    public void initGui(GuiTable screen)
    {
        super.initGui(screen);

        Bounds bounds = bounds();

        buttons = new GuiButton[actions.length];
        int buttonWidth = bounds.getWidth() / actions.length;
        for (int i = 0; i < actions.length; i++)
        {
            Action action = actions[i];
            int realWidth = buttonWidth - (i == actions.length - 1 ? 0 : 2);
            buttons[i] = new GuiButton(-1, bounds.getMinX() + buttonWidth * i, bounds.getMinY() + (bounds.getHeight() - 20) / 2, realWidth, 20, action.title);
            buttons[i].visible = !isHidden();
            buttons[i].enabled = action.enabled;
            screen.addButton(this, i, buttons[i]);
        }
    }

    @Override
    public void setHidden(boolean hidden)
    {
        super.setHidden(hidden);

        for (GuiButton button : buttons)
        {
            button.visible = !hidden;
        }
    }

    @Override
    public void buttonClicked(int buttonID)
    {
        super.buttonClicked(buttonID);

        for (TableCellActionListener listener : listeners)
            listener.actionPerformed(this, actions[buttonID].id);
    }

    @Override
    public void drawFloating(GuiTable screen, int mouseX, int mouseY, float partialTicks)
    {
        super.drawFloating(screen, mouseX, mouseY, partialTicks);

        for (int i = 0; i < actions.length; i++)
        {
            Action action = actions[i];
            GuiButton button = buttons[i];
            if (action.tooltip != null)
                screen.drawTooltipRect(action.tooltip, TableCellPresetAction.getBounds(button), mouseX, mouseY, getFontRenderer());
        }
    }

    public static class Action
    {
        public String id;
        public String title;
        public List<String> tooltip;
        public boolean enabled;

        public Action(String id, String title, List<String> tooltip, boolean enabled)
        {
            this.id = id;
            this.title = title;
            this.tooltip = tooltip;
            this.enabled = enabled;
        }

        public Action(String id, String title, boolean enabled)
        {
            this(id, title, null, enabled);
        }

        public Action(String id, String title, List<String> tooltip)
        {
            this(id, title, tooltip, true);
        }

        public Action(String id, String title)
        {
            this(id, title, null, true);
        }
    }
}
