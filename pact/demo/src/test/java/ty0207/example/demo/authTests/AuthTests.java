package ty0207.example.demo.authTests;

import static org.junit.Assert.assertFalse;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ty0207.example.demo.entity.User;
import ty0207.example.demo.repository.UserRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuthTests {

  @Autowired
  UserRepository userRepository;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void Test001getAllUsers() {
    userRepository.insert(new User());
    List<User> users = userRepository.findAll();
    assertFalse("Returned user should not be empty", users.isEmpty());
  }


}
