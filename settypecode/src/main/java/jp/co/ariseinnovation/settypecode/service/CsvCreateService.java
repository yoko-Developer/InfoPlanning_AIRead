package jp.co.ariseinnovation.settypecode.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jp.co.ariseinnovation.settypecode.consts.CommonConsts;
import jp.co.ariseinnovation.settypecode.dao.CorrectionTypeMDao;
import jp.co.ariseinnovation.settypecode.entity.CorrectionTypeMEntity;

@Service
public class CsvCreateService {
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(CsvCreateService.class);
    private final static String FORM_TYPE_LOAN_PAYABLE = "02110";

    private Integer valurColIndex;

    @Autowired
    private CorrectionTypeMDao correctionTypeMDao;

    public List<List<String>> createRecord(Map<String, List<String>> groupedValues, Integer intdex, String bottomRowValue) throws IOException {

        this.valurColIndex = intdex;

        List<List<String>> typeRecords = new ArrayList<>();

        // 借入金
        List<CorrectionTypeMEntity> correctionTypeMList = correctionTypeMDao.searchByFormTypeId(FORM_TYPE_LOAN_PAYABLE);

        // 事前チェック：長期・短期のワードを含む行が存在するか確認（合計行も含む）
        boolean hasAnyMatch = groupedValues.entrySet().stream()
            .anyMatch(entry -> {
                String joinValue = String.join("", entry.getValue());
                return getMatchTypeMInfo(correctionTypeMList, joinValue).isPresent();
            });
        
        // 長期・短期のワードが見つからない場合、信頼値を空文字にする
        String confidenceValue = hasAnyMatch ? "100" : "";
        log.info("長期・短期のワードの存在チェック結果: " + hasAnyMatch + ", 信頼値: " + confidenceValue);

        // 直前行の長期短期判定用
        String typeCodeBefore = bottomRowValue;
        // typeCodeBeforeがmatchTypeMInfo.isPresent()で設定されたかどうかのフラグ
        boolean typeCodeBeforeFromMatch = false;

        // 一括更新対象inxexリスト
        List<Integer> bulkUpdateIndexList = new ArrayList<>();

        // 最初の合計行が出現したかどうかのフラグ
        boolean firstTotalRowAppeared = false;
        // 最初の合計行より前に長期・短期のワードが見つかったかどうかのフラグ
        boolean foundMatchBeforeFirstTotal = false;
        // 先頭行が通常データ（合計行でも長期・短期ワードでもない）かどうか
        Boolean firstRowIsNormalData = null; // null=未判定、true=通常データ、false=合計行または長期短期ワード

        int rowNo = 0;

        for (Map.Entry<String, List<String>> entry : groupedValues.entrySet()) {
            // key情報
            String key = entry.getKey();
            // key情報を分割
            List<String> keyList = Arrays.asList(key.split("\\|"));

            // Value情報
            List<String> valueList = entry.getValue();
            // Valueを結合
            String joinValue = String.join("", valueList);

            // 合計行判定
            boolean isTotalRow = false;
            for (String value : valueList) {
                isTotalRow = checkTotalRow(value);
                if (isTotalRow) {
                    break;
                }
            }
            if (isTotalRow) {
                // 先頭行の判定（合計行）
                if (firstRowIsNormalData == null) {
                    firstRowIsNormalData = false;
                    log.info("先頭行は合計行です。信頼値は100を使用します。");
                }
                
                // 最初の合計行が出現
                if (!firstTotalRowAppeared) {
                    firstTotalRowAppeared = true;
                    log.info("最初の合計行が出現しました。行番号: " + rowNo);
                    
                    // 先頭行が通常データで、最初の合計行より前に長期・短期のワードが見つかっている場合、
                    // それまでの全レコードの信頼値を""に更新
                    if (firstRowIsNormalData && foundMatchBeforeFirstTotal) {
                        log.info("先頭行が通常データで、最初の合計行前に長期・短期ワードが見つかったため、それまでの信頼値を空文字に更新");
                        for (int i = 0; i < typeRecords.size(); i++) {
                            List<String> record = typeRecords.get(i);
                            // 信頼値の位置（keyListのサイズ + type_cd + 信頼値 = keyListのサイズ + 2）
                            int confidenceIndex = record.size() - 7; // 最後から7番目が信頼値の位置
                            record.set(confidenceIndex, "");
                        }
                    }
                }
                
                // 長期・短期のワードを含むか確認
                Optional<CorrectionTypeMEntity> matchTypeMInfo = getMatchTypeMInfo(correctionTypeMList, joinValue);
                if (matchTypeMInfo.isPresent() && !bulkUpdateIndexList.isEmpty()) {
                    // 長期・短期のワードを含み、一括更新対象が存在する場合、合計行から取得される種類コードで対象行を一括更新
                    typeRecords = bulkUpdateValue(typeRecords, bulkUpdateIndexList, matchTypeMInfo);
                    bulkUpdateIndexList.clear();
                }
                // rowNo++;
                continue;
            }

            // csv追加レコード情報
            List<String> typeRecord= new ArrayList<>();

            typeRecord.add("type_cd");
            typeRecord.addAll(keyList);
            log.info("joinValue : " + joinValue);
            Optional<CorrectionTypeMEntity> matchTypeMInfo = getMatchTypeMInfo(correctionTypeMList, joinValue);
            if (matchTypeMInfo.isPresent()) {
            log.info("matchTypeMInfo.isPresent()");
            log.info("matchTypeMInfo.get().getTypeCd() : " + matchTypeMInfo.get().getTypeCd());
                // 先頭行の判定（長期・短期のワードを含む行）
                if (firstRowIsNormalData == null) {
                    firstRowIsNormalData = false;
                    log.info("先頭行は長期・短期のワードを含む行です。信頼値は100を使用します。");
                }
                
                // 最初の合計行が出現する前に長期・短期のワードが見つかった
                if (!firstTotalRowAppeared) {
                    foundMatchBeforeFirstTotal = true;
                    log.info("最初の合計行前に長期・短期のワードが見つかりました。行番号: " + rowNo);
                }
                typeRecord.add(matchTypeMInfo.get().getTypeCd());
                typeRecord.add(confidenceValue);
                typeCodeBefore = matchTypeMInfo.get().getTypeCd();
            } else if (StringUtils.isNotEmpty(typeCodeBefore)) {
            log.info("tringUtils.isNotEmpty(typeCodeBefore)");
            log.info("mtypeCodeBefore : " + typeCodeBefore);
                // 先頭行の判定（typeCodeBeforeを使用する通常データ）
                if (firstRowIsNormalData == null) {
                    firstRowIsNormalData = true;
                    log.info("先頭行は通常データ（typeCodeBeforeを使用）です。");
                }
                typeRecord.add(typeCodeBefore);
                typeRecord.add(confidenceValue);
            } else {
            log.info("else");
                // 先頭行の判定（typeCodeBeforeがない通常データ）
                if (firstRowIsNormalData == null) {
                    firstRowIsNormalData = true;
                    log.info("先頭行は通常データ（typeCodeBeforeなし）です。");
                }
                // 一括更新対象リストに行番号を追加
                bulkUpdateIndexList.add(rowNo);
                // 原則として長期に分類
                typeRecord.add(CommonConsts.TYPE_CODE_LONG);
                typeRecord.add("");
            }

            typeRecord.add("");
            typeRecord.add("0");
            typeRecord.add("0");
            typeRecord.add("0");
            typeRecord.add("0");

            typeRecords.add(typeRecord);

            rowNo++;
        }

        return typeRecords;
    }

