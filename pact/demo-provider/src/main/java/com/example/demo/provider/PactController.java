package com.example.demo.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PactController {
  @RequestMapping(value = "/pact", method = { RequestMethod.GET, RequestMethod.OPTIONS }, produces = "application/json;charset=UTF-8")
  public ResponseEntity<?> pact() {
    Map<String, Object> json = new HashMap<>();
    json.put("condition", true);
    json.put("name", "tony");
    HttpHeaders headers = new HttpHeaders();

    ArrayList<String> typeList = new ArrayList<>();
    typeList.add("application/json");
    headers.put("Content-Type", typeList);
    return new ResponseEntity<>(json,headers, HttpStatus.OK);
  }

  @RequestMapping(value = "/create", method = { RequestMethod.POST, RequestMethod.OPTIONS }, produces = "application/json;charset=UTF-8")
  public ResponseEntity<?> create() {
    return new ResponseEntity<>(HttpStatus.CREATED);
  }
}
