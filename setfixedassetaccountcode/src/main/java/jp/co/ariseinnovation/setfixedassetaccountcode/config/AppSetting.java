package jp.co.ariseinnovation.setfixedassetaccountcode.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
@Getter
public class AppSetting {
    static AppSetting current;

    public static AppSetting current() {
        return current;
    }

    @Value("${csv.outPath}")
    private String csvOutPath;

}
