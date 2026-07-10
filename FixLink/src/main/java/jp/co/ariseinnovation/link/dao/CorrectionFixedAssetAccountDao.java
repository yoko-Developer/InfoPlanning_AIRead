package jp.co.ariseinnovation.link.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.ariseinnovation.link.entity.CorrectionFixedAssetAccountEntity;

/**
 * 固定資産科目補正マスタのDAO
 */
public interface CorrectionFixedAssetAccountDao extends JpaRepository<CorrectionFixedAssetAccountEntity, Integer> {
}
