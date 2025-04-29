package demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import demo.model.MessageQueue;

public interface QueueRepository extends CrudRepository<MessageQueue, String> {

    Optional<MessageQueue> findById(String id);

    List<MessageQueue> findAllBy();
}