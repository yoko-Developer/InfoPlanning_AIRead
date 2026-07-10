package jp.co.ariseinnovation.link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jp.co.ariseinnovation.link.service.FixLinkService;

@Component
public class AppRunner implements ApplicationRunner {
    private final static Logger log = LoggerFactory.getLogger(AppRunner.class);
    @Autowired
    private FixLinkService dataLinkService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("app start.");
        var runArgs = args.getNonOptionArgs();
        if (runArgs.isEmpty() || runArgs.size() != 1) {
            log.error("起動パラメータエラー");
            throw new Exception();
        }
        dataLinkService.typeLink(runArgs.get(0));
        log.info("app end.");
    }
}
