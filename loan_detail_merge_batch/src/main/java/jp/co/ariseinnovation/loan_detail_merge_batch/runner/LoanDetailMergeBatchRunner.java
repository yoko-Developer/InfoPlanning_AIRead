package jp.co.ariseinnovation.loan_detail_merge_batch.runner;

import jp.co.ariseinnovation.loan_detail_merge_batch.service.LoanDetailMergeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LoanDetailMergeBatchRunner implements CommandLineRunner {

  private final LoanDetailMergeService service;

  public LoanDetailMergeBatchRunner(LoanDetailMergeService service) {
    this.service = service;
  }

  @Override
  public void run(String... args) {
    if (args.length < 2) {
      System.err.println("Usage: java -jar app.jar <PROCESSID> <PROCESSDATE>");
      return;
    }

    String processId = args[0];
    String processDate = args[1];

    // debugZipは固定でfalse
    service.run(processId, processDate, false);
  }
}
