package jp.co.ariseinnovation.loan_detail_merge_batch.service;

import java.util.*;

public class RecordMerger {

    /**
     * レコードをマージする
     *
     * @param records            GrupID≠-1 の横持ちレコード
     * @param hasNameMinusOne    ItemName=name & GrupID=-1 が存在するか
     * @param hasAddressMinusOne ItemName=address & GrupID=-1 が存在するか
     * @return マージ後のレコードリスト
     */
    public static List<Map<String, String>> mergeRecords(
            List<Map<String, String>> records,
            boolean hasNameMinusOne,
            boolean hasAddressMinusOne
    ) {
        List<Map<String, String>> result = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, String> current = records.get(i);

            // ===== スキップ条件 =====
            String valName = current.getOrDefault("Value_name", "");
            String valAddr = current.getOrDefault("Value_address", "");
            String valInterest = current.getOrDefault("Value_interest", "");

            // 「利率」を含む → 削除対象
            if (valInterest.contains("利率")) {
                continue;
            }

            // 「所在地」を含む → 削除対象
            if (valName.contains("所在地")) {
                continue;
            }

            // 「借入」を含む → 「計」と同じ扱い（残す）
            if (valName.contains("借入")) {
                result.add(current);
                continue;
            }

            // 「計」を含む → 残す
            if (valName.contains("計") || valAddr.contains("計")) {
                result.add(current);
                continue;
            }

            // アルファベットのみ → スキップ
            if (valName.matches("^[A-Za-z]+$") || valAddr.matches("^[A-Za-z]+$")) {
                continue;
            }

            // ===== マージ処理 =====
            if (i + 1 < records.size()) {
                Map<String, String> next = records.get(i + 1);
                int currentId = Integer.parseInt(current.get("GrupID"));
                int nextId = Integer.parseInt(next.get("GrupID"));

                if (nextId == currentId + 1) {

                    // --- case 1: address=-1 がある場合 ---
                    if (hasAddressMinusOne) {
                        current.put("Value_address",
                                next.getOrDefault("Value_name", current.get("Value_address")));
                        current.put("Conf_address",
                                next.getOrDefault("Conf_name", current.get("Conf_address")));
                        current.put("KeyWord_address",
                                next.getOrDefault("KeyWord_name", current.get("KeyWord_address")));
                        current.put("x_address",
                                next.getOrDefault("x_name", current.get("x_address")));
                        current.put("y_address",
                                next.getOrDefault("y_name", current.get("y_address")));
                        current.put("w_address",
                                next.getOrDefault("w_name", current.get("w_address")));
                        current.put("h_address",
                                next.getOrDefault("h_name", current.get("h_address")));

                        // address 補完
                        if (isEmpty(current.get("Value_address"))) {
                            String addr1 = current.getOrDefault("Value_address", "");
                            String addr2 = next.getOrDefault("Value_address", "");
                            if (!addr1.isEmpty()) {
                                current.put("Value_address", addr1);
                            } else if (!addr2.isEmpty()) {
                                current.put("Value_address", addr2);
                            }
                        }
                    }

                    // --- case 2: name=-1 がある場合 ---
                    else if (hasNameMinusOne) {
                        // 住所スライド処理
                        current.put("Value_name", current.getOrDefault("Value_address", ""));
                        current.put("Conf_name", current.getOrDefault("Conf_address", ""));
                        current.put("KeyWord_name", current.getOrDefault("KeyWord_address", ""));
                        current.put("x_name", current.getOrDefault("x_address", ""));
                        current.put("y_name", current.getOrDefault("y_address", ""));
                        current.put("w_name", current.getOrDefault("w_address", ""));
                        current.put("h_name", current.getOrDefault("h_address", ""));

                        current.put("Value_address", next.getOrDefault("Value_address", ""));
                        current.put("Conf_address", next.getOrDefault("Conf_address", ""));
                        current.put("KeyWord_address", next.getOrDefault("KeyWord_address", ""));
                        current.put("x_address", next.getOrDefault("x_address", ""));
                        current.put("y_address", next.getOrDefault("y_address", ""));
                        current.put("w_address", next.getOrDefault("w_address", ""));
                        current.put("h_address", next.getOrDefault("h_address", ""));
                    }

                    // ===== interest_rate関連 =====
                    current.put("Value_interest_rate",
                            next.getOrDefault("Value_interest", current.get("Value_interest_rate")));
                    if (isEmpty(current.get("Value_interest_rate"))) {
                        String rate1 = current.getOrDefault("Value_interest_and_rate", "");
                        String rate2 = next.getOrDefault("Value_interest_and_rate", "");
                        if (!rate1.isEmpty()) {
                            current.put("Value_interest_rate", rate1);
                        } else if (!rate2.isEmpty()) {
                            current.put("Value_interest_rate", rate2);
                        }
                    }

                    // ===== final_balance関連 =====
                    if (isEmpty(current.get("Value_final_balance"))
                            && !isEmpty(next.get("Value_final_balance"))) {
                        current.put("Value_final_balance", next.get("Value_final_balance"));
                    }

                    // ===== collateral関連 =====
                    String col1 = current.getOrDefault("Value_collateral", "");
                    String col2 = next.getOrDefault("Value_collateral", "");
                    if (!col2.isEmpty()) {
                        if (col1.isEmpty()) {
                            current.put("Value_collateral", col2);
                        } else {
                            current.put("Value_collateral", col1 + ";" + col2);
                        }
                    }

                    result.add(current);
                    i++; // 次の行をスキップ
                    continue;
                }
            }

            // マージ対象がなければそのまま追加
            result.add(current);
        }

        return result;
    }

    private static boolean isEmpty(String val) {
        return val == null || val.isEmpty();
    }
}
