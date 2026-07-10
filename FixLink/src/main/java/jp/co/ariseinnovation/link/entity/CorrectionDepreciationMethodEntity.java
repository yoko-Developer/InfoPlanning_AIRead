package jp.co.ariseinnovation.link.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 償却方法補正マスタ
 */
@Setter
@Getter
@Entity
@Table(name = "correction_depreciation_method_m")
@ToString
public class CorrectionDepreciationMethodEntity extends OcrShare implements Serializable {
    /** 補正前償却資産科目 */
    private String beforeCorrectionString;
    /** 補正後償却資産科目 */
    private String afterCorrectionString;
    /** 種類ID */
    @Id
    private Integer correctionDepreciationMethodId;
}
