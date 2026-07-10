package jp.co.ariseinnovation.link.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 固定資産台帳
 */
@Getter
@Setter
@Entity
@Table(name = "fixed_asset_register")
@IdClass(value = OcrKey.class)
@ToString
public class FixedAssetRegisterEntity extends OcrShare implements Serializable {
   /** OCR結果ID */
   @Id
   @Column(name = "ocr_result_id")
   private String ocrResultId;
   /** ページ番号 */
   @Id
   @Column(name = "page_no")
   private Integer pageNo;
   /** ID */
   @Id
   @Column(name = "id")
   private Integer id;
   /** 固定資産科目名称 */
   private String fixedAssetAccountName;
   /** 固定資産科目コード */
   private String fixedAssetAccountCode;
   /** 償却方法 */
   private String depreciationMethod;
   /** 償却方法コード */
   private String depreciationCode;
}