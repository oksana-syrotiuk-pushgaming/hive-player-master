package io.gsi.hive.platform.player.registry.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistryExceptionResponse {
    private String code;
    private String message;
    private String debug;
    private String reqId;
    private ZonedDateTime timestamp;
}
