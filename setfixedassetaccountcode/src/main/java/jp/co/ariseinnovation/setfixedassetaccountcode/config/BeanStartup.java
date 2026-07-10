package jp.co.ariseinnovation.setfixedassetaccountcode.config;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BeanStartup implements IBeanStartup {

	@Override
	public void startup(ApplicationContext appContext) {
		AppSetting.current = appContext.getBean(AppSetting.class);
	}

}
