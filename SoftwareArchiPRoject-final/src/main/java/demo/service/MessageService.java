package demo.service;

import demo.model.Message;
import demo.model.MessageQueue;
import demo.model.Topic;
import demo.repository.MessageRepository;
import demo.repository.QueueRepository;
import demo.repository.TopicRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

@Service
public class MessageService {

    private static final Logger logger = LogManager.getLogger(MessageService.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private TopicRepository topicRepository;

    private final Counter messagesProcessed;
    private final Counter messagesFailed;

    public MessageService(MeterRegistry registry) {
        this.messagesProcessed = registry.counter("messages.processed");
        this.messagesFailed = registry.counter("messages.failed");
    }

    // Initialize sample messages at startup
    @PostConstruct
    private void init() {
        // Step 1: Create and save all messages
        Message systemStatus = new Message("SYSTEM: All services running normally - CPU: 45%, Memory: 60%");
        Message dbAlert = new Message("DATABASE: Connection pool at 75% capacity - Consider scaling");
        Message networkAlert = new Message("NETWORK: High latency detected between app1 and db (150ms)");
        Message containerStatus = new Message("CONTAINER: app1 resource usage - CPU: 80%, Memory: 512MB");
        Message containerScaling = new Message("CONTAINER: app2 scaling up - New instance requested");
        Message workerStatus = new Message("WORKER: Processing batch job #1234 - 50% complete");

        messageRepository.save(systemStatus);
        messageRepository.save(dbAlert);
        messageRepository.save(networkAlert);
        messageRepository.save(containerStatus);
        messageRepository.save(containerScaling);
        messageRepository.save(workerStatus);

        // Step 2: Create and save queues
        MessageQueue alertQueue = new MessageQueue();
        alertQueue.setId("main");
        queueRepository.save(alertQueue);

        MessageQueue maintenanceQueue = new MessageQueue();
        maintenanceQueue.setId("secondary");
        queueRepository.save(maintenanceQueue);

        MessageQueue restartQueue = new MessageQueue();
        restartQueue.setId("restart-queue");
        queueRepository.save(restartQueue);

        // Step 3: Add messages to queues and save
        alertQueue.addMessage(systemStatus);
        alertQueue.addMessage(dbAlert);
        queueRepository.save(alertQueue);

        maintenanceQueue.addMessage(networkAlert);
        queueRepository.save(maintenanceQueue);

        // Step 4: Create and save topics
        Topic systemTopic = new Topic("system-events");
        Topic containerTopic = new Topic("container-status");
        Topic workerTopic = new Topic("worker-logs");

        topicRepository.save(systemTopic);
        topicRepository.save(containerTopic);
        topicRepository.save(workerTopic);

        // Step 5: Add messages to topics and save
        systemTopic.addMessage(systemStatus);
        systemTopic.addMessage(dbAlert);
        topicRepository.save(systemTopic);

        containerTopic.addMessage(containerStatus);
        containerTopic.addMessage(containerScaling);
        topicRepository.save(containerTopic);

        workerTopic.addMessage(workerStatus);
        topicRepository.save(workerTopic);

        // Step 6: Create monitoring queues
        MessageQueue monitoringQueue = new MessageQueue();
        monitoringQueue.setId("monitoring-commands");
        queueRepository.save(monitoringQueue);

        MessageQueue resultsQueue = new MessageQueue();
        resultsQueue.setId("monitoring-results");
        queueRepository.save(resultsQueue);

        logger.info("Initialized system with messages, queues, and topics");
    }

    // Fetch all queues that start with a given prefix
    public List<MessageQueue> getQueuesStartingWith(String prefix) {
        return queueRepository.findAllBy().stream().filter(q -> q.getId().startsWith(prefix)).toList();
    }

    // Fetch all messages for a given queue
    public List<Message> getMessagesForQueue(String queueId) {
        Optional<MessageQueue> queueOpt = queueRepository.findById(queueId);
        if (queueOpt.isEmpty()) {
            logger.warn("Queue '{}' not found when fetching messages.", queueId);
            return null;
        }
        return messageRepository.findAllByQueue(queueOpt.get());
    }

    // Fetch next message from queue and mark it as read
    @Transactional
    public Message getNextMessage(String queueId) {
        try {
            Optional<MessageQueue> queueOpt = queueRepository.findById(queueId);
            if (queueOpt.isPresent()) {
                MessageQueue queue = queueOpt.get();
                List<Message> messages = messageRepository.findUnconsumedMessagesByQueueId(queueId);
                
                if (!messages.isEmpty()) {
                    Message message = messages.get(0);
                    
                    // Mark as read and track metadata
                    message.markAsRead();
                    
                    // Mark as consumed and set consumed timestamp
                    message.setConsumed(true);
                    message.setConsumedAt(LocalDateTime.now());
                    
                    // Remove from queue but keep in repository for tracking
                    queue.removeMessage(message.getId());
                    queueRepository.save(queue);
                    
                    // Save the message with its new status
                    messageRepository.save(message);
                    
                    logger.info("Message ID {} consumed from queue '{}'. Read count: {}. Remains in {} topics.",
                        message.getId(), queueId, message.getReadCount(), message.getTopics().size());
                    
                    messagesProcessed.increment();
                    return message;
                }
            }
            messagesFailed.increment();
            return null;
        } catch (Exception e) {
            messagesFailed.increment();
            throw e;
        }
    }
    @Transactional
    // Add a message to a queue
    public Message addMessageToQueue(String queueId, String content) {
        Optional<MessageQueue> queueOpt = queueRepository.findById(queueId);
        if (queueOpt.isPresent()) {
            MessageQueue queue = queueOpt.get();
            Message message = new Message(content);
            queue.addMessage(message);
            messageRepository.save(message);

            logger.info("New message added to queue '{}': {}", queueId, content);
            return message;
        }
        logger.error("Queue '{}' not found. Message not added.", queueId);
        return null;
    }

    // Delete a message from a queue
    public boolean deleteMessage(String queueId, long messageId) {
        Optional<MessageQueue> queueOpt = queueRepository.findById(queueId);
        if (queueOpt.isEmpty()) {
            logger.warn("Queue '{}' not found. Cannot delete message ID {}", queueId, messageId);
            return false;
        }

        MessageQueue queue = queueOpt.get();
        Optional<Message> messageOpt = queue.removeMessage(messageId);

        if (messageOpt.isEmpty()) {
            logger.warn("Message ID {} not found in queue '{}'", messageId, queueId);
            return false;
        }

        Message message = messageOpt.get();
        // Check if the message has been read
        if (!message.isRead()) {
            logger.warn("Cannot delete unread message (ID: {}) from queue '{}'", messageId, queueId);
            // Put the message back in the queue since we removed it earlier
            queue.addMessage(message);
            queueRepository.save(queue);
            return false;
        }

        messageRepository.delete(messageOpt.get());
        logger.info("Message ID {} deleted from queue '{}'", messageId, queueId);
        return true;
    }

    // Get message content (container IDs) from the restart queue
    public List<String> getMessagesFromQueue(String queueId) {
        Optional<MessageQueue> queueOpt = queueRepository.findById(queueId);
        if (queueOpt.isEmpty()) {
            logger.warn("Queue '{}' not found when retrieving messages.", queueId);
            return List.of();
        }

        MessageQueue queue = queueOpt.get();
        List<Message> messages = messageRepository.findAllByQueue(queue);

        return messages.stream()
                .map(Message::getText)
                .toList();
    }

    // Remove a message from a queue by content (for the container restart functionality)
    @Transactional
    public void removeMessageFromQueue(String queueId, String content) {
        Optional<MessageQueue> queueOpt = queueRepository.findById(queueId);
        if (queueOpt.isEmpty()) {
            logger.warn("Queue '{}' not found when removing message with content: {}", queueId, content);
            return;
        }

        MessageQueue queue = queueOpt.get();
        List<Message> messages = messageRepository.findAllByQueue(queue);

        // Find the first message with matching content
        Optional<Message> messageToRemove = messages.stream()
                .filter(msg -> content.equals(msg.getText()))
                .findFirst();

        if (messageToRemove.isPresent()) {
            Message message = messageToRemove.get();
            // Mark as read first to ensure it can be deleted
            message.markAsRead();
            messageRepository.save(message);

            // Now try to remove it
            queue.removeMessage(message.getId());
            messageRepository.delete(message);
            queueRepository.save(queue);

            logger.info("Removed message with content '{}' from queue '{}'", content, queueId);
        } else {
            logger.warn("No message with content '{}' found in queue '{}'", content, queueId);
        }
    }

    // Add this new method
    public List<Message> getConsumedMessages(String queueId) {
        Optional<MessageQueue> queueOpt = queueRepository.findById(queueId);
        if (queueOpt.isEmpty()) {
            logger.warn("Queue '{}' not found when fetching consumed messages.", queueId);
            return List.of();
        }
        return messageRepository.findConsumedMessagesByQueueId(queueId);
    }
}
