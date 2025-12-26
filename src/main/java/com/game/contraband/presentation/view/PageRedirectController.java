package com.game.contraband.presentation.view;

import java.net.URI;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
public class PageRedirectController {

    @GetMapping("/")
    public ResponseEntity<Void> redirectIndex() {
        return ResponseEntity.status(HttpStatus.FOUND)
                             .location(URI.create("/index.html"))
                             .build();
    }

    @GetMapping("/monitor")
    public ResponseEntity<Void> redirectMonitor() {
        return ResponseEntity.status(HttpStatus.FOUND)
                             .location(URI.create("/monitor.html"))
                             .build();
    }

    @GetMapping("/monitor/actors")
    public ResponseEntity<Void> redirectMonitorActors() {
        return ResponseEntity.status(HttpStatus.FOUND)
                             .location(URI.create("/monitor-actors.html"))
                             .build();
    }
}
