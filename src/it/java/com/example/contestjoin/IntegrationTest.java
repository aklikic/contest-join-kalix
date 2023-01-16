package com.example.contestjoin;

import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * This is a skeleton for implementing integration tests for a Kalix application built with the Spring SDK.
 *
 * This test will initiate a Kalix Proxy using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 *
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */
@TestPropertySource(locations="classpath:test.properties")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class IntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(5, ChronoUnit.SECONDS);

  @Test
  public void happyPath() throws Exception {
    var contestName = "contest1";
    var userName1 = "userName1";
    var userName2 = "userName2";
    var userName3 = "userName3";

    //Send join for userName1
    ResponseEntity<Model.EmptyResponse> resProcess = sendJoin(contestName,userName1);
    assertEquals(HttpStatus.OK,resProcess.getStatusCode());
    ResponseEntity<Model.GetResponse> getRes = getState(contestName);
    assertEquals(1,getRes.getBody().processed().size());

    //Re-send join for userName1 - deduplication
    resProcess = sendJoin(contestName,userName1);
    assertEquals(HttpStatus.OK,resProcess.getStatusCode());
    getRes = getState(contestName);
    assertEquals(1,getRes.getBody().processed().size());

    //Send join for userName2
    resProcess = sendJoin(contestName,userName2);
    assertEquals(HttpStatus.OK,resProcess.getStatusCode());
    getRes = getState(contestName);
    assertEquals(2,getRes.getBody().processed().size());

    //Send join for userName3

    assertThrows(WebClientResponseException.BadRequest.class,()->sendJoin(contestName,userName3));

  }

  private ResponseEntity<Model.EmptyResponse> sendJoin(String contestName, String userName){
    return webClient.post().uri("/contest/"+contestName+"/join")
            .bodyValue(new Model.JoinContest(userName))
            .retrieve()
            .toEntity(Model.EmptyResponse.class)
            .block(timeout);
  }

  private ResponseEntity<Model.GetResponse> getState(String contestName){
    return webClient.get().uri("/contest/"+contestName)
            .retrieve()
            .toEntity(Model.GetResponse.class)
            .block(timeout);
  }
}