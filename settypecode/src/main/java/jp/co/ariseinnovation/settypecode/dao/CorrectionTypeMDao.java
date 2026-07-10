package jp.co.ariseinnovation.settypecode.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.co.ariseinnovation.settypecode.entity.CorrectionTypeMEntity;

/**
 * 補正種類マスタのDAO
 */
@Repository
public interface CorrectionTypeMDao extends JpaRepository<CorrectionTypeMEntity, Short> {
    /**
     * 帳票分類IDで補正種類マスタを検索
     * correction_type_idの昇順でソート
     * 
     * @param formTypeId 帳票分類ID
     * @return 該当する補正種類マスタのリスト（correction_type_id昇順）
     */
    @Query("select o from CorrectionTypeMEntity o where o.formTypeId = :formTypeId order by o.correctionTypeId asc")
    List<CorrectionTypeMEntity> searchByFormTypeId(String formTypeId);
}
