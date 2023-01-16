package com.example.contestjoin;

import kalix.springsdk.annotations.TypeName;

import java.util.ArrayList;
import java.util.List;

public sealed interface Model {
    record JoinContest(String userName) implements Model {}
    record EmptyResponse() implements Model {
        public static EmptyResponse of(){
            return new EmptyResponse();
        }
    }

    record GetResponse(int maxAvailableSpots, List<JoinContest> processed)implements Model{}


    record State(int maxAvailableSpots, List<JoinContest> processed) implements Model{
        public static State empty(int maxAvailableSpots){
            return new State(maxAvailableSpots,new ArrayList<>());
        }

        public State handleContestJoinProcessed(ContestJoinProcessed event){
            processed.add(event.join());
            return new State(maxAvailableSpots,processed);
        }

        public boolean checkDuplicate(JoinContest join){
            return processed().stream().filter(cj -> cj.userName().equalsIgnoreCase(join.userName())).findFirst().isPresent();
        }

    }

    @TypeName("contest-join-processed")
    record ContestJoinProcessed(Model.JoinContest join) implements Model{}
}
