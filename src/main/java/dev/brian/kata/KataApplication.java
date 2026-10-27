package dev.brian.kata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KataApplication {
    public static void main(String[] args) {
        SpringApplication.run(KataApplication.class, args);
    }
}