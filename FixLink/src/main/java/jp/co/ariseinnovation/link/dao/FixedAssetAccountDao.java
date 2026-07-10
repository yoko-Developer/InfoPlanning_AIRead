package jp.co.ariseinnovation.link.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.ariseinnovation.link.entity.FixedAssetAccountEntity;

/**
 * 固定資産科目マスタのDAO
 */
public interface FixedAssetAccountDao extends JpaRepository<FixedAssetAccountEntity, String> {
    public Optional<FixedAssetAccountEntity> findByFixedAssetAccountName(String fixedAssetAccountName);
}
