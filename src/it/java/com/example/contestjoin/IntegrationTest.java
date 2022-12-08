package com.example.contestjoin;

import com.example.contestjoin.Main;
import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;


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
    var requests = IntStream.range(0,6).mapToObj(i -> "username-"+i).map(username -> new Model.JoinContest(username)).collect(Collectors.toList());

    ResponseEntity<Model.EmptyResponse> resProcess = sendJoin(contestName,requests.get(0));
    assertEquals(HttpStatus.OK,resProcess.getStatusCode());

    ResponseEntity<Model.GetResponse> getRes = getState(contestName);
    assertEquals(1,getRes.getBody().queued().size());
    assertEquals(0,getRes.getBody().processed().size());

    resProcess = sendJoin(contestName,requests.get(1));
    assertEquals(HttpStatus.OK,resProcess.getStatusCode());

    getRes = getState(contestName);
    assertEquals(0,getRes.getBody().queued().size());
    assertEquals(2,getRes.getBody().processed().size());

    resProcess = sendJoin(contestName,requests.get(2));
    assertEquals(HttpStatus.OK,resProcess.getStatusCode());
    resProcess = sendJoin(contestName,requests.get(3));
    assertEquals(HttpStatus.OK,resProcess.getStatusCode());

    getRes = getState(contestName);
    assertEquals(0,getRes.getBody().queued().size());
    assertEquals(4,getRes.getBody().processed().size());

    resProcess = sendJoin(contestName,requests.get(4));
    assertEquals(HttpStatus.OK,resProcess.getStatusCode());

//    resProcess = sendJoin(contestName,requests.get(4));
//    assertEquals(HttpStatus.ALREADY_REPORTED,resProcess.getStatusCode());

    getRes = getState(contestName);
    assertEquals(0,getRes.getBody().queued().size());
    assertEquals(5,getRes.getBody().processed().size());

//    resProcess = sendJoin(contestName,requests.get(5));
//    assertEquals(HttpStatus.BAD_REQUEST,resProcess.getStatusCode());


  }

  private ResponseEntity<Model.EmptyResponse> sendJoin(String contestName, Model.JoinContest request){
    return webClient.post().uri("/contest/"+contestName+"/join")
            .bodyValue(request)
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