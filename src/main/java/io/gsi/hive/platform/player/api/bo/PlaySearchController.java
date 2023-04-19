package io.gsi.hive.platform.player.api.bo;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.play.search.PlaySearchArguments;
import io.gsi.hive.platform.player.play.search.PlaySearchRecord;
import io.gsi.hive.platform.player.play.search.PlaySearchService;
import java.util.Collections;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bo/platform/player/v1")
@Loggable
public class PlaySearchController {

    private final PlaySearchService playSearchService;
    @Autowired
    public PlaySearchController(final PlaySearchService playSearchService)
    {
        this.playSearchService = playSearchService;
    }

    @GetMapping(path="/play/search")
    public Page<PlaySearchRecord> performPlaySearch(@ModelAttribute @Valid PlaySearchArguments playSearchArguments)
    {
        if(playSearchService.isDateRangeWithinAcceptableBoundary(
                playSearchArguments.getDateFrom(), playSearchArguments.getDateTo()))
        {
            return playSearchService.search(playSearchArguments);
        }
        else
        {
            throw new IllegalArgumentException("date range too large");
        }
    }

    /**
     * @deprecated
     *  Deprecated due to implementing multiple igp code play search
     */
    @GetMapping(path="/igp/{igpCode}/play/search")
    @Deprecated
    public Page<PlaySearchRecord> performBackwardsCompatiblePlaySearch(@PathVariable("igpCode") String igpCode,
    @ModelAttribute @Valid PlaySearchArguments playSearchArguments)
    {
        playSearchArguments.setIgpCodes(Collections.singletonList(igpCode));
        return this.performPlaySearch(playSearchArguments);
    }

}
