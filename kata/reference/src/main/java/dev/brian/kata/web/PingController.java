package dev.brian.kata.web;

import dev.brian.kata.config.KataProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    private final KataProperties kataProperties;

    public PingController(KataProperties kataProperties) {
        this.kataProperties = kataProperties;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "UP");
    }

    @GetMapping("/greeting")
    public GreetingResponse greeting(@RequestParam(defaultValue = "World") String name) {
        return new GreetingResponse(kataProperties.greeting() + ", " + name);
    }

    @PostMapping("/echo")
    public EchoMessage echo(@RequestBody EchoMessage echoMessage) {
        return echoMessage;
    }

    public record GreetingResponse(String message) {
    }

    public record EchoMessage(String text) {
    }
}
