package jp.co.ariseinnovation.link.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.ariseinnovation.link.entity.FixedAssetRegisterEntity;
import jp.co.ariseinnovation.link.entity.OcrKey;

/**
 * 固定資産台帳のDAO
 */
public interface FixedAssetRegisterDao extends JpaRepository<FixedAssetRegisterEntity, OcrKey> {
    List<FixedAssetRegisterEntity> findByOcrResultId(String ocrResultId);
}
