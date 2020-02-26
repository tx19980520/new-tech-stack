package ty0207.example.demo.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ty0207.example.demo.dto.LoginRequest;
import ty0207.example.demo.exception.exceptions.CustomizeException;

@RestController
@RequestMapping("/auth")
@Api("用户管理")
public class AuthController {
  @ApiOperation(value = "用户登录", notes = "通过用户账号密码进行登录")
  @RequestMapping(value = "/login", method = { RequestMethod.POST, RequestMethod.OPTIONS }, produces = "application/json;charset=UTF-8")
  public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
    return null;
  }

  @RequestMapping(value = "/exceptionURL", method = { RequestMethod.GET, RequestMethod.OPTIONS }, produces = "application/json;charset=UTF-8")
  public ResponseEntity<?> exceptionURL(@RequestBody LoginRequest loginRequest) {
    throw new CustomizeException("error", HttpStatus.FORBIDDEN);
  }

  @RequestMapping(value = "/annotationExample", method = { RequestMethod.GET, RequestMethod.OPTIONS }, produces = "application/json;charset=UTF-8")
  public ResponseEntity<?> annotationExample(@RequestHeader("jwt") String jwt) {
    throw new CustomizeException("error", HttpStatus.FORBIDDEN);
  }
}
