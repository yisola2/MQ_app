package demo.controller;

import demo.model.Message;
import demo.model.Topic;
import demo.repository.MessageRepository;
import demo.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/topics")
public class TopicController {

    @Autowired
    private TopicRepository topicRepo;

    @Autowired
    private MessageRepository messageRepo;

    // Get all messages from a topic
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable("id") String topicId,
            @RequestParam(value = "start", required = false) Long startId) {

        if (startId != null) {
            // If start ID is provided, get messages from that sequence
            List<Message> messages = messageRepo.findMessagesFromSequence(topicId, startId);
            return ResponseEntity.ok(messages);
        } else {
            // If no start ID, get all messages
            Optional<Topic> topic = topicRepo.findById(topicId);
            if (topic.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(topic.get().getMessages());
        }
    }

    // ✅ 2. Search Messages in a Topic by Partial Content
    @GetMapping("/{id}/messages/search")
    public ResponseEntity<List<Message>> searchMessagesInTopic(
            @PathVariable("id") String topicId,
            @RequestParam("q") String text) {

        List<Message> messages = messageRepo.searchMessagesInTopic(topicId, text);
        return ResponseEntity.ok(messages);
    }

    // ✅ 3. Remove a Message from a Topic, Delete it Only if Not in Any Other Topic
    @DeleteMapping("/{topicId}/messages/{messageId}")
    public ResponseEntity<String> deleteMessageFromTopic(
            @PathVariable("topicId") String topicId,
            @PathVariable("messageId") Long messageId) {

        Optional<Topic> topicOpt = topicRepo.findById(topicId);
        Optional<Message> messageOpt = messageRepo.findById(messageId);

        if (topicOpt.isEmpty()) {
            return new ResponseEntity<>("Topic not found", HttpStatus.NOT_FOUND);
        }
        if (messageOpt.isEmpty()) {
            return new ResponseEntity<>("Message not found", HttpStatus.NOT_FOUND);
        }

        Topic topic = topicOpt.get();
        Message message = messageOpt.get();

        // Remove the message from the topic
        topic.removeMessage(message);
        topicRepo.save(topic);

        // If the message is not in any other topics, delete it
        if (message.getTopics().isEmpty()) {
            messageRepo.delete(message);
            return ResponseEntity.ok("Message deleted from topic and removed from database.");
        }

        return ResponseEntity.ok("Message removed from topic but still exists in other topics.");
    }
}