    private boolean checkTotalRow(String joinValue) {
        boolean checkEqualVal = CommonConsts.equals(joinValue);
        boolean checkContainVal = CommonConsts.isContain(joinValue);
        boolean checkSuffixVal = joinValue.endsWith(CommonConsts.SUFFIX_CHECK);
        boolean checkNotContainVal = CommonConsts.isNotContain(joinValue);

        return checkEqualVal
            || (checkContainVal || checkSuffixVal)
            && checkNotContainVal;
    }

    private Optional<CorrectionTypeMEntity> getMatchTypeMInfo(List<CorrectionTypeMEntity> correctionTypeMList, String checkValue) {
        Optional<CorrectionTypeMEntity> matchTypeMInfo = correctionTypeMList.stream()
                                        .filter(correctionTypeM -> {
                                            String regexPattern = correctionTypeM.getCorrectionString();
                                            return Pattern.matches(regexPattern, checkValue);
                                        })
                                        .findFirst();
        return matchTypeMInfo;
    }

    private List<List<String>> bulkUpdateValue(List<List<String>> typeRecords, List<Integer> bulkUpdateIndexList, Optional<CorrectionTypeMEntity> matchTypeMInfo) {
        for (Integer index : bulkUpdateIndexList) {
            typeRecords.get(index).set(valurColIndex, matchTypeMInfo.get().getTypeCd());
        }
        return typeRecords;
    }
}
