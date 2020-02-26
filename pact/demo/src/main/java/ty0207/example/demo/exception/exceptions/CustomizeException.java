package ty0207.example.demo.exception.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class CustomizeException extends RuntimeException {
  private HttpStatus code;
  public CustomizeException(String message, HttpStatus code) {
    super(message);
    this.code = code;
  }

}
