package jp.co.ariseinnovation.setfixedassetaccountcode.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.co.ariseinnovation.setfixedassetaccountcode.entity.FixedAssetAccountMEntity;

/**
 * 4.1.9 固定資産科目マスタのDAO
 */
@Repository
public interface FixedAssetAccountMDao extends JpaRepository<FixedAssetAccountMEntity, String> {

    // CONCAT関数を使って、PostgreSQLとSQL Serverの両方で動くようにした決定版クエリ！
    @Query(value = "select * from fixed_asset_account_m where :fixedAssetAccountName LIKE CONCAT('%', fixed_asset_account_name, '%')", nativeQuery = true)
    List<FixedAssetAccountMEntity> searchByFixedAssetAccountName(@Param("fixedAssetAccountName") String fixedAssetAccountName);
}
