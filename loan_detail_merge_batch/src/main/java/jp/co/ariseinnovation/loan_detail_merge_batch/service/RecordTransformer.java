package jp.co.ariseinnovation.loan_detail_merge_batch.service;

import java.util.*;

public class RecordTransformer {

    /**
     * 縦→横変換
     * GrupID=-1 の行は minusOneRows に溜めて返す
     */
    public static List<Map<String, String>> toWide(List<String[]> input, List<String[]> minusOneRows) {
        List<Map<String, String>> out = new ArrayList<>();
        if (input.isEmpty()) return out;

        String[] header = input.get(0);
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            colIndex.put(header[i], i);
        }

        Map<String, Map<String, String>> grouped = new LinkedHashMap<>();

        for (int i = 1; i < input.size(); i++) {
            String[] row = input.get(i);
            String itemName = row[colIndex.get("ItemName")];
            String page = row[colIndex.get("Page")];
            String gid = row[colIndex.get("GID")];
            String grupId = row[colIndex.get("GrupID")];

            if ("-1".equals(grupId)) {
                minusOneRows.add(row);
                continue;
            }

            String key = page + "|" + gid + "|" + grupId;
            Map<String, String> record = grouped.computeIfAbsent(key, k -> new LinkedHashMap<>());
            record.put("Page", page);
            record.put("GID", gid);
            record.put("GrupID", grupId);

            record.put(CsvConstants.VALUE_PREFIX + itemName, row[colIndex.get("Value")]);
            record.put(CsvConstants.CONF_PREFIX + itemName, row[colIndex.get("Conf")]);
            record.put(CsvConstants.KEYWORD_PREFIX + itemName, row[colIndex.get("KeyWord")]);
            record.put(CsvConstants.X_PREFIX + itemName, row[colIndex.get("x")]);
            record.put(CsvConstants.Y_PREFIX + itemName, row[colIndex.get("y")]);
            record.put(CsvConstants.W_PREFIX + itemName, row[colIndex.get("w")]);
            record.put(CsvConstants.H_PREFIX + itemName, row[colIndex.get("h")]);
        }

        out.addAll(grouped.values());
        return out;
    }

    /**
     * 横→縦変換
     */
    public static List<String[]> toTall(List<Map<String, String>> wide, List<String[]> minusOneRows) {
        List<String[]> out = new ArrayList<>();
        out.add(CsvConstants.INPUT_HEADER);

        for (Map<String, String> record : wide) {
            String page = record.get("Page");
            String gid = record.get("GID");
            String grupId = record.get("GrupID");

            for (String item : CsvConstants.ITEM_ORDER) {
                String val = record.get(CsvConstants.VALUE_PREFIX + item);
                if (val != null) {
                    out.add(new String[]{
                        item, page, gid, grupId,
                        val,
                        record.get(CsvConstants.CONF_PREFIX + item),
                        record.get(CsvConstants.KEYWORD_PREFIX + item),
                        record.get(CsvConstants.X_PREFIX + item),
                        record.get(CsvConstants.Y_PREFIX + item),
                        record.get(CsvConstants.W_PREFIX + item),
                        record.get(CsvConstants.H_PREFIX + item)
                    });
                }
            }
        }

        // GrupID=-1 の行を最後に追加
        out.addAll(minusOneRows);

        return out;
    }

    /**
     * 並び替え（ItemName順）
     */
    public static List<String[]> reorderByItemName(List<String[]> input) {
        if (input.isEmpty()) return input;

        List<String[]> out = new ArrayList<>();
        out.add(input.get(0)); // ヘッダ

        Map<String, List<String[]>> grouped = new HashMap<>();
        for (int i = 1; i < input.size(); i++) {
            String[] row = input.get(i);
            grouped.computeIfAbsent(row[0], k -> new ArrayList<>()).add(row);
        }

        for (String item : CsvConstants.ITEM_ORDER) {
            List<String[]> rows = grouped.get(item);
            if (rows != null) out.addAll(rows);
        }
        for (Map.Entry<String, List<String[]>> e : grouped.entrySet()) {
            if (!CsvConstants.ITEM_ORDER.contains(e.getKey())) {
                out.addAll(e.getValue());
            }
        }
        return out;
    }
}
