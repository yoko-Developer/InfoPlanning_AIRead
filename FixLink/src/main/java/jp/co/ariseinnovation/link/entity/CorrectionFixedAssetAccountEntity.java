package jp.co.ariseinnovation.link.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 固定資産科目補正マスタ
 */
@Setter
@Getter
@Entity
@Table(name = "correction_fixed_asset_account_m")
@ToString
public class CorrectionFixedAssetAccountEntity extends OcrShare implements Serializable {
    /** 補正前固定資産科目 */
    private String beforeCorrectionString;
    /** 補正後固定資産科目 */
    private String afterCorrectionString;
    /** 種類ID */
    @Id
    private Integer fixedAssetAccountId;
}
