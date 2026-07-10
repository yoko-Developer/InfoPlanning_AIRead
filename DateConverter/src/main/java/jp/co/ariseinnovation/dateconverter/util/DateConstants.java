package jp.co.ariseinnovation.dateconverter.util;

/**
 * 日付変換アプリケーションで使用する定数定義クラス
 * 
 * アプリケーション全体で使用される定数値を一元管理します。
 * マジックナンバーの排除と保守性の向上を目的としています。
 */
public final class DateConstants {
    
    // ========== 和暦の基準年定義 ==========
    
    /** 昭和の基準年（昭和元年 = 1926年、計算用に1925を使用） */
    public static final int SHOWA_BASE_YEAR = 1925;
    
    /** 平成の基準年（平成元年 = 1989年、計算用に1988を使用） */
    public static final int HEISEI_BASE_YEAR = 1988;
    
    /** 令和の基準年（令和元年 = 2019年、計算用に2018を使用） */
    public static final int REIWA_BASE_YEAR = 2018;
    
    // ========== デフォルト設定 ==========
    
    /** 
     * デフォルトの変換対象列名
     * CSVの1列目（項目名）がこれらの文字列と一致する場合、
     * 5列目の値を日付変換の対象とします
     */
    public static final String[] DEFAULT_TARGET_COLUMNS = {
        "取得",      // 取得
        "取得日",    // 取得日
        "使用",      // 使用開始日等  
        "供用",      // 供用
        "供用日",    // 供用日
        "事業供用開始日",    // 事業供用開始日
        "契約"       // 契約日等
    };
    
    /** デフォルト文字コード */
    public static final String DEFAULT_CHARSET = "UTF-8";
    
    // ========== 処理設定 ==========
    
    /** 進捗表示間隔（行数） */
    public static final int PROGRESS_INTERVAL = 1000;
    
    /**
     * プライベートコンストラクタ
     * ユーティリティクラスのため、インスタンス化を禁止します
     */
    private DateConstants() {
        throw new AssertionError("DateConstants class should not be instantiated");
    }
}