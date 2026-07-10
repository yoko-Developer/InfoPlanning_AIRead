package jp.co.ariseinnovation.link.entity;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * キークラス
 */
@Setter
@Getter
public class OcrKey implements Serializable {
   /*
    * OCR結果ID
    */
   private String ocrResultId;
   /**
    * ページ番号
    */
   private int pageNo;
   /**
    * ID
    */
   private int id;
}
