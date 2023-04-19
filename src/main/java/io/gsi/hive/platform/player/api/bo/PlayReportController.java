package io.gsi.hive.platform.player.api.bo;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.play.report.PlayReportArguments;
import io.gsi.hive.platform.player.play.report.PlayReportRecord;
import io.gsi.hive.platform.player.play.report.PlayReportService;

@RestController
@RequestMapping("/bo/platform/player/v1")
@Loggable
public class PlayReportController
{
    private final PlayReportService playReportService;

    public PlayReportController(PlayReportService playReportService) {
        this.playReportService = playReportService;
    }

    @GetMapping(path="/play/report")
    public List<PlayReportRecord> performPlayReport(@ModelAttribute @Valid PlayReportArguments playReportArguments)
    {
        return playReportService.generateReport(playReportArguments);
    }

    /**
     * @deprecated
     *  Deprecated due to implementing multiple igp code play report
     */
    @GetMapping(path="/igp/{igpCode}/play/report")
    @Deprecated
    public List<PlayReportRecord> performBackwardsCompatiblePlayReport(@PathVariable("igpCode") String igpCode,
        @ModelAttribute @Valid PlayReportArguments playReportArguments)
    {
        playReportArguments.setIgpCodes(Collections.singletonList(igpCode));
        return playReportService.generateReport(playReportArguments);
    }
}