package demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import demo.model.Topic;
import demo.model.Message;
import demo.repository.TopicRepository;
import org.springframework.transaction.annotation.Transactional;
import demo.repository.MessageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class MonitoringService {
    private static final Logger logger = LogManager.getLogger(MonitoringService.class);

    @Value("${DOCKER_API_URL:http://docker_api:2375}")
    private String dockerApiUrl;

    @Value("${INSTANCE_NAME:unknown}")
    private String instanceName;

    private final MessageService messageService;
    private final TopicRepository topicRepository;
    private final MessageRepository messageRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Counter healthCheckCounter;
    private final Counter unhealthyCheckCounter;
    private final Counter restartCounter;

    public MonitoringService(MessageService messageService, TopicRepository topicRepository, MessageRepository messageRepository, MeterRegistry registry) {
        this.messageService = messageService;
        this.topicRepository = topicRepository;
        this.messageRepository = messageRepository;
        this.healthCheckCounter = Counter.builder("container_health_check_total")
            .description("Total number of container health checks performed")
            .register(registry);
        this.unhealthyCheckCounter = Counter.builder("container_health_check_unhealthy_total")
            .description("Number of times containers were found unhealthy")
            .register(registry);
        this.restartCounter = Counter.builder("container_restart_count_total")
            .description("Number of container restarts")
            .register(registry);
    }

    @Value("${monitoring.enabled:false}")
    private boolean monitoringEnabled;

    private boolean isMonitoringInstance() {
        return "monitoring_service".equals(instanceName);
    }

    @Transactional
    private void logToTopic(String message) {
        if (!isMonitoringInstance()) {
            return;
        }

        Optional<Topic> topic = topicRepository.findById("worker-logs");
        if (topic.isPresent()) {
            Topic workerLogsTopic = topic.get();
            
            // Keep only last 100 messages
            List<Message> messages = workerLogsTopic.getMessages();
            if (messages.size() >= 100) {
                // Remove oldest messages
                List<Message> messagesToRemove = messages.subList(0, messages.size() - 99);
                for (Message oldMessage : messagesToRemove) {
                    workerLogsTopic.removeMessage(oldMessage);
                    messageRepository.delete(oldMessage);
                }
            }

            // Add new message
            Message logMessage = new Message(message);
            messageRepository.save(logMessage);
            workerLogsTopic.addMessage(logMessage);
            topicRepository.save(workerLogsTopic);
            logger.debug("Logged to worker-logs topic: {}", message);
        } else {
            logger.error("worker-logs topic not found");
        }
    }

    @Scheduled(fixedRate = 30000)  // Run every 30 seconds instead of 10
    public void monitorContainers() {
        if (!monitoringEnabled || !isMonitoringInstance()) {
            return;
        }

        try {
            logToTopic("Monitoring cycle started");
            logger.debug("Monitoring containers at {}", dockerApiUrl);

            try {
                String url = dockerApiUrl + "/containers/json?all=true";
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );

                List<Map<String, Object>> containers = response.getBody();
                if (containers == null) {
                    String msg = "No containers data received from Docker API";
                    logToTopic(msg);
                    logger.warn(msg);
                    return;
                }

                logger.debug("Found {} containers to monitor", containers.size());
                for (Map<String, Object> container : containers) {
                    String containerId = (String) container.get("Id");
                    String status = (String) container.get("Status");

                    String containerName = "unknown";
                    if (container.get("Names") instanceof List) {
                        List<?> names = (List<?>) container.get("Names");
                        if (names != null && !names.isEmpty()) {
                            containerName = ((String) names.get(0)).replaceFirst("^/", "");
                        }
                    }

                    // Only log container info if there's an issue
                    if (status != null && (status.contains("Exited") || status.contains("Dead"))) {
                        String msg = "Container stopped: " + containerName;
                        logToTopic(msg);
                        logger.info(msg);
                        messageService.addMessageToQueue("restart-queue", containerId);
                    }
                    else if (status != null && status.contains("Running")) {
                        boolean healthy = isContainerHealthy(containerId);
                        if (!healthy) {
                            String msg = "Container unhealthy: " + containerName;
                            logToTopic(msg);
                            logger.info(msg);
                            messageService.addMessageToQueue("restart-queue", containerId);
                        }
                    }
                }
            } catch (Exception e) {
                String msg = "Docker API connection failed: " + e.getMessage();
                logToTopic(msg);
                logger.error(msg, e);
            }
            
            logToTopic("Monitoring cycle completed");
        } catch (Exception e) {
            String msg = "Error in monitoring cycle: " + e.getMessage();
            logToTopic(msg);
            logger.error(msg, e);
        }
    }

    public List<Map<String, Object>> listContainers() {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    dockerApiUrl + "/containers/json?all=true",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error listing containers: " + e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> getContainerDetails(String containerId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    dockerApiUrl + "/containers/" + containerId + "/json",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error getting container details: " + e.getMessage());
            return Map.of();
        }
    }

    public Map<String, Object> getContainerStats(String containerId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    dockerApiUrl + "/containers/" + containerId + "/stats?stream=false",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error getting container stats: " + e.getMessage());
            return Map.of();
        }
    }

    // Restart a container by ID
    public void restartContainer(String containerId) {
        try {
            Map<String, Object> details = getContainerDetails(containerId);
            String containerName = "unknown";
            if (details.containsKey("Name")) {
                containerName = ((String) details.get("Name")).replaceFirst("^/", "");
            }
            
            logger.info("Attempting to restart container: {} ({})", containerName, containerId);

            // Start the container using Docker API
            restTemplate.postForEntity(
                    dockerApiUrl + "/containers/" + containerId + "/start",
                    null,
                    String.class);

            // Only increment counter if restart was successful
            restartCounter.increment();
            logger.info("Container restarted successfully: {} ({})", containerName, containerId);
            logToTopic("Container restarted: " + containerName);
        } catch (Exception e) {
            logger.error("Error restarting container: " + e.getMessage(), e);
            logToTopic("Failed to restart container: " + e.getMessage());
        }
    }

    // Check if a container is healthy
    public boolean isContainerHealthy(String containerId) {
        try {
            healthCheckCounter.increment();
            Map<String, Object> details = getContainerDetails(containerId);
            
            // Check if container has health check configured
            Map<String, Object> state = (Map<String, Object>) details.get("State");
            if (state == null) {
                return true; // Assume healthy if no state info
            }

            // Check health status if available
            Map<String, Object> health = (Map<String, Object>) state.get("Health");
            if (health != null) {
                String status = (String) health.get("Status");
                if (!"healthy".equals(status)) {
                    unhealthyCheckCounter.increment();
                    return false;
                }
            }

            // Check if container is running
            String status = (String) state.get("Status");
            boolean healthy = "running".equals(status);
            if (!healthy) {
                unhealthyCheckCounter.increment();
            }
            return healthy;
        } catch (Exception e) {
            logger.error("Error checking container health: " + e.getMessage(), e);
            unhealthyCheckCounter.increment();
            return false;
        }
    }

    // Process the restart queue
    @Scheduled(fixedRate = 30000)  // Run every 30 seconds instead of 15
    public void processRestartQueue() {
        if (!monitoringEnabled || !isMonitoringInstance()) {
            return;
        }

        try {
            List<String> containerIdsToRestart = messageService.getMessagesFromQueue("restart-queue");

            if (containerIdsToRestart != null && !containerIdsToRestart.isEmpty()) {
                logToTopic("Processing " + containerIdsToRestart.size() + " containers for restart");

                for (String containerId : containerIdsToRestart) {
                    logToTopic("Restarting container: " + containerId);
                    restartContainer(containerId);
                    messageService.removeMessageFromQueue("restart-queue", containerId);
                }
            }
        } catch (Exception e) {
            String msg = "Error processing restart queue: " + e.getMessage();
            logToTopic(msg);
            logger.error(msg, e);
        }
    }
}