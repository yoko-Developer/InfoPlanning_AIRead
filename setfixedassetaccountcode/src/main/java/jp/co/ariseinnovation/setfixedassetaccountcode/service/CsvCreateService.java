package jp.co.ariseinnovation.setfixedassetaccountcode.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jp.co.ariseinnovation.setfixedassetaccountcode.consts.CommonConsts;
import jp.co.ariseinnovation.setfixedassetaccountcode.dao.CorrectionFixedAssetAccountMDao;
import jp.co.ariseinnovation.setfixedassetaccountcode.dao.FixedAssetAccountMDao;
import jp.co.ariseinnovation.setfixedassetaccountcode.dao.TypeMDao;
import jp.co.ariseinnovation.setfixedassetaccountcode.entity.FixedAssetAccountMEntity;
import jp.co.ariseinnovation.setfixedassetaccountcode.entity.TypeMEntity;

@Service
public class CsvCreateService {
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(CsvCreateService.class);
    private final static String FORM_TYPE_LOAN_PAYABLE = "02110";

    private Integer valueColIndex;

    @Autowired
    private TypeMDao typeMDao;

    @Autowired
    private CorrectionFixedAssetAccountMDao correctionFixedAssetAccountMDao;

    @Autowired
    private FixedAssetAccountMDao fixedAssetAccountMDao;

    public List<List<String>> createRecord(Map<String, List<String>> groupedValues, Map<String, String> balanceValues, Integer intdex, String bottomRowValue) throws IOException {

        this.valueColIndex = intdex;

        List<List<String>> fixedAssetAccountMRecords = new ArrayList<>();

        // 借入金
        List<TypeMEntity> TypeMList = typeMDao.searchByFormTypeId(FORM_TYPE_LOAN_PAYABLE);

        // 見出し行の科目コード
        String fixedAssetAccountCodeHead = null;
        // 前行の科目コード
        String fixedAssetAccountCodeBefore = bottomRowValue;

        // 一括更新対象inxexリスト
        List<Integer> bulkUpdateIndexList = new ArrayList<>();

        int rowNo = 0;

        for (Map.Entry<String, List<String>> entry : groupedValues.entrySet()) {
            // key情報
            String key = entry.getKey();
            // key情報を分割
            List<String> keyList = Arrays.asList(key.split("\\|"));

            // Value情報
            List<String> valueList = entry.getValue();
            // 合計行判定
            boolean isTotalRow = false;
            for (String value : valueList) {
                isTotalRow = checkTotalRow(value);
                if (isTotalRow) {
                    break;
                }
            }
            // Value補正
            valueList = correctValue(valueList);
            // Valueを結合
            String joinValue = String.join("", valueList);
            if (StringUtils.isEmpty(joinValue)) {
                fixedAssetAccountCodeBefore = null;
                continue;
            }
            // 結合Valueから取得される固定資産科目マスタ情報
            List<FixedAssetAccountMEntity> fixedAssetAccountMList = fixedAssetAccountMDao.searchByFixedAssetAccountName(joinValue);


            log.info("rowNo : " + rowNo);
            log.info("key : " + key);
            log.info("joinValue : " + joinValue);
            log.info("isTotalRow : " + isTotalRow);
            log.info("balanceValues.get(key)) : " + balanceValues.get(key));
            // 結合Valueが固定資産科目マスタの科目名を含むかを判定
            Optional<FixedAssetAccountMEntity> matchFixedAssetAccountMInfo = getMatchFixedAssetAccountMInfo(fixedAssetAccountMList, joinValue);
            if (matchFixedAssetAccountMInfo.isPresent()) {
                // 科目名を含む
                // 金額有無判定
                if (StringUtils.isEmpty(balanceValues.get(key))) {
                    log.info("見出し科目");
                    // 金額を持たない場合、見出し科目
                    // 見出し科目の科目コードを保持し、次の行へ
                    fixedAssetAccountCodeHead = String.valueOf(matchFixedAssetAccountMInfo.get().getFixedAssetAccountCode());
                    continue;
                } else {
                    // 金額あり
                    if (isTotalRow) {
                        // 合計行
                        // 一括更新対象の確認
                        if (bulkUpdateIndexList.isEmpty()) {
                            log.info("合計行・一括更新対象無し");
                            // 一括更新対象が存在しなければ、前行情報に科目コードを格納し次の行へ
                            fixedAssetAccountCodeBefore = String.valueOf(matchFixedAssetAccountMInfo.get().getFixedAssetAccountCode());
                        } else {
                            log.info("合計行・一括更新対象あり");
                            // 一括更新対象が存在した場合、合計行の科目コードで対象行を一括更新
                            bulkUpdateValue(fixedAssetAccountMRecords, bulkUpdateIndexList, matchFixedAssetAccountMInfo);
                            // 一括更新対象のクリア
                            bulkUpdateIndexList.clear();
                            // // TODO:見出し項目のクリア
                            // // 前行情報に科目コード情報を格納
                            // fixedAssetAccountCodeBefore = String.valueOf(matchFixedAssetAccountMInfo.get().getFixedAssetAccountCode());
                        }

                        // 見出し科目情報をクリア
                        fixedAssetAccountCodeHead = null;
                    } else {
                        log.info("科目名を含む内訳行");
                        // 科目名を含む内訳行
                        // 科目コード情報で行を追加
                        fixedAssetAccountMRecords = addRow(fixedAssetAccountMRecords, keyList, String.valueOf(matchFixedAssetAccountMInfo.get().getFixedAssetAccountCode()), "100");
                        // 前行情報に科目コード情報を格納
                        fixedAssetAccountCodeBefore = String.valueOf(matchFixedAssetAccountMInfo.get().getFixedAssetAccountCode());
                    }
                }
            } else {
                // 科目名を含まない場合、見出し情報の有無を判定
                if (StringUtils.isEmpty(fixedAssetAccountCodeHead)) {
                    // 見出し情報なし
                    // 前行情報の有無を確認
                    if (StringUtils.isEmpty(fixedAssetAccountCodeBefore)) {
                        // 合計行判定
                        if (!isTotalRow) {
                            log.info("科目名無し、見出し情報なし、前行情報なし、合計行でない");
                            // 科目名無し、見出し情報なし、前行情報なし、合計行でない場合、一括更新対象リストに行番号を追加
                            bulkUpdateIndexList.add(rowNo);
                            // 科目コードは空の状態で行追加
                            fixedAssetAccountMRecords = addRow(fixedAssetAccountMRecords, keyList, "", "");
                        }
                    } else {
                        // 合計行判定
                        if (!isTotalRow) {
                            log.info("科目名無し、見出し情報なし、前行情報あり");
                            // 前行情報がある場合、前行情報で行追加
                            fixedAssetAccountMRecords = addRow(fixedAssetAccountMRecords, keyList, fixedAssetAccountCodeBefore, "");
                        }
                    }

                } else {
                    log.info("科目名無し、見出し情報あり");
                    // 見出し情報が存在する場合
                    if (!isTotalRow) {
                        // 合計行でない場合、見出し情報で行追加
                        fixedAssetAccountMRecords = addRow(fixedAssetAccountMRecords, keyList, fixedAssetAccountCodeHead, "");
                        // 前行情報に見出し情報を代入
                        fixedAssetAccountCodeBefore = fixedAssetAccountCodeHead;
                    } else {
                        fixedAssetAccountCodeHead = null;
                    }
                }
            }
            rowNo++;
        }

        return fixedAssetAccountMRecords;
    }

