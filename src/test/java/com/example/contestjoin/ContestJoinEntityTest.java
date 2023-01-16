package com.example.contestjoin;

import io.grpc.Status;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.springsdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
public class ContestJoinEntityTest {

    @Test
    public void happyPath(){

        var contestName = "contest1";
        var userName1 = "userName1";
        var userName2 = "userName2";
        var userName3 = "userName3";

        //Create testkit for one instance of contest
        EventSourcedTestKit<Model.State, ContestJoinEntity> testKit = EventSourcedTestKit.of(contestName, id -> new ContestJoinEntity(id));

        //Send join userName1
        EventSourcedResult<Model.EmptyResponse> res = testKit.call(controller -> controller.join(new Model.JoinContest(userName1)));
        //assert event emitted
        Model.ContestJoinProcessed event = res.getNextEventOfType(Model.ContestJoinProcessed.class);
        assertEquals(userName1,event.join().userName());
        //assert state updated
        Model.State updatedState = (Model.State)res.getUpdatedState();
        assertEquals(1,updatedState.processed().size());

        //Resend join for userName1 - test deduplication
        res = testKit.call(controller -> controller.join(new Model.JoinContest(userName1)));
        //event should not emit due to deduplication
        assertFalse(res.didEmitEvents());
        //state should not change
        Model.State state = (Model.State)res.getUpdatedState();
        assertEquals(1,updatedState.processed().size());

        //Send join for userName2
        res = testKit.call(controller -> controller.join(new Model.JoinContest(userName2)));
        //assert event emitted
        event = res.getNextEventOfType(Model.ContestJoinProcessed.class);
        assertEquals(userName2,event.join().userName());
        //assert state updated
        updatedState = (Model.State)res.getUpdatedState();
        assertEquals(2,updatedState.processed().size());

        //Send join for userName3 - no available spaces
        res = testKit.call(controller -> controller.join(new Model.JoinContest(userName3)));
        assertEquals(Status.Code.INVALID_ARGUMENT,res.getErrorStatusCode());
    }

}
