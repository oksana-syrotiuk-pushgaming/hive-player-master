package io.gsi.hive.platform.player.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    @NotNull
    private String content;
    @NotNull
    private String type;
    @NotNull
    private String format;
}
