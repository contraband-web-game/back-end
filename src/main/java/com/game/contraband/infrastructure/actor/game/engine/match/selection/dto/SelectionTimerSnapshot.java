package com.game.contraband.infrastructure.actor.game.engine.match.selection.dto;

import java.time.Duration;
import java.time.Instant;

public record SelectionTimerSnapshot(Instant startedAt, Duration duration) { }
