package jp.co.ariseinnovation.loan_detail_merge_batch.service;

import java.util.Arrays;
import java.util.List;

public class CsvConstants {

    // 入力ヘッダ
    public static final String[] INPUT_HEADER = {
        "ItemName", "Page", "GID", "GrupID", "Value", "Conf", "KeyWord", "x", "y", "w", "h"
    };

    // 出力順序
    public static final List<String> ITEM_ORDER = Arrays.asList(
        "Image",
        "Image_jshfilename",
        "modifyDate",
        "processDate",
        "result",
        "formid",
        "name",
        "address",
        "relationship",
        "final_balance",
        "interest_and_rate",
        "interest",
        "interest_rate",
        "collateral"
    );

    // プレフィックス（共通化すると変更に強い）
    public static final String VALUE_PREFIX = "Value_";
    public static final String CONF_PREFIX = "Conf_";
    public static final String KEYWORD_PREFIX = "KeyWord_";
    public static final String X_PREFIX = "x_";
    public static final String Y_PREFIX = "y_";
    public static final String W_PREFIX = "w_";
    public static final String H_PREFIX = "h_";

    private CsvConstants() {
        // インスタンス化防止
    }
}
