package jp.co.ariseinnovation.link.service;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jp.co.ariseinnovation.link.dao.CorrectionDepreciationMethodDao;
import jp.co.ariseinnovation.link.dao.CorrectionFixedAssetAccountDao;
import jp.co.ariseinnovation.link.dao.DepreciationDao;
import jp.co.ariseinnovation.link.dao.FixedAssetAccountDao;
import jp.co.ariseinnovation.link.dao.FixedAssetRegisterDao;

@Service
public class FixLinkService {

    private final static Logger log = LoggerFactory.getLogger(FixLinkService.class);
    @Autowired
    private CorrectionFixedAssetAccountDao correctionFixedAssetAccountDao;
    @Autowired
    private FixedAssetAccountDao fixedAssetAccountDao;
    @Autowired
    private CorrectionDepreciationMethodDao correctionDepreciationMethodDao;
    @Autowired
    private DepreciationDao depreciationDao;
    @Autowired
    private FixedAssetRegisterDao fixedAssetRegisterDao;

    private final static boolean matchs(String val, String regex) {
        if (Objects.isNull(val)) {
            return false;
        }
        return val.matches(regex);
    }

    @Transactional
    public void typeLink(String ocrResultId) throws Exception {
        log.info("type link start");
        // var correctionFixedAssetAccounts = correctionFixedAssetAccountDao.findAll();
        var correctionDepreciationMethods = correctionDepreciationMethodDao.findAll();

        var datas = fixedAssetRegisterDao.findByOcrResultId(ocrResultId);
        for (var data : datas) {
            /*
            var nm = data.getFixedAssetAccountName();
            var caa = correctionFixedAssetAccounts.stream()
                    .filter(i -> matchs(data.getFixedAssetAccountName(), i.getBeforeCorrectionString()))
                    .findFirst();

            if (caa.isPresent()) {
                nm = caa.get().getAfterCorrectionString();
            }
            var aan = fixedAssetAccountDao.findByFixedAssetAccountName(nm);
            if (aan.isPresent()) {
                data.setFixedAssetAccountCode(aan.get().getFixedAssetAccountCode());
            }
            */

            var nm = data.getDepreciationMethod();
            var cdm = correctionDepreciationMethods.stream()
                    .filter(i -> matchs(data.getDepreciationMethod(), i.getBeforeCorrectionString()))
                    .findFirst();

            if (cdm.isPresent()) {
                nm = cdm.get().getAfterCorrectionString();
            }
            var adm = depreciationDao.findByDepreciationName(nm);
            if (adm.isPresent()) {
                data.setDepreciationCode(adm.get().getDepreciationCode());
            }
            fixedAssetRegisterDao.save(data);
        }
        log.info("type link end");
    }
}
