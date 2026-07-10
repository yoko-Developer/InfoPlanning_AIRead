package jp.co.ariseinnovation.setfixedassetaccountcode.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 4.2.5 固定資産科補正マスタ
 */
@Setter
@Getter
@Entity
@Table(name = "correction_fixed_asset_account_m")
@ToString
public class CorrectionFixedAssetAccountMEntity extends OcrShare implements Serializable {
   /** 固定資産科目補正ID */
   @Id
   private Integer fixedAssetAccountId;

   /** 補正前固定資産科目文字列 */
   private String beforeCorrectionString;
   /** 補正後固定資産科目文字列 */
   private String afterCorrectionString;
}
