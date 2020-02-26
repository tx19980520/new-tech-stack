package ty0207.example.demo.authTests;

import static org.junit.Assert.assertFalse;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import ty0207.example.demo.entity.User;
import ty0207.example.demo.repository.UserRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PactTests {

  @Autowired
  UserRepository userRepository;

  @Rule
  public PactProviderRuleMk2 mockProvider
      = new PactProviderRuleMk2("test_provider", "localhost", 8080, this);


  @Pact(consumer = "test_consumer")
  public RequestResponsePact createPact(PactDslWithProvider builder) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    return builder
        .given("test GET")
        .uponReceiving("GET REQUEST")
        .path("/pact")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(headers)
        .body(new PactDslJsonBody().stringType("name").booleanType("condition"))
        .given("test POST")
        .uponReceiving("POST REQUEST")
        .method("POST")
        .headers(headers)
        .body("{\"name\": \"Michael\"}")
        .path("/create")
        .willRespondWith()
        .status(201)
        .toPact();
  }

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  @PactVerification()
  public void givenGet_whenSendRequest_shouldReturn200WithProperHeaderAndBody() {

    // when
    ResponseEntity<String> response = new RestTemplate()
        .getForEntity(mockProvider.getUrl() + "/pact", String.class);

    // then
    Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    String jsonBody = "{\"name\": \"Michael\"}";

    // when
    ResponseEntity<String> postResponse = new RestTemplate()
        .exchange(
            mockProvider.getUrl() + "/create",
            HttpMethod.POST,
            new HttpEntity<>(jsonBody, httpHeaders),
            String.class);
  }

}
