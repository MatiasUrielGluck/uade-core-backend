package com.uade.corehub;

import com.uade.corehub.channels.ChannelRegistryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan(basePackageClasses = { ChannelRegistryProperties.class })
@SpringBootApplication
public class CoreHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreHubApplication.class, args);
	}

}
