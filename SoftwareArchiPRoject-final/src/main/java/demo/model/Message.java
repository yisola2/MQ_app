package demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
//import com.fasterxml.jackson.annotation.JsonManagedReference;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
)
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String text;

    @ManyToOne
    @JoinColumn(name = "queue_id")
    @JsonBackReference
    private MessageQueue queue;

    @ManyToMany(mappedBy = "messages")
    private List<Topic> topics = new ArrayList<>();



    // our new field to track message status
    private boolean isRead = false;

    private LocalDateTime createdAt;
    private LocalDateTime firstAccessedAt;
    private int readCount = 0;

    private boolean consumed = false;
    private LocalDateTime consumedAt;

    public Message() {
        this.createdAt = LocalDateTime.now();
    }

    public Message(String text) {
        this.id = id;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MessageQueue getQueue() {
        return queue;
    }

    public void setQueue(MessageQueue queue) {
        this.queue = queue;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setIsRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getFirstAccessedAt() { return firstAccessedAt; }
    public int getReadCount() { return readCount; }

    public List<Topic> getTopics() {
        return topics;
    }

    public void setTopics(List<Topic> topics) {
        this.topics = topics;
    }
    public void setRead(boolean read) { this.isRead = read; }

    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.firstAccessedAt = LocalDateTime.now();
        }
        this.readCount++;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }
}