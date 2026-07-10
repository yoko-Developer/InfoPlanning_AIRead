package jp.co.ariseinnovation.loan_detail_merge_batch.service;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CsvUtils {

    private CsvUtils() {
        // インスタンス化防止
    }

    public static List<String[]> readAll(String path) throws IOException, CsvException {
        try (var r = new CSVReaderBuilder(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))
                .withCSVParser(
                        new CSVParserBuilder()
                                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                                .withQuoteChar(CSVWriter.DEFAULT_QUOTE_CHARACTER)
                                .withEscapeChar('\u0000')
                                .build())
                .build()) {
            return r.readAll();
        }
    }

    public static void writeAll(String path, List<String[]> rows) throws IOException {
        try (var w = new CSVWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.NO_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            for (var row : rows) {
                w.writeNext(row, true);
            }
            w.flush();
        }
    }

    public static void putCsv(ZipOutputStream zos, String name, List<String[]> rows) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        try (var writer = new CSVWriter(
                new OutputStreamWriter(zos, StandardCharsets.UTF_8),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.NO_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            for (var row : rows) {
                writer.writeNext(row, true);
            }
            writer.flush();
        }
        zos.closeEntry();
    }

    public static List<String[]> mapToRows(List<Map<String, String>> wide) {
        List<String[]> out = new ArrayList<>();
        if (wide.isEmpty())
            return out;
        Set<String> keys = wide.get(0).keySet();
        out.add(keys.toArray(new String[0]));
        for (Map<String, String> rec : wide) {
            List<String> row = new ArrayList<>();
            for (String k : keys)
                row.add(rec.getOrDefault(k, ""));
            out.add(row.toArray(new String[0]));
        }
        return out;
    }
}
