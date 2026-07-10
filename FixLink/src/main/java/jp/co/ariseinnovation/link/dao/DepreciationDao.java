package jp.co.ariseinnovation.link.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.ariseinnovation.link.entity.DepreciationEntity;

/**
 * 固定資産科目マスタのDAO
 */
public interface DepreciationDao extends JpaRepository<DepreciationEntity, String> {
    public Optional<DepreciationEntity> findByDepreciationName(String DepreciationName);
}
