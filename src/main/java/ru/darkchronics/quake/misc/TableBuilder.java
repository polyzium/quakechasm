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
