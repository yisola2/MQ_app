package demo.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Topic {

    @Id
    private String id;  // Unique identifier for the topic

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "topic_message",
            joinColumns = @JoinColumn(name = "topic_id"),
            inverseJoinColumns = @JoinColumn(name = "message_id")
    )
    private List<Message> messages = new ArrayList<>();

    // Constructors
    public Topic() {
    }

    public Topic(String id) {
        this.id = id;
    }

    // Add message to topic
    public void addMessage(Message message) {
        messages.add(message);
        message.getTopics().add(this);
    }

    // Remove message from topic
    public void removeMessage(Message message) {
        messages.remove(message);
        message.getTopics().remove(this);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
