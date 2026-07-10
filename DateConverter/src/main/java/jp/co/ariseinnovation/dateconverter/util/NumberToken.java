package jp.co.ariseinnovation.dateconverter.util;

/**
 * 日付文字列から抽出された数値トークンを管理するクラス（最適化版）
 * 
 * 不変オブジェクトとして設計し、必要最小限の機能のみを提供します。
 */
public class NumberToken {

    /** 数値 */
    public final int token;
    /** 数値の文字列表現 */
    public final String tokenAsString;
    /** 後続文字列 */
    public final String afterWord;

    /**
     * 数値のみのコンストラクタ
     * @param token 数値
     */
    public NumberToken(int token) {
        this.token = token;
        this.tokenAsString = String.valueOf(token);
        this.afterWord = "";
    }

    /**
     * 文字列から数値を抽出するコンストラクタ
     * @param tokenAsString 数値文字列
     */
    public NumberToken(String tokenAsString) {
        this.tokenAsString = tokenAsString;
        this.token = Integer.parseInt(tokenAsString);
        this.afterWord = "";
    }

    /**
     * 数値と後続文字列を指定するコンストラクタ
     * @param tokenAsString 数値文字列
     * @param afterWord 後続文字列
     */
    public NumberToken(String tokenAsString, String afterWord) {
        this.tokenAsString = tokenAsString;
        this.token = Integer.parseInt(tokenAsString);
        this.afterWord = afterWord != null ? afterWord : "";
    }

    /**
     * 後続文字列に指定の文字が含まれているかチェック
     * @param word チェックする文字
     * @return 含まれている場合true
     */
    public boolean hasAfter(String word) {
        return word != null && afterWord.contains(word);
    }

    /**
     * 配列を作成するファクトリメソッド（効率化版）
     * @param tokens 数値の配列
     * @return NumberTokenの配列
     */
    public static NumberToken[] createArray(int... tokens) {
        NumberToken[] result = new NumberToken[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = new NumberToken(tokens[i]);
        }
        return result;
    }

    /**
     * 配列を作成するファクトリメソッド（文字列版、効率化版）
     * @param tokens 数値文字列の配列
     * @return NumberTokenの配列
     */
    public static NumberToken[] createArray(String... tokens) {
        NumberToken[] result = new NumberToken[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = new NumberToken(tokens[i]);
        }
        return result;
    }
}