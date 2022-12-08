package com.example.contestjoin;

import io.grpc.Status;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.springsdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
public class ContestJoinControllerTest {

    @Test
    public void happyPath(){

//        var config = new Config(2,5);
        var contestName = "contest1";
        var requests = IntStream.range(0,6).mapToObj(i -> "username-"+i).map(username -> new Model.JoinContest(username)).collect(Collectors.toList());

        EventSourcedTestKit<Model.State, ContestJoinController> testKit = EventSourcedTestKit.of(contestName, id -> new ContestJoinController(id/*,config*/));

        EventSourcedResult<Model.EmptyResponse> res = testKit.call(controller -> controller.join(requests.get(0)));
        assertFalse(res.didEmitEvents());

        res = testKit.call(controller -> controller.join(requests.get(1)));
        Model.ProcessedContestJoinBatch event = res.getNextEventOfType(Model.ProcessedContestJoinBatch.class);
        assertEquals(2,event.list().size());
        Model.State state = (Model.State)res.getUpdatedState();
        assertEquals(2,state.processed().size());

        //check duplicate
        res = testKit.call(controller -> controller.join(requests.get(1)));
        assertFalse(res.didEmitEvents());
        assertEquals(Status.Code.ALREADY_EXISTS,res.getErrorStatusCode());

        res = testKit.call(controller -> controller.join(requests.get(2)));
        res = testKit.call(controller -> controller.join(requests.get(3)));
        event = res.getNextEventOfType(Model.ProcessedContestJoinBatch.class);
        assertEquals(2,event.list().size());
        state = (Model.State)res.getUpdatedState();
        assertEquals(4,state.processed().size());

        res = testKit.call(controller -> controller.join(requests.get(4)));
        event = res.getNextEventOfType(Model.ProcessedContestJoinBatch.class);
        assertEquals(1,event.list().size());
        state = (Model.State)res.getUpdatedState();
        assertEquals(5,state.processed().size());

        res = testKit.call(controller -> controller.join(requests.get(5)));
        assertFalse(res.didEmitEvents());
        assertEquals(Status.Code.INVALID_ARGUMENT,res.getErrorStatusCode());

    }

}
