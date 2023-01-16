package com.example.contestjoin;

import io.grpc.Status;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@EntityKey("contestName")
@EntityType("contest")
@RequestMapping("/contest/{contestName}")
public class ContestJoinEntity extends EventSourcedEntity<Model.State> {

    private static Logger logger = LoggerFactory.getLogger(ContestJoinEntity.class);
    private final String contestName;
    private final static int maxAvailableSpots = 2;

    public ContestJoinEntity(EventSourcedEntityContext context) {
        this.contestName = context.entityId();
    }

    @Override
    public Model.State emptyState() {
        return Model.State.empty(maxAvailableSpots);
    }

    @PostMapping("/join")
    public Effect<Model.EmptyResponse> join(@RequestBody Model.JoinContest request){
        //check if duplicate request
        if(currentState().checkDuplicate(request)){
            return effects().reply(Model.EmptyResponse.of());
        }
        //check if contest is already full
        if(currentState().processed().size() == maxAvailableSpots){
            return effects().error("NO available spots.", Status.Code.INVALID_ARGUMENT);
        }
        var event = new Model.ContestJoinProcessed(request);
        return effects().emitEvent(event).thenReply(newState -> Model.EmptyResponse.of());
    }

    @GetMapping
    public Effect<Model.GetResponse> get(){
        return effects().reply(new Model.GetResponse(currentState().maxAvailableSpots(), currentState().processed()));
    }

    @EventHandler
    public Model.State onContestJoinProcessed(Model.ContestJoinProcessed event){
        return currentState().handleContestJoinProcessed(event);
    }

}
