package ty0207.example.demo.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class User {
  @Id
  private String id;
  private String type;
  private String password;
  private String caseId;

}
