package jp.co.ariseinnovation.loan_detail_merge_batch.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

@Service
public class LoanDetailMergeService {

  private static final String DEFAULT_DEBUG_ZIP = "result.zip";

  /**
   * @param processId   %PROCESSID%
   * @param processDate %PROCESSDATE%
   * @param debugZip    trueならデバッグ用ZIPを出力
   */
  public void run(String processId, String processDate, boolean debugZip) {
    Path baseDir = Paths.get("C:/AIRead_ETL/ocrtemp", processId + "_" + processDate);
    Path inputDir = baseDir.resolve("csv_AIRead");
    Path backupDir = baseDir.resolve("csv_AIRead_bak");

    try {
      // バックアップフォルダ作成
      if (!Files.exists(backupDir)) {
        Files.createDirectories(backupDir);
      }

      // csv_AIRead配下のCSVを処理
      try (var paths = Files.list(inputDir)) {
        paths.filter(p -> p.toString().endsWith(".csv"))
            .forEach(csvFile -> {
              try {
                // --- オリジナルをバックアップにコピー ---
                Path backupFile = backupDir.resolve(csvFile.getFileName());
                Files.copy(csvFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

                // --- CSV処理 ---
                processCsv(csvFile, debugZip);

              } catch (Exception e) {
                throw new RuntimeException("Failed processing file: " + csvFile, e);
              }
            });
      }

    } catch (IOException e) {
      throw new RuntimeException("Batch failed", e);
    }
  }

  private void processCsv(Path csvFile, boolean debugZip) throws Exception {
    // 1) 読み込み（縦持ち）
    List<String[]> tallInput = CsvUtils.readAll(csvFile.toString());

    // --- formidチェック ---
    boolean isTarget = tallInput.stream()
        .anyMatch(row -> row.length > 4
            && "formid".equals(row[0])
            && "14_deptandinterestexpense_0".equals(row[4]));

    if (!isTarget) {
      // 対象外 → 入力をそのまま上書き
      CsvUtils.writeAll(csvFile.toString(), tallInput);
      System.out.printf("Skipped processing. file=%s%n", csvFile);
      return;
    }

    // 2) フラグ判定（縦持ち入力に対して）
    boolean hasNameMinusOne = tallInput.stream()
        .anyMatch(row -> row.length > 3
            && "name".equals(row[0])
            && "-1".equals(row[3]));   // GrupID列が -1
    boolean hasAddressMinusOne = tallInput.stream()
        .anyMatch(row -> row.length > 3
            && "address".equals(row[0])
            && "-1".equals(row[3]));

    // 3) GrupID=-1 の行を保持するための入れ物
    List<String[]> minusOneRows = new ArrayList<>();

    // 4) 縦→横変換
    List<Map<String, String>> wide = RecordTransformer.toWide(tallInput, minusOneRows);

    // 5) マージ（フラグを渡す）
    List<Map<String, String>> merged = RecordMerger.mergeRecords(wide, hasNameMinusOne, hasAddressMinusOne);

    // 6) 横→縦に戻す
    List<String[]> tallOutput = RecordTransformer.toTall(merged, minusOneRows);

    // 7) 並び替え
    List<String[]> tallSorted = RecordTransformer.reorderByItemName(tallOutput);

    // 8) 出力（同じファイル名で上書き）
    CsvUtils.writeAll(csvFile.toString(), tallSorted);

    // 9) デバッグZIP（任意）
    if (debugZip) {
      try (var zos = new ZipOutputStream(Files.newOutputStream(Paths.get(DEFAULT_DEBUG_ZIP)))) {
        CsvUtils.putCsv(zos, "01_input.csv", tallInput);
        CsvUtils.putCsv(zos, "02_wide.csv", CsvUtils.mapToRows(wide));
        CsvUtils.putCsv(zos, "03_merged.csv", CsvUtils.mapToRows(merged));
        CsvUtils.putCsv(zos, "04_output.csv", tallOutput);
        CsvUtils.putCsv(zos, "05_sorted.csv", tallSorted);
        CsvUtils.putCsv(zos, "06_minusOne.csv", minusOneRows);
      }
    }

    System.out.printf("Done. file=%s, debugZip=%s%n", csvFile, debugZip);
  }
}
