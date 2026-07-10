package jp.co.ariseinnovation.link.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.ariseinnovation.link.entity.CorrectionDepreciationMethodEntity;

/**
 * 償却方法補正マスタのDAO
 */
public interface CorrectionDepreciationMethodDao extends JpaRepository<CorrectionDepreciationMethodEntity, Integer> {
}
