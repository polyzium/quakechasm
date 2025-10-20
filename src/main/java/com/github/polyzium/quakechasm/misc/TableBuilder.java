/*
 * Quakechasm, a Quake minigame plugin for Minecraft servers running PaperMC
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

package com.github.polyzium.quakechasm.misc;

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
        // Find the maximum number of columns across all rows
        int columnCount = 0;
        for (String[] row : rows) {
            columnCount = Math.max(columnCount, row.length);
        }
        
        int[] widths = new int[columnCount];
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        return widths;
    }

    public String build() {
        StringBuilder table = new StringBuilder();
        columnWidths = calculateColumnWidths();
        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            for (int i = 0; i < row.length; i++) {
                table.append(row[i]);
                int spacesToAdd = columnWidths[i] - row[i].length();
                table.append(" ".repeat(Math.max(0, spacesToAdd)));
                if (i < row.length-1)
                table.append(" ");
            }

            if (r < rows.size() - 1) {
                table.append("\n");
            }
        }
        return table.toString();
    }
}
