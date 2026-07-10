package jp.co.ariseinnovation.settypecode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jp.co.ariseinnovation.settypecode.service.CsvCreateService;

@Component
public class AppRunner implements ApplicationRunner {
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(ApplicationRunner.class);
    @Autowired
    private CsvCreateService csvCreateService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var runArgs = args.getNonOptionArgs();

        if (runArgs.isEmpty() || runArgs.size() != 1) {
            throw new Exception();
        }

        var inputFilePath = runArgs.get(0) + "\\csv_AIRead";

        exportCsv(inputFilePath);
    }

    private void exportCsv(String inputFilePath) throws FileNotFoundException, IOException {
        // 入力ディレクトリ
        File csvDir = new File(inputFilePath);

        // .csvでファイルを抽出
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv");
            }
        };

        // CSVファイルの一覧を取得
        File[] csvList = csvDir.listFiles(filter);

        String bottomRowValue = null;

        for (File inputFile : csvList) {
            // 借入金判定
            try (
                Stream<String> lines = Files.lines(inputFile.toPath())) {
                if (lines.noneMatch(line -> line.contains("14_deptandinterestexpense_"))) {
                    // 借入金以外なので終了
                    continue;
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

            // 入力CSV
            Reader reader = new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8);

            // CSVファイルの読み込み
            @SuppressWarnings("deprecation")
            CSVParser csvParser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);
            
            // "Value"の列番号を取得
            Integer valueColIndex = csvParser.getHeaderMap().get("Value");
            if (Objects.isNull(valueColIndex)) {
                log.info("ヘッダ項目の形式が不正のため、処理を終了します");
                return;
            }
            // Valueの結合情報
            Map<String, List<String>> groupedValues = new LinkedHashMap<>(); 

            for (CSVRecord record : csvParser) {
                if (StringUtils.equals(record.get("ItemName"), "type_cd")) {
                    log.info("type_cd行追加済みのため、処理を終了します。");
                    return;
                }
                String grupId = record.get("GrupID");

                if (StringUtils.equals(grupId, "-1")) {
                    continue;
                }

                String page = record.get("Page");
                String gId = record.get("GID");

                // page、gId, grupIdを結合
                String checkKeys = page + "|" + gId + "|" + grupId;
                // page、gId, grupIdごとのValueをまとめる
                groupedValues
                    .computeIfAbsent(checkKeys, k -> new ArrayList<>())
                    .add(record.get("Value"));
            }

            
            List<List<String>> typeCodeRecords = csvCreateService.createRecord(groupedValues, valueColIndex, bottomRowValue);

            // 最終行のValueを取得
            bottomRowValue = typeCodeRecords.get(typeCodeRecords.size() - 1).get(valueColIndex);
            var bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inputFile, true), "UTF-8"));
            try {
                for (List<String> typeCodeRecord : typeCodeRecords) {
                    for (int j = 0; j < typeCodeRecord.size(); j++) {
                        bw.write("\"" + typeCodeRecord.get(j) + "\"");
                        if (j != typeCodeRecord.size() - 1) {
                            bw.write(",");
                        }
                    }
                    bw.newLine();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                // Files.delete(Paths.get(inputFilePath));
            } finally {
                // ファイルクローズ
                bw.close();
            }
        }
    }


    static {
    }
}
