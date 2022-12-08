package com.example.contestjoin;

import io.grpc.Status;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@EntityKey("contestName")
@EntityType("contest")
@RequestMapping("/contest/{contestName}")
public class ContestJoinController extends EventSourcedEntity<Model.State> {

    private static Logger logger = LoggerFactory.getLogger(ContestJoinController.class);
    private final String contestName;
    private final Config config = new Config(2,5);
    private List<Model.JoinContest> batchQueue = new ArrayList<>();

    @Autowired
    public ContestJoinController(EventSourcedEntityContext context/*, Config config*/) {
        this.contestName = context.entityId();
//        this.config = config;
    }

    @Override
    public Model.State emptyState() {
        return Model.State.empty(config.getMaxAvailableSpots());
    }

    @PostMapping("/join")
    public Effect<Model.EmptyResponse> join(@RequestBody Model.JoinContest request){
        //check if duplicate request
        if(currentState().processed().stream().filter(cj -> cj.userName().equalsIgnoreCase(request.userName())).findFirst().isPresent()){
            return effects().error("Duplicate request.", Status.Code.ALREADY_EXISTS);
        }
        //check if contest is already full
        if(currentState().processed().size() == config.getMaxAvailableSpots()){
            return effects().error("NO available spots.", Status.Code.INVALID_ARGUMENT);
        }
        //add to local queue for batching
        batchQueue.add(request);

        int occupiedSpots = currentState().processed().size()+batchQueue.size();
        logger.info("[{}] occupiedSpots: {}({}), batchQueue size: {}({})",contestName,occupiedSpots,config.getMaxAvailableSpots(),batchQueue.size(),config.getBatchSize());
        //if with this request all remaining spots are empty or batch size reaches the defined one, persist the batch event
        if(occupiedSpots == config.getMaxAvailableSpots() || batchQueue.size()==config.getBatchSize()){
            var event = new Model.ProcessedContestJoinBatch(List.copyOf(batchQueue));
            batchQueue.clear();
            return effects().emitEvent(event).thenReply(newState -> Model.EmptyResponse.of());
        } else {
            return effects().reply(Model.EmptyResponse.of());
        }
    }

    @GetMapping
    public Effect<Model.GetResponse> get(){
        return effects().reply(new Model.GetResponse(currentState().maxAvailableSpots(), currentState().processed(),batchQueue));
    }

    @EventHandler
    public Model.State onProcessedContestJoinBatch(Model.ProcessedContestJoinBatch event){
        return currentState().handleProcessedContestJoinBatch(event);
    }

}
