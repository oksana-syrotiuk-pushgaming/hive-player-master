/**
 * © gsi.io 2017
 */
package io.gsi.hive.platform.player;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Player functionality for the Hive Gaming Platform
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import(io.gsi.commons.config.MetricsConfig.class)
@ComponentScan(basePackageClasses = HivePlayer.class)
public class HivePlayer
{
	public static void main(String[] args) {
		SpringApplication.run(HivePlayer.class,args);
	}
}
