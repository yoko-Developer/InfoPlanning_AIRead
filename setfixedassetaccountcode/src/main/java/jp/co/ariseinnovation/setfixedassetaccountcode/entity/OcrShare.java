package jp.co.ariseinnovation.setfixedassetaccountcode.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class OcrShare {
    protected OcrShare() {
    }

    /**
     * 登録時刻
     */
    @Column(name = "insertdatetime")
    private LocalDateTime insertDatetime;
    /**
     * 更新時刻
     */
    @Column(name = "updatedatetime")
    private LocalDateTime updateDatetime;
}
