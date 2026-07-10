package jp.co.ariseinnovation.dateconverter.enums;

/**
 * 日付変換の出力形式を定義する列挙型
 */
public enum OutputFormat {
    /** YYYYMM形式（年月のみ） */
    YYYYMM("YYYYMM"),
    
    /** YYYYMMDD形式（年月日） */
    YYYYMMDD("YYYYMMDD");
    
    private final String format;
    
    OutputFormat(String format) {
        this.format = format;
    }
    
    public String getFormat() {
        return format;
    }
}