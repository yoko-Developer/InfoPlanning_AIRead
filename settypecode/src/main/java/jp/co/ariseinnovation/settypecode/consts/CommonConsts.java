package jp.co.ariseinnovation.settypecode.consts;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public final class CommonConsts {

    private final static String[] EQUAL_CHECK_ARRAY = { "合", "計", "TOTAL", "ＴＯＴＡＬ" };
    private final static String[] CONTAIN_CHECK_ARRAY = { "合計", "小計" };
    private final static String[] NOT_CONTAIN_CHECK_ARRAY = { "その他",
                                                "計画",
                                                "計算",
                                                "計装",
                                                "小計字",
                                                "番地",
                                                "ス計",
                                                "の計",
                                                "一計",
                                                "栄計",
                                                "音計",
                                                "会計",
                                                "改計",
                                                "丸計",
                                                "技計",
                                                "居計",
                                                "九計",
                                                "熊計",
                                                "群計",
                                                "経計",
                                                "財計",
                                                "三計",
                                                "山計",
                                                "産計",
                                                "士計",
                                                "時計",
                                                "芝計",
                                                "社計",
                                                "主計",
                                                "住計",
                                                "昭計",
                                                "政計",
                                                "西計",
                                                "設計",
                                                "創計",
                                                "総計",
                                                "綜計",
                                                "測計",
                                                "速計",
                                                "大計",
                                                "地計",
                                                "電計",
                                                "都計",
                                                "東計",
                                                "統計",
                                                "日計",
                                                "博計",
                                                "百計",
                                                "裕計",
                                                "會計" };
    
    public final static String SUFFIX_CHECK = "計";
    public final static String TYPE_CODE_LONG = "01";
    public final static String TYPE_CODE_SHORT = "02";

    public final static boolean equals(String val) {
        for (String checkVal : EQUAL_CHECK_ARRAY) {
            if (StringUtils.equals(val,checkVal)) {
                return true;
            }
        }
        return false;
    }

    public final static boolean isContain(String val) {
        for (String checkVal : CONTAIN_CHECK_ARRAY) {
            if (val.contains(checkVal)) {
                return true;
            }
        }
        return false;
    }

    public final static boolean isNotContain(String val) {
        for (String checkVal : NOT_CONTAIN_CHECK_ARRAY) {
            if (val.contains(checkVal)) {
                return false;
            }
        }
        return true;
    }
}
