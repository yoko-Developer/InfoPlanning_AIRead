package jp.co.ariseinnovation.setfixedassetaccountcode.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 4.1.9 固定資産科目マスタ
 */
@Setter
@Getter
@Entity
@Table(name = "fixed_asset_account_m")
@ToString
public class FixedAssetAccountMEntity extends OcrShare implements Serializable {
   /** 固定資産科目コード */
   @Id
   private Integer fixedAssetAccountCode;

   /** 固定資産科目名称 */
   private String fixedAssetAccountName;
   /** 固定資産科目表示名称 */
   private String fixedAssetAccountNameView;
}
