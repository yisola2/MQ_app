package demo.service;

import demo.model.Message;
import demo.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class StatisticsService {
    private static final Logger logger = LogManager.getLogger(StatisticsService.class);

    @Autowired
    private TopicRepository topicRepository;

    private Map<String, Integer> containerRestartCount = new HashMap<>();
    private Map<String, Integer> containerErrorCount = new HashMap<>();
    private int totalMonitoringCycles = 0;
    private LocalDateTime lastComputationTime = LocalDateTime.now();

    @Scheduled(fixedRate = 60000) // Compute statistics every minute
    @Transactional
    public void computeStatistics() {
        var topic = topicRepository.findById("worker-logs");
        if (topic.isEmpty()) {
            logger.warn("Worker logs topic not found");
            return;
        }

        List<Message> logs = topic.get().getMessages();
        
        // Reset counters
        containerRestartCount.clear();
        containerErrorCount.clear();
        int newMonitoringCycles = 0;

        for (Message log : logs) {
            String content = log.getText();
            LocalDateTime messageTime = log.getCreatedAt();

            // Only process messages since last computation
            if (messageTime.isAfter(lastComputationTime)) {
                if (content.contains("Monitoring cycle started")) {
                    newMonitoringCycles++;
                } else if (content.contains("Container stopped:")) {
                    String containerId = extractContainerId(content);
                    containerRestartCount.merge(containerId, 1, Integer::sum);
                } else if (content.contains("Container unhealthy:")) {
                    String containerId = extractContainerId(content);
                    containerErrorCount.merge(containerId, 1, Integer::sum);
                }
            }
        }

        // Update total monitoring cycles
        totalMonitoringCycles += newMonitoringCycles;

        // Log statistics
        logger.info("Statistics for period {} to {}:", lastComputationTime, LocalDateTime.now());
        logger.info("Total monitoring cycles: {}", totalMonitoringCycles);
        logger.info("New monitoring cycles: {}", newMonitoringCycles);
        logger.info("Container restart counts: {}", containerRestartCount);
        logger.info("Container error counts: {}", containerErrorCount);

        // Update last computation time
        lastComputationTime = LocalDateTime.now();
    }

    private String extractContainerId(String logMessage) {
        // Extract container ID or name from log message
        // Example: "Container stopped: mycontainer" -> "mycontainer"
        int colonIndex = logMessage.indexOf(':');
        if (colonIndex != -1 && colonIndex < logMessage.length() - 1) {
            return logMessage.substring(colonIndex + 1).trim();
        }
        return "unknown";
    }

    // Getter methods for statistics
    public int getTotalMonitoringCycles() {
        return totalMonitoringCycles;
    }

    public Map<String, Integer> getContainerRestartCount() {
        return new HashMap<>(containerRestartCount);
    }

    public Map<String, Integer> getContainerErrorCount() {
        return new HashMap<>(containerErrorCount);
    }
} 