package jp.co.ariseinnovation.dateconverter.exception;

/**
 * フォーマットエラーを表す例外クラス
 */
public class FormatException extends Exception {
    
    /** シリアルバージョンUID（警告解消のため追加） */
    private static final long serialVersionUID = 1L;
    
    /**
     * デフォルトコンストラクタ
     */
    public FormatException() {
        super();
    }
    
    /**
     * メッセージ付きコンストラクタ
     * @param message エラーメッセージ
     */
    public FormatException(String message) {
        super(message);
    }
    
    /**
     * 原因付きコンストラクタ
     * @param message エラーメッセージ
     * @param cause 原因となる例外
     */
    public FormatException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 原因のみのコンストラクタ
     * @param cause 原因となる例外
     */
    public FormatException(Throwable cause) {
        super(cause);
    }
}