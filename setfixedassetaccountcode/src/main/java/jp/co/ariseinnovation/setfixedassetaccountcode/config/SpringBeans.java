package jp.co.ariseinnovation.setfixedassetaccountcode.config;

import java.util.Objects;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringBeans implements ApplicationContextAware {
	private static ApplicationContext appContext;

	@SuppressWarnings("null")
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		appContext = applicationContext;
		var beamStartup = appContext.getBean(IBeanStartup.class);
		if (Objects.nonNull(beamStartup)) {
			beamStartup.startup(appContext);
		}
	}

	public final static ApplicationContext appContext() {
		return appContext;
	}
}
