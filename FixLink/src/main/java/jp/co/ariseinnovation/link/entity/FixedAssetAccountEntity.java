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
@Table(name = "fixed_asset_account_m")
@ToString
public class FixedAssetAccountEntity extends OcrShare implements Serializable {
    /** 固定資産科目名称 */
    private String fixedAssetAccountName;
    /** 固定資産科目コード */
    @Id
    private String fixedAssetAccountCode;
}
