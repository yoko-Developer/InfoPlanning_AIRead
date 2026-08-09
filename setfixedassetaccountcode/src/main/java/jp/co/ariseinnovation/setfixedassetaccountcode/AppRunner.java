package jp.co.ariseinnovation.setfixedassetaccountcode;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
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

import jp.co.ariseinnovation.setfixedassetaccountcode.service.CsvCreateService;

@Component
public class AppRunner implements ApplicationRunner {
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(ApplicationRunner.class);
    @Autowired
    private CsvCreateService csvCreateService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var runArgs = args.getNonOptionArgs();

        // 引数が2つ（入力パス、出力パス）でない場合はエラーにする
        if (runArgs.isEmpty() || runArgs.size() != 2) {
            log.error("起動引数の指定が不正です。「入力ディレクトリのパス」と「出力ディレクトリのパス」の2つを指定してください。");
            throw new IllegalArgumentException("Invalid application arguments. Requires 2 arguments: inputPath and outputPath.");
        }

        var inputDir = runArgs.get(0);
        var outputDir = runArgs.get(1);

        exportCsv(inputDir, outputDir);
    }

    private void exportCsv(String inputDirPath, String outputDirPath) throws FileNotFoundException, IOException {
        // 入力ディレクトリのパス作成（\\csv_AIRead を結合）
        Path inputFilePath = Paths.get(inputDirPath, "csv_AIRead");
        File csvDir = inputFilePath.toFile();

        // ディレクトリが存在しない場合のガード処理
        if (!csvDir.exists() || !csvDir.isDirectory()) {
            log.error("指定された入力ディレクトリが存在しません: " + csvDir.getAbsolutePath());
            return;
        }

        // 出力ディレクトリが存在しない場合は作成しておく
        File outDir = new File(outputDirPath);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        // .csvでファイルを抽出
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv");
            }
        };

        // CSVファイルの一覧を取得
        File[] csvList = csvDir.listFiles(filter);
        if (csvList == null) {
            return;
        }

        String bottomRowValue = null;
        for (File inputFile : csvList) {
            // 固定資産台帳判定
            try (
                    Stream<String> lines = Files.lines(inputFile.toPath())) {
                if (lines.noneMatch(line -> line.contains("05_010_00"))) {
                    // 固定資産台帳以外なので終了
                    continue;
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

            // 入力CSVの読み込み
            Reader reader = new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8);

            @SuppressWarnings("deprecation")
            CSVParser csvParser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            // "Value"の列番号を取得
            Integer valueColIndex = csvParser.getHeaderMap().get("Value");
            if (Objects.isNull(valueColIndex)) {
                log.info("ヘッダ項目の形式が不正のため、処理を終了します");
                reader.close();
                return;
            }
            // Valueの結合情報
            Map<String, List<String>> groupedValues = new LinkedHashMap<>();

            // 金額情報
            Map<String, String> balanceValues = new LinkedHashMap<>();

            for (CSVRecord record : csvParser) {
                if (StringUtils.equals(record.get("ItemName"), "fixedAssetAccountCode")) {
                    log.info("fixedAssetAccountCode行追加済みのため、処理を終了します。");
                    reader.close();
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
                // page、gId, grupIdごとのValueをまとめる(6件まで)
                if (groupedValues.computeIfAbsent(checkKeys, k -> new ArrayList<>()).size() <= 6) {
                    groupedValues
                            .computeIfAbsent(checkKeys, k -> new ArrayList<>())
                            .add(record.get("Value"));
                }
                // 金額情報をまとめる
                if (StringUtils.equals(record.get("ItemName"), "取得価額")) {
                    balanceValues.put(checkKeys, record.get("Value"));
                }
            }
            reader.close(); // リーダーはちゃんと閉じる

            // レコード生成処理
            List<List<String>> fixedAssetAccountCodeRecords = csvCreateService.createRecord(groupedValues, balanceValues, valueColIndex, bottomRowValue);

            // 最終行のValueを取得
            if (!fixedAssetAccountCodeRecords.isEmpty()) {
                bottomRowValue = fixedAssetAccountCodeRecords.get(fixedAssetAccountCodeRecords.size() - 1).get(valueColIndex);
            }

            // 出力先のファイルを決定（出力ディレクトリの中に同名ファイルを作成する等）
            File outputFile = new File(outDir, inputFile.getName());

            // 入力ファイルをコピーするか、新規に出力先へ書き込む形にする
            // ここでは安全に出力先ディレクトリへ新規作成・書き込みを行うよ
            Files.copy(inputFile.toPath(), outputFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            var bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, true), "UTF-8"));
            try {
                for (List<String> fixedAssetAccountCodeRecord : fixedAssetAccountCodeRecords) {
                    for (int j = 0; j < fixedAssetAccountCodeRecord.size(); j++) {
                        bw.write("\"" + fixedAssetAccountCodeRecord.get(j) + "\"");
                        if (j != fixedAssetAccountCodeRecord.size() - 1) {
                            bw.write(",");
                        }
                    }
                    bw.newLine();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                bw.close();
            }
        }
    }
}
