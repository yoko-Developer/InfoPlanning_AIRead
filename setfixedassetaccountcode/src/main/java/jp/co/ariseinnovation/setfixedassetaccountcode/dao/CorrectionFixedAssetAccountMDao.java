package jp.co.ariseinnovation.setfixedassetaccountcode.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.co.ariseinnovation.setfixedassetaccountcode.entity.CorrectionFixedAssetAccountMEntity;

/**
 * 4.2.5 固定資産科補正マスタのDAO
 */
@Repository
public interface CorrectionFixedAssetAccountMDao extends JpaRepository<CorrectionFixedAssetAccountMEntity, String> {
    // @Query("select o from FixedAssetAccountMEntity o where o.formTypeId = :formTypeId")
    // List<FixedAssetAccountMEntity> searchByFormTypeId(String formTypeId);
}
