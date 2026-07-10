package jp.co.ariseinnovation.link;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Main App */
@SpringBootApplication
public class App {
	public static void main(String[] args) {
		var app = new SpringApplication(App.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}
}
