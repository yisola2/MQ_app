package demo.controller;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import demo.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import demo.model.MessageQueue;
import jakarta.annotation.PostConstruct;
import demo.model.Message;

@RestController
@RequestMapping("/queues")
public class MessageQueueController {

    @Value("${INSTANCE_NAME:spring_app}")
    private String instanceName;

    @GetMapping("/instance")
    public ResponseEntity<String> getQueues() {
        return ResponseEntity.ok("Response from " + instanceName);
    }


    @Autowired
    private MessageService messageService;

    // so we can fetch all queues
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Collection<MessageQueue>> getProduct(

            @RequestParam(value = "startWith", defaultValue = "") String prefix) {
        return new ResponseEntity<>(messageService.getQueuesStartingWith(prefix), HttpStatus.OK);

    }

    // to fetch all messages for a queue
    @RequestMapping(value = "/{id}/messages", method = RequestMethod.GET)
    public ResponseEntity<List<Message>> getMessages(@PathVariable("id") String id) {
        List<Message> messages = messageService.getMessagesForQueue(id);
        if (messages == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    // fetch next message from a queue
    @RequestMapping(value = "/{id}/messages/next", method = RequestMethod.GET)
    public ResponseEntity<Message> getNextMessage(@PathVariable("id") String id) {
        Message message = messageService.getNextMessage(id);
        if (message == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    //add a message to a queue
    @RequestMapping(value = "/{id}/messages", method = RequestMethod.POST)
    public ResponseEntity<Message> addMessage(@PathVariable("id") String id, @RequestBody String contentString) {
        Message message = messageService.addMessageToQueue(id, contentString);
        if (message == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    // delete a message from a queue
    @RequestMapping(value = "/{id}/messages/{msg}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteMessage(@PathVariable("id") String id, @PathVariable("msg") long mid) {
        boolean success = messageService.deleteMessage(id, mid);
        if (!success) {
            // Check if the message exists but is unread
            List<Message> messages = messageService.getMessagesForQueue(id);
            if (messages != null) {
                Optional<Message> messageOpt = messages.stream().filter(m -> m.getId() == mid).findFirst();
                if (messageOpt.isPresent() && !messageOpt.get().isRead()) {
                    return new ResponseEntity<>("Cannot delete unread message", HttpStatus.FORBIDDEN);
                }
            }
            return new ResponseEntity<>("Message or queue not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>("Message deleted successfully", HttpStatus.OK);
    }

    // Add this endpoint
    @GetMapping("/{id}/messages/consumed")
    public ResponseEntity<List<Message>> getConsumedMessages(@PathVariable("id") String id) {
        List<Message> messages = messageService.getConsumedMessages(id);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }
}
