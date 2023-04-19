package io.gsi.hive.platform.player.api.bo;

import io.gsi.commons.logging.Loggable;
import io.gsi.hive.platform.player.cleardown.report.CleardownReportArguments;
import io.gsi.hive.platform.player.cleardown.report.CleardownReportRecord;
import io.gsi.hive.platform.player.cleardown.report.CleardownReportService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bo/platform/player/v1")
@Loggable
public class CleardownReportController {

  private final CleardownReportService cleardownReportService;

  public CleardownReportController(CleardownReportService cleardownReportService) {
    this.cleardownReportService = cleardownReportService;
  }

  @GetMapping(path = "/cleardown/report")
  public List<CleardownReportRecord> performCleardownReport(
      @ModelAttribute @Valid CleardownReportArguments cleardownReportArguments) {
    return cleardownReportService.generateReport(cleardownReportArguments);
  }

}
