package ty0207.example.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ty0207.example.demo.entity.User;

public interface UserRepository extends MongoRepository<User, String> {

}
