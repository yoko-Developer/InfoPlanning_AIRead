package jp.co.ariseinnovation.dateconverter.util;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * メッセージ取得ユーティリティクラス
 * 
 * messages.propertiesファイルからメッセージを取得するためのユーティリティです。
 * Spring MessageSourceを使用してメッセージの国際化対応と一元管理を実現します。
 * 
 * 使用例：
 * - MessageUtil.getMessage("app.start")
 * - MessageUtil.getMessage("csv.complete", 100, 50)
 * 
 * 設定：
 * - ベースネーム: messages（messages.propertiesファイル）
 * - エンコーディング: UTF-8
 * - デフォルトロケール: 日本語
 */
@Component
public class MessageUtil {
    
    /** Spring MessageSourceインスタンス */
    private static final MessageSource messageSource;
    
    /** デフォルトロケール（日本語） */
    private static final Locale DEFAULT_LOCALE = Locale.JAPANESE;
    
    /**
     * 静的初期化ブロック
     * MessageSourceの設定を行います
     */
    static {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");        // messages.propertiesを指定
        source.setDefaultEncoding("UTF-8");    // UTF-8エンコーディングを設定
        messageSource = source;
    }
    
    /**
     * パラメータなしメッセージを取得
     * 
     * @param key メッセージキー（messages.propertiesで定義されたキー）
     * @return 取得されたメッセージ文字列
     */
    public static String getMessage(String key) {
        return messageSource.getMessage(key, null, DEFAULT_LOCALE);
    }
    
    /**
     * パラメータ付きメッセージを取得
     *
     * メッセージ内の{0}、{1}等のプレースホルダーが引数で置換されます。
     *
     * 数値引数はあらかじめ文字列化してから渡す。内部で使用するjava.text.MessageFormatは
     * Number型の引数をロケールの数値書式（桁区切りのカンマ付き）で自動整形するため、
     * 1000以上の数値をそのまま渡すと「1,000行」のように意図しない桁区切りが挿入されるため。
     *
     * @param key メッセージキー（messages.propertiesで定義されたキー）
     * @param args メッセージのプレースホルダーに埋め込む引数
     * @return パラメータが埋め込まれたメッセージ文字列
     */
    public static String getMessage(String key, Object... args) {
        Object[] stringifiedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            stringifiedArgs[i] = args[i] != null ? String.valueOf(args[i]) : null;
        }
        return messageSource.getMessage(key, stringifiedArgs, DEFAULT_LOCALE);
    }
}