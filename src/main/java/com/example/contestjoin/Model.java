package com.example.contestjoin;

import kalix.springsdk.annotations.TypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public sealed interface Model {
    record JoinContest(String userName) implements Model {}
    record EmptyResponse() implements Model {
        public static EmptyResponse of(){
            return new EmptyResponse();
        }
    }

    record GetResponse(int maxAvailableSpots, List<JoinContest> processed, List<JoinContest> queued)implements Model{}



    record State(int maxAvailableSpots, List<JoinContest> processed) implements Model{
        public static State empty(int maxAvailableSpots){
            return new State(maxAvailableSpots,new ArrayList<>());
        }

        public State handleProcessedContestJoinBatch(ProcessedContestJoinBatch event){
            var newList = Stream.concat(processed().stream(), event.list.stream()).collect(Collectors.toList());
            return new State(maxAvailableSpots,newList);
        }
    }

    @TypeName("processed-contest-join-batch")
    record ProcessedContestJoinBatch(List<Model.JoinContest> list) implements Model{}
}
