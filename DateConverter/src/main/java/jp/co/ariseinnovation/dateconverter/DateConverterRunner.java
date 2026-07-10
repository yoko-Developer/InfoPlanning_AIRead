package jp.co.ariseinnovation.dateconverter;

import jp.co.ariseinnovation.dateconverter.enums.OutputFormat;
import jp.co.ariseinnovation.dateconverter.service.CsvDateConverterService;
import jp.co.ariseinnovation.dateconverter.util.DateConstants;
import jp.co.ariseinnovation.dateconverter.util.MessageUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * コマンドライン実行用のランナークラス
 * 
 * Spring Bootアプリケーション起動時に実行され、コマンドライン引数を解析して
 * CSVファイルの和暦→西暦変換処理を実行します。
 * 
 * 対応する引数:
 * --in: 入力CSVファイルパス（必須）
 * --out: 出力CSVファイルパス（必須）
 * --cols: 変換対象列名（カンマ区切り、省略時はデフォルト列を使用）
 * --charset: 文字コード（省略時はUTF-8）
 * --format: 出力形式（YYYYMMDD または YYYYMM、省略時はYYYYMMDD）
 * --extra-separators: 追加の区切り文字（省略時は追加なし）
 */
@Component
public class DateConverterRunner implements CommandLineRunner {

    /** ログ出力用 */
    private static final Logger logger = LoggerFactory.getLogger(DateConverterRunner.class);

    /** CSV変換サービス */
    @Autowired
    private CsvDateConverterService csvDateConverterService;

    /**
     * アプリケーションのメイン処理
     * コマンドライン引数を解析し、CSV変換処理を実行します。
     * 
     * @param args コマンドライン引数
     * @throws Exception 処理中にエラーが発生した場合
     */
    @Override
    public void run(String... args) throws Exception {
        // 引数が指定されていない場合は使用方法を表示
        if (args.length == 0) {
            printUsage();
            return;
        }

        // コマンドライン引数を解析
        Arguments params = Arguments.parse(args);
        if (!params.isValid()) {
            printUsage();
            System.exit(2); // 引数エラーで終了
        }

        // 処理開始ログと設定情報を出力
        logger.info(MessageUtil.getMessage("app.start"));
        logger.info(MessageUtil.getMessage("config.input.file", params.input));
        logger.info(MessageUtil.getMessage("config.output.file", params.output));
        logger.info(MessageUtil.getMessage("config.target.columns", String.join(", ", params.columns)));
        logger.info(MessageUtil.getMessage("config.charset", params.charset));
        logger.info(MessageUtil.getMessage("config.output.format", params.outputFormat.getFormat()));
        logger.info(MessageUtil.getMessage("config.extra.separators", params.extraSeparators));

        // 必須コンテンツチェック（--required-content 指定時のみ）
        if (StringUtils.isNotBlank(params.requiredContent)) {
            Charset cs = Charset.forName(params.charset);
            boolean found;
            try (var lines = Files.lines(Path.of(params.input), cs)) {
                found = lines.anyMatch(line -> line.contains(params.requiredContent));
            }
            if (!found) {
                logger.info("必須コンテンツ '{}' が見つからないためスキップします: {}",
                        params.requiredContent, params.input);
                Files.copy(Path.of(params.input), Path.of(params.output),
                        StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }

        try {
            // CSV変換処理を実行
            int convertedCount = csvDateConverterService.convertCsvFile(
                params.input,
                params.output,
                params.columns,
                params.charset,
                params.outputFormat,
                params.extraSeparators
            );
            
            // 正常終了ログを出力
            logger.info(MessageUtil.getMessage("app.complete", convertedCount));
            
        } catch (Exception e) {
            // エラー発生時はログ出力してアプリケーションを終了
            logger.error(MessageUtil.getMessage("app.error", e.getMessage()), e);
            System.exit(1); // エラーで終了
        }
    }

    /**
     * 使用方法をコンソールに表示
     * 引数が不正な場合や引数が指定されていない場合に呼び出されます。
     */
    private void printUsage() {
        System.out.println(MessageUtil.getMessage("usage.header"));
        System.out.println(MessageUtil.getMessage("usage.command"));
        System.out.println();
        System.out.println(MessageUtil.getMessage("usage.options.header"));
        System.out.println(MessageUtil.getMessage("usage.option.in"));
        System.out.println(MessageUtil.getMessage("usage.option.out"));
        System.out.println(MessageUtil.getMessage("usage.option.cols"));
        System.out.println(MessageUtil.getMessage("usage.option.charset"));
        System.out.println(MessageUtil.getMessage("usage.option.format"));
        System.out.println(MessageUtil.getMessage("usage.option.extra.separators"));
        System.out.println(MessageUtil.getMessage("usage.option.required.content"));
        System.out.println();
        System.out.println(MessageUtil.getMessage("usage.example.header"));
        System.out.println(MessageUtil.getMessage("usage.example.command"));
        System.out.println();
        System.out.println(MessageUtil.getMessage("usage.target.columns.example"));
        System.out.println(MessageUtil.getMessage("usage.conversion.example"));
    }

    /**
     * コマンドライン引数を解析・保持するクラス
     * 
     * 解析対象の引数:
     * --in: 入力ファイルパス
     * --out: 出力ファイルパス  
     * --charset: 文字コード
     * --cols: 変換対象列（カンマ区切り）
     * --format: 出力形式（YYYYMMDD または YYYYMM）
     * --extra-separators: 追加の区切り文字
     */
    static class Arguments {
        /** 入力ファイルパス */
        String input;
        /** 出力ファイルパス */
        String output;
        /** 文字コード（デフォルト: UTF-8） */
        String charset = DateConstants.DEFAULT_CHARSET;
        /** 変換対象列のリスト */
        List<String> columns;
        /** 出力形式（デフォルト: YYYYMMDD） */
        OutputFormat outputFormat = OutputFormat.YYYYMMDD;
        /** 必須コンテンツ（指定した文字列を含まないCSVをスキップ、null=チェックなし） */
        String requiredContent;
        /** 追加の区切り文字（デフォルト: なし。この中に含まれる各文字が「.」として扱われる） */
        String extraSeparators = "";

        /**
         * 引数の妥当性をチェック
         * @return 必須項目（入力・出力ファイル）が設定されている場合true
         */
        boolean isValid() {
            return input != null && output != null && !input.isEmpty() && !output.isEmpty();
        }

        /**
         * コマンドライン引数を解析してArgumentsオブジェクトを作成（最適化版）
         * 
         * @param args コマンドライン引数配列
         * @return 解析結果のArgumentsオブジェクト
         */
        static Arguments parse(String[] args) {
            Arguments a = new Arguments();
            
            // 効率化：引数の妥当性チェックを事前に実行
            if (args.length % 2 != 0) {
                return a; // 奇数個の引数は無効
            }
            
            // 引数を2つずつペアで処理（オプション名 + 値）
            for (int i = 0; i < args.length - 1; i += 2) {
                String option = args[i];
                String value = args[i + 1];
                
                // 効率化：switch式を使用してオプション処理を最適化
                switch (option) {
                    case "--in" -> a.input = value;
                    case "--out" -> a.output = value;
                    case "--charset" -> a.charset = value;
                    case "--cols" -> a.columns = parseColumns(value);
                    case "--format" -> a.outputFormat = parseOutputFormat(value);
                    case "--required-content" -> a.requiredContent = value;
                    case "--extra-separators" -> a.extraSeparators = value != null ? value : "";
                    default -> { /* 未知のオプションは無視 */ }
                }
            }
            
            // 対象列が指定されていない場合はデフォルト値を設定
            if (a.columns == null || a.columns.isEmpty()) {
                a.columns = Arrays.asList(DateConstants.DEFAULT_TARGET_COLUMNS);
            }
            
            return a;
        }
        
        /**
         * カンマ区切りの列名文字列をリストに変換
         * 
         * @param colsString カンマ区切りの列名文字列
         * @return 列名のリスト（空白文字は除去）
         */
        private static List<String> parseColumns(String colsString) {
            return Arrays.stream(colsString.split(","))
                    .map(String::trim)          // 前後の空白を除去
                    .filter(s -> !s.isEmpty())  // 空文字列を除外
                    .toList();
        }

        /**
         * 出力形式文字列をOutputFormatに変換
         * 
         * @param formatString 出力形式文字列（YYYYMMDD または YYYYMM）
         * @return OutputFormat（無効な場合はデフォルトのYYYYMMDD）
         */
        private static OutputFormat parseOutputFormat(String formatString) {
            if (StringUtils.isBlank(formatString)) {
                return OutputFormat.YYYYMMDD;
            }
            
            String normalized = formatString.trim().toUpperCase();
            try {
                return OutputFormat.valueOf(normalized);
            } catch (IllegalArgumentException e) {
                // 無効な形式の場合はデフォルトを返す
                logger.warn("無効な出力形式が指定されました: {}. デフォルト形式 {} を使用します。", 
                           formatString, OutputFormat.YYYYMMDD.getFormat());
                return OutputFormat.YYYYMMDD;
            }
        }
    }
}