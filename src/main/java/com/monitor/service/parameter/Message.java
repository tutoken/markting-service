package com.monitor.service.parameter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;

public class Message extends LinkedHashSet<String> {

    enum ALIGNMENT {
        LEFT, RIGHT, CENTER
    }

    private static final Integer FIX_WIDTH = 2;

    public void addDirectMessage(String message) {
        super.add(message);
    }

    public void addCodeBlockMessage(String message) {
        super.add(String.format("```%s```", message));
    }

    public void addTable(String[] title, Map<String, ?> contents) {
        String table = this.createTable(title, contents.keySet().toArray(String[]::new), contents);
        super.add(table);
    }

    public void addTable(String[] title, String[] columnKeys, Map<String, ?> contents) {
        String table = this.createTable(title, columnKeys, contents);
        super.add(table);
    }

    private String createTable(String[] firstRow, String[] columnKeys, Map<String, ?> contents) {
        String[][] content = new String[columnKeys.length + 1][firstRow.length];
        int[] columnSize = new int[content[0].length];
        ALIGNMENT[][] alignments = new ALIGNMENT[content.length][content[0].length];

        content[0] = firstRow;
        for (int offset = 0; offset < firstRow.length; offset++) {
            if (firstRow[offset] == null) {
                throw new RuntimeException("First row must not contain null");
            }
            columnSize[offset] = firstRow[offset].length();
            alignments[0][offset] = ALIGNMENT.CENTER;
        }

        for (int i = 1; i < content.length; i++) {
            content[i][0] = columnKeys[i - 1];
            columnSize[0] = Math.max(columnSize[0], lengthOf(columnKeys[i - 1]));
            alignments[i][0] = ALIGNMENT.LEFT;

            for (int j = 1; j < firstRow.length; j++) {
                content[i][j] = getValue(contents, columnKeys[i - 1], firstRow[j]);
                columnSize[j] = Math.max(columnSize[j], lengthOf(content[i][j]));
                alignments[i][j] = ALIGNMENT.RIGHT;
            }
        }

        StringBuilder line = new StringBuilder("+");
        Arrays.stream(columnSize).forEach(k -> line.append("-".repeat(k + FIX_WIDTH)).append("+"));

        StringBuilder result = new StringBuilder(line);
        for (int i = 0; i < content.length; i++) {
            String[] strings = content[i];

            result.append("\n|");

            for (int j = 0; j < strings.length; j++) {
                result.append(format(strings[j], columnSize[j], alignments[i][j])).append("|");
            }
            result.append("\n").append(line);
        }

        return String.format("```%s```", result);
    }

    private int lengthOf(String s) {
        return s == null ? 0 : s.length();
    }

    private String getValue(Map<String, ?> contents, String key1, String key2) {
        Object content = contents.get(key1);
        if (content == null) {
            return "N/A";
        }
        if (content instanceof Map) {
            Object value = ((Map<?, ?>) content).get(key2);
            return value == null ? "N/A" : value.toString();
        }
        return content.toString();
    }

    private String format(String content, int size, ALIGNMENT align) {
        int length = size - lengthOf(content);
        if (size <= 0) {
            return content;
        }
        switch (align) {
            case LEFT:
                return String.format(" %s%s ", content, " ".repeat(length));
            case RIGHT:
                return String.format(" %s%s ", " ".repeat(length), content);
            default:
                int spaces = (length + FIX_WIDTH) / 2;
                StringBuilder result = new StringBuilder(content);
                result.append(" ".repeat(spaces));
                int left = length + FIX_WIDTH - spaces;
                return left > 0 ? result.insert(0, " ".repeat(left)).toString() : result.toString();
        }
    }
}