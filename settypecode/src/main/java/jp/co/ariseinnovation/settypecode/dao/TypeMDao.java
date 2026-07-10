package jp.co.ariseinnovation.settypecode.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.co.ariseinnovation.settypecode.entity.TypeMEntity;

/**
 * 4.1.6 種類マスタのDAO
 */
@Repository
public interface TypeMDao extends JpaRepository<TypeMEntity, String> {
    @Query("select o from TypeMEntity o where o.formTypeId = :formTypeId")
    List<TypeMEntity> searchByFormTypeId(String formTypeId);
}
