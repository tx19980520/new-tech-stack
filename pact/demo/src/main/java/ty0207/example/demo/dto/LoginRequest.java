package ty0207.example.demo.dto;

import java.io.Serializable;
import lombok.Data;

@Data
public class LoginRequest implements Serializable {
  private String password;
  private String phone;
  private String type;
}
