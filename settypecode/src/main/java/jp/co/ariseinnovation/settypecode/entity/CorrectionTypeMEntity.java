package jp.co.ariseinnovation.settypecode.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 補正種類マスタ
 */
@Setter
@Getter
@Entity
@Table(name = "correction_type_m")
@ToString
public class CorrectionTypeMEntity extends OcrShare implements Serializable {
   /** 補正種類ID */
   @Id
   @Column(name = "correction_type_id")
   private Short correctionTypeId;

   /** 変動文字列 */
   @Column(name = "fluctuation_string", nullable = false, length = 128)
   private String fluctuationString;

   /** 補正文字列 */
   @Column(name = "correction_string", length = 128)
   private String correctionString;

   /** 帳票分類ID */
   @Column(name = "form_type_id", nullable = false, length = 5)
   private String formTypeId;

   /** 種類コード */
   @Column(name = "type_cd", length = 2)
   private String typeCd;

   /** 更新ユーザー */
   @Column(name = "updateuser", length = 32)
   private String updateuser;
}
