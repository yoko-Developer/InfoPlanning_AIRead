package jp.co.ariseinnovation.mergerowsaireadcsv;

import jp.co.ariseinnovation.mergerowsaireadcsv.service.CsvProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@SpringBootApplication
public class MergerowsaireadcsvApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(MergerowsaireadcsvApplication.class);

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private CsvProcessorService csvProcessorService;

	public static void main(String[] args) {
		SpringApplication.run(MergerowsaireadcsvApplication.class, args);
	}

	/**
	 * アプリケーションのメイン処理
	 * コマンドライン引数からフォルダパスを取得し、CSVファイルを処理する
	 */
	@Override
	public void run(String... args) throws Exception {
		if (args.length == 0) {
			logger.error(messageSource.getMessage("error.no.folder.path", null, Locale.getDefault()));
			System.exit(1);
		}

		String folderPath = args[0];
		Path path = Paths.get(folderPath);

		if (!Files.exists(path)) {
			logger.error(messageSource.getMessage("error.path.not.exists", new Object[] { folderPath },
					Locale.getDefault()));
			System.exit(1);
		}

		if (!Files.isDirectory(path)) {
			logger.error(messageSource.getMessage("error.path.not.directory", new Object[] { folderPath },
					Locale.getDefault()));
			System.exit(1);
		}

		logger.info(messageSource.getMessage("info.folder.path", new Object[] { path.toAbsolutePath() },
				Locale.getDefault()));

		csvProcessorService.processCsvFiles(path);
	}



}
