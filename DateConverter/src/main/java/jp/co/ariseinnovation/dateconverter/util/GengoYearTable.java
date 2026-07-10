package jp.co.ariseinnovation.dateconverter.util;

import java.util.List;

/**
 * 和暦の年号情報を管理するクラス
 * 
 * 効率化のため、不変オブジェクトとして設計し、
 * 静的初期化で全ての元号情報を事前構築します。
 */
public class GengoYearTable {
    
    /** 元号文字列 */
    public final String gengo;
    /** 西暦変換用の基準年 */
    public final int yearAdd;
    
    /**
     * コンストラクタ
     * @param gengo 元号文字列
     * @param yearAdd 西暦変換用の基準年
     */
    public GengoYearTable(String gengo, int yearAdd) {
        this.gengo = gengo;
        this.yearAdd = yearAdd;
    }
    
    // ========== 和暦定義（効率化のため定数として事前定義） ==========
    
    // フル表記
    public static final GengoYearTable SHOWA = new GengoYearTable("昭和", DateConstants.SHOWA_BASE_YEAR);
    public static final GengoYearTable HEISEI = new GengoYearTable("平成", DateConstants.HEISEI_BASE_YEAR);
    public static final GengoYearTable REIWA = new GengoYearTable("令和", DateConstants.REIWA_BASE_YEAR);
    
    // アルファベット略称
    public static final GengoYearTable SHOWA_S = new GengoYearTable("S", DateConstants.SHOWA_BASE_YEAR);
    public static final GengoYearTable HEISEI_H = new GengoYearTable("H", DateConstants.HEISEI_BASE_YEAR);
    public static final GengoYearTable REIWA_R = new GengoYearTable("R", DateConstants.REIWA_BASE_YEAR);
    public static final GengoYearTable REIWA_L = new GengoYearTable("L", DateConstants.REIWA_BASE_YEAR);
    
    // 漢字1文字略称
    public static final GengoYearTable SHOWA_1 = new GengoYearTable("昭", DateConstants.SHOWA_BASE_YEAR);
    public static final GengoYearTable HEISEI_1 = new GengoYearTable("平", DateConstants.HEISEI_BASE_YEAR);
    public static final GengoYearTable REIWA_1 = new GengoYearTable("令", DateConstants.REIWA_BASE_YEAR);
    
    /**
     * 全和暦のリスト（不変リストとして効率化）
     * 長い元号から順に配置することで、findMatchingGengoでのソート処理を削減
     */
    public static final List<GengoYearTable> table = List.of(
        // 長い順に配置（昭和、平成、令和が優先）
        SHOWA, HEISEI, REIWA,
        // アルファベット略称
        SHOWA_S, HEISEI_H, REIWA_R, REIWA_L,
        // 漢字1文字略称
        SHOWA_1, HEISEI_1, REIWA_1
    );
}