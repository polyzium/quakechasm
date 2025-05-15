/*
 * DarkChronics-Quake, a Quake minigame plugin for Minecraft servers running PaperMC
 * 
 * Copyright (C) 2024-present Polyzium
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.darkchronics.quake.misc;

import java.util.ArrayList;
import java.util.List;

public class TableBuilder {
    private List<String[]> rows = new ArrayList<>();
    private int[] columnWidths;

    public TableBuilder() {
    }

    public TableBuilder addRow(String... cells) {
        rows.add(cells);
        return this;
    }

    private int[] calculateColumnWidths() {
        int columnCount = rows.get(0).length;
        int[] widths = new int[columnCount];
        for (String[] row : rows) {
            for (int i = 0; i < columnCount; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        return widths;
    }

    private String generateSeparatorRow() {
        StringBuilder separator = new StringBuilder();
        separator.append("+");
        for (int width : columnWidths) {
            separator.append("-".repeat(Math.max(0, width + 2)));
            separator.append("+");
        }
        return separator.toString();
    }

    public String build() {
        StringBuilder table = new StringBuilder();
        columnWidths = calculateColumnWidths();
        String separatorRow = generateSeparatorRow();

        table.append(separatorRow).append("\n");
        for (String[] row : rows) {
            table.append("|");
            for (int i = 0; i < row.length; i++) {
                table.append(" ").append(row[i]);
                int spacesToAdd = columnWidths[i] - row[i].length();
                table.append(" ".repeat(Math.max(0, spacesToAdd)));
                table.append(" |");
            }
            table.append("\n");
            table.append(separatorRow).append("\n");
        }
        return table.toString();
    }
}
