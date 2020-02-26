package com.example.demo.provider;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.web.context.ConfigurableWebApplicationContext;

@RunWith(PactRunner.class)
@Provider("test_provider")
@PactFolder("pacts")
public class PactTests {

  private static ConfigurableWebApplicationContext application;

  @BeforeClass
  public static void start() {
    application = (ConfigurableWebApplicationContext)
        SpringApplication.run(DemoProviderApplication.class);
  }

  @TestTarget
  public final Target target = new HttpTarget("http", "localhost", 8080, "/");

  @State({"test GET"})
  @Test
  public void testGet() {
  }

  @State({"test POST"})
  public void testPOST() {
  }
}
