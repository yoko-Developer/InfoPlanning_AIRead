package jp.co.ariseinnovation.settypecode.entity;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 4.1.6 種類マスタ
 */
@Setter
@Getter
@Entity
@Table(name = "type_m")
@ToString
public class TypeMEntity extends OcrShare implements Serializable {
   /** 種類ID */
   @Id
   private Short typeId;

   /** 帳票分類ID */
   private String formTypeId;
   /** 種類コード */
   private String typeCd;
   /** 種類名 */
   private String typeString;
   /** 種類紐付け名 */
   private String typeLinkName;
}
