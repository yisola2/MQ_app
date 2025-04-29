package demo.controller;

import demo.service.MonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/containers")
    public ResponseEntity<List<Map<String, Object>>> listContainers() {
        return ResponseEntity.ok(monitoringService.listContainers());
    }

    @GetMapping("/containers/{containerId}")
    public ResponseEntity<Map<String, Object>> getContainerDetails(@PathVariable String containerId) {
        return ResponseEntity.ok(monitoringService.getContainerDetails(containerId));
    }

    @GetMapping("/containers/{containerId}/stats")
    public ResponseEntity<Map<String, Object>> getContainerStats(@PathVariable String containerId) {
        return ResponseEntity.ok(monitoringService.getContainerStats(containerId));
    }

    @GetMapping("/containers/{containerId}/health")
    public ResponseEntity<Map<String, Object>> getContainerHealth(@PathVariable String containerId) {
        boolean healthy = monitoringService.isContainerHealthy(containerId);
        return ResponseEntity.ok(Map.of(
                "containerId", containerId,
                "healthy", healthy
        ));
    }

    @PostMapping("/containers/{containerId}/restart")
    public ResponseEntity<Map<String, Object>> restartContainer(@PathVariable String containerId) {
        monitoringService.restartContainer(containerId);
        return ResponseEntity.ok(Map.of(
                "containerId", containerId,
                "action", "restart initiated"
        ));
    }
}