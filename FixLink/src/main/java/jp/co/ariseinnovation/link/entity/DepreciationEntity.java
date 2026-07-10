package jp.co.ariseinnovation.link.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 固定資産科目マスタ
 */
@Setter
@Getter
@Entity
@Table(name = "depreciation_m")
@ToString
public class DepreciationEntity extends OcrShare implements Serializable {
    /** 償却方法名称 */
    private String depreciationName;
    /** 償却方法コード */
    @Id
    private String depreciationCode;
    /** 種類ID */
}
