package jp.co.ariseinnovation.dateconverter.util;

import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageUtilのテストクラス
 */
class MessageUtilTest {

    @Test
    void testGetMessage_パラメータなし() {
        assertEquals("日付変換処理を開始します", MessageUtil.getMessage("app.start"));
        assertEquals("CSVファイルの和暦変換を開始します", MessageUtil.getMessage("csv.start"));
    }

    @Test
    void testGetMessage_単一パラメータ() {
        assertEquals("日付変換処理が正常に完了しました (変換行数: 5行)",
                MessageUtil.getMessage("app.complete", 5));
    }

    @Test
    void testGetMessage_複数パラメータ() {
        assertEquals("CSVファイルの和暦変換が完了しました (総行数: 100行, 変換行数: 50行)",
                MessageUtil.getMessage("csv.complete", 100, 50));
    }

    @Test
    void testGetMessage_日付変換メッセージ() {
        assertEquals("日付変換実行: [取得日] H10.5 → 199805",
                MessageUtil.getMessage("date.convert.success", "取得日", "H10.5", "199805"));
    }

    @Test
    void testGetMessage_存在しないキー() {
        assertThrows(NoSuchMessageException.class, () -> MessageUtil.getMessage("no.such.key"));
    }

    @Test
    void testGetMessage_設定情報メッセージ() {
        assertEquals("入力ファイル: input.csv", MessageUtil.getMessage("config.input.file", "input.csv"));
        assertEquals("出力形式: YYYYMM", MessageUtil.getMessage("config.output.format", "YYYYMM"));
    }

    @Test
    void testGetMessage_追加区切り文字メッセージ() {
        // 本日追加したキー：プレースホルダー数と実引数の不一致がないことを確認する
        assertEquals("追加の区切り文字: ／~", MessageUtil.getMessage("config.extra.separators", "／~"));
        assertEquals("追加の区切り文字: ", MessageUtil.getMessage("config.extra.separators", ""));
    }

    @Test
    void testGetMessage_無効な日付メッセージ() {
        // 本日追加したキー：4つのプレースホルダー（{0}〜{3}）に対して4引数を渡して例外なく整形できることを確認する
        assertEquals("実在しない日付が指定されました: H10.2.30 (西暦1998年2月30日は存在しません)",
                MessageUtil.getMessage("date.convert.invalid.date", "H10.2.30", 1998, 2, 30));
    }

    /**
     * 回帰テスト：数値の桁区切り（カンマ）が意図せず挿入されないことを確認する。
     * MessageFormatはInteger等の数値型引数をロケールの数値書式（桁区切り付き）で
     * 自動整形するため、1000以上の数値を渡すと「1,000行」のように表示が崩れる。
     */
    @Test
    void testGetMessage_1000以上の数値でも桁区切りカンマが入らない() {
        assertEquals("CSVファイルの和暦変換が完了しました (総行数: 1500行, 変換行数: 1200行)",
                MessageUtil.getMessage("csv.complete", 1500, 1200));
        assertEquals("処理進捗: 2000行目を処理中...",
                MessageUtil.getMessage("csv.progress", 2000));
        assertEquals("日付変換処理が正常に完了しました (変換行数: 1234行)",
                MessageUtil.getMessage("app.complete", 1234));
    }

    /**
     * 網羅性確保のためのテスト：アプリケーションコードから実際に呼び出されている
     * 全メッセージキーについて、プレースホルダー数と実引数の数が一致し、
     * 例外を投げずに正しく整形されることを機械的に確認する。
     * （config.detailsは実引数が1つ不足しており{4}が未解決のまま出力される不具合があった）
     */
    @Test
    void testGetMessage_アプリケーションから呼ばれる全キーが正しく整形される() {
        assertEquals("日付変換処理でエラーが発生しました: 原因メッセージ",
                MessageUtil.getMessage("app.error", "原因メッセージ"));
        assertEquals("CSVファイル変換処理でエラーが発生しました: 原因メッセージ",
                MessageUtil.getMessage("csv.error", "原因メッセージ"));
        assertEquals("対象列: 取得, 使用, 供用",
                MessageUtil.getMessage("config.target.columns", "取得, 使用, 供用"));
        assertEquals("文字コード: UTF-8",
                MessageUtil.getMessage("config.charset", "UTF-8"));
        assertEquals("変換設定 - 入力: in.csv, 出力: out.csv, 対象列: [取得日], 文字コード: UTF-8, 出力形式: YYYYMMDD",
                MessageUtil.getMessage("config.details", "in.csv", "out.csv", "[取得日]", "UTF-8", "YYYYMMDD"));
        assertEquals("未対応の年号が指定されました: X (値: X10.5)",
                MessageUtil.getMessage("date.convert.unsupported.era", "X", "X10.5"));
        assertEquals("日付形式として認識できませんでした: invalid (理由: invalid is not parsable)",
                MessageUtil.getMessage("date.convert.parse.failed", "invalid", "invalid is not parsable"));
        assertEquals("日付文字列の解析に失敗しました: invalid (エラー: 理由)",
                MessageUtil.getMessage("date.parse.failed", "invalid", "理由"));
    }

    @Test
    void testGetMessage_RequiredContentのヘルプ表示() {
        // .propertiesの仕様上、値の先頭の空白は読み込み時に削除されるため、期待値は先頭スペースなし
        // （既存のusage.option.*キーも同様に、定義上のインデントは実行時には表示されない）
        assertEquals("--required-content <str>  この文字列を含まないファイルは変換せずそのままコピー（デフォルト: チェックなし）",
                MessageUtil.getMessage("usage.option.required.content"));
    }

    /**
     * null引数が渡された場合（例：例外のgetMessage()がnullを返すケース）でも、
     * 例外を投げずに整形できることを確認する。
     */
    @Test
    void testGetMessage_null引数でも例外を投げない() {
        assertEquals("日付変換処理でエラーが発生しました: null",
                MessageUtil.getMessage("app.error", (Object) null));
    }
}