    // 合計行判定
    private boolean checkTotalRow(String joinValue) {
        boolean checkEqualVal = CommonConsts.equals(joinValue);
        boolean checkContainVal = CommonConsts.isContain(joinValue);
        boolean checkSuffixVal = joinValue.endsWith(CommonConsts.SUFFIX_CHECK);
        // boolean checkNotContainVal = CommonConsts.isNotContain(joinValue);

        return checkEqualVal
                || checkContainVal || checkSuffixVal;
        // && checkNotContainVal;
    }

    // 対象Valueが固定資産科目マスタの科目名を含んでいるかを判定
    private Optional<FixedAssetAccountMEntity> getMatchFixedAssetAccountMInfo(List<FixedAssetAccountMEntity> fixedAssetAccountMList, String checkValue) {
        Optional<FixedAssetAccountMEntity> matchFixedAssetAccountM = fixedAssetAccountMList.stream()
                .filter(fixedAssetAccountM -> checkValue.contains(fixedAssetAccountM.getFixedAssetAccountName()))
                .findFirst();
        return matchFixedAssetAccountM;
    }

    // 行追加
    private List<List<String>> addRow(List<List<String>> fixedAssetAccountMRecords, List<String> keyList, String fixedAssetAccountCode, String conf){
        List<String> fixedAssetAccountMRecord= new ArrayList<>();
        fixedAssetAccountMRecord.add("fixedAssetAccountCode");
        fixedAssetAccountMRecord.addAll(keyList);
        fixedAssetAccountMRecord.add(fixedAssetAccountCode);
        fixedAssetAccountMRecord.add(conf);
        fixedAssetAccountMRecord.add("");
        fixedAssetAccountMRecord.add("0");
        fixedAssetAccountMRecord.add("0");
        fixedAssetAccountMRecord.add("0");
        fixedAssetAccountMRecord.add("0");

        fixedAssetAccountMRecords.add(fixedAssetAccountMRecord);

        return fixedAssetAccountMRecords;
    }


    private List<List<String>> bulkUpdateValue(List<List<String>> fixedAssetAccountMRecords, List<Integer> bulkUpdateIndexList, Optional<FixedAssetAccountMEntity> matchFixedAssetAccountMInfo) {
        for (Integer index : bulkUpdateIndexList) {
            fixedAssetAccountMRecords.get(index).set(valueColIndex, String.valueOf(matchFixedAssetAccountMInfo.get().getFixedAssetAccountCode()));
        }
        return fixedAssetAccountMRecords;
    }

    // 科目補正
    @Transactional
    public List<String> correctValue(List<String> valueList) {
        log.info("correct value start");
        var correctionFixedAssetAccounts = correctionFixedAssetAccountMDao.findAll();
        // var correctionDepreciationMethods = correctionDepreciationMethodDao.findAll();

        for (int i = 0; i < valueList.size(); i++) {
            String value = valueList.get(i);

            var nm = value;
            var caa = correctionFixedAssetAccounts.stream()
                    .filter(j -> matchs(value, j.getBeforeCorrectionString()))
                    .findFirst();

            if (caa.isPresent()) {
                nm = caa.get().getAfterCorrectionString();
            }
            valueList.set(i, nm);

            // nm = data.getDepreciationMethod();
            // var cdm = correctionDepreciationMethods.stream()
            //         .filter(i -> matchs(data.getDepreciationMethod(), i.getBeforeCorrectionString()))
            //         .findFirst();

            // if (cdm.isPresent()) {
            //     nm = cdm.get().getAfterCorrectionString();
            // }
        }
        log.info("correct value end");
        return valueList;
    }

    private final static boolean matchs(String val, String regex) {
        if (Objects.isNull(val)) {
            return false;
        }
        return val.matches(regex);
    }
}
