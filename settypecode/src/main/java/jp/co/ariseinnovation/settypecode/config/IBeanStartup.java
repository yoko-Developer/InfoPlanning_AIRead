package jp.co.ariseinnovation.settypecode.config;

import org.springframework.context.ApplicationContext;

public interface IBeanStartup {
	public void startup(ApplicationContext appContext);
}
