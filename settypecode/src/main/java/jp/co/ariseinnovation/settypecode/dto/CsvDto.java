package jp.co.ariseinnovation.settypecode.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CsvDto {
    /** 金融機関名 */
    private String bankName;
    /** 金融機関コード */
    private String bankCode;
    /** 割引手形 */
    private long notesReceivable;
    /** 短期借入金 */
    private long longLoanPayable;
    /** 長期借入金 */
    private long shortLoanPayable;
    /** 流動性預金 */
    private long flowDepositsSavings;
    /** 定期性預金 */
    private long fixDepositsSavings;
}
