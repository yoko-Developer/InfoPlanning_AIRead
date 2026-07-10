package jp.co.ariseinnovation.link.utils;

import java.util.Objects;

public class ObjectUtils {
    public final static String toStringIfNullBlank(Object val) {
        if (Objects.isNull(val)) {
            return "";
        }
        return val.toString();
    }
}
