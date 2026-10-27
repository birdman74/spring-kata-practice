package dev.brian.kata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kata")
public record KataProperties (String greeting) {}
