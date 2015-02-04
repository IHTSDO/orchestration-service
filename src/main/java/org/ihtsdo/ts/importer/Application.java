package org.ihtsdo.ts.importer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Application {

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[] {"spring_context.xml"});
		Importer importer = applicationContext.getBean(Importer.class);
		importer.go();
	}

}
