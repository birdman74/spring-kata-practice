package dev.brian.kata.controller;

import dev.brian.kata.config.KataProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class KataController {

    private final KataProperties kataProperties;

    public KataController(KataProperties kataProperties) {
        this.kataProperties = kataProperties;
    }

    public record PingResponse(String status) {}

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("UP");
    }

    public record Greeting(String message) {}

    @GetMapping("/greeting")
    public Greeting getGreeting(@RequestParam(defaultValue = "World") String name) {
        return new Greeting(kataProperties.greeting() + ", " + name);
    }

    public record Message(String text) {}

    @PostMapping("/echo")
    public Message echoMessage(@RequestBody Message message) {
        return message;
    }
}
