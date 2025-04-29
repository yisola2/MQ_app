package demo.controller;

import demo.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("totalMonitoringCycles", statisticsService.getTotalMonitoringCycles());
        statistics.put("containerRestartCount", statisticsService.getContainerRestartCount());
        statistics.put("containerErrorCount", statisticsService.getContainerErrorCount());
        
        return ResponseEntity.ok(statistics);
    }
} 