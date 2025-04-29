package demo.repository;

import java.util.List;


import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import demo.model.Message;
import demo.model.MessageQueue;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MessageRepository extends CrudRepository<Message, Long> {

    // List<Message> findByLastName(String lastName);

    Message findById(long id);
    //Used Spring Data JPA Derived query method to query for partial text match
    List<Message> findByTextContaining(String text);
    //Query so I can retrieve messages for a specific queue ID
    List<Message> findByQueue_Id(String queueId);
    //this query combines both. I filter based on the queue and a partial text match
    List<Message> findByQueueAndTextContaining(MessageQueue queue, String text);
    //or List<Message> findByQueue_IdAndTextContaining(String queueId, String text); this one uses the queueID
    List<Message> findAllByQueue(MessageQueue queue);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.id = :id AND m.isRead = true")
    int deleteIfRead(@Param("id") Long id);

    @Query("SELECT m FROM Message m JOIN m.topics t WHERE t.id = :topicId AND m.id >= :startId ORDER BY m.id ASC")
    List<Message> findMessagesFromSequence(@Param("topicId") String topicId, @Param("startId") Long startId);
    @Query("SELECT m FROM Message m JOIN m.topics t WHERE t.id = :topicId AND m.text LIKE %:text%")
    List<Message> searchMessagesInTopic(@Param("topicId") String topicId, @Param("text") String text);

    @Query("SELECT m FROM Message m WHERE m.queue.id = :queueId AND m.consumed = true")
    List<Message> findConsumedMessagesByQueueId(@Param("queueId") String queueId);

    @Query("SELECT m FROM Message m WHERE m.queue.id = :queueId AND m.consumed = false ORDER BY m.id ASC")
    List<Message> findUnconsumedMessagesByQueueId(@Param("queueId") String queueId);

}