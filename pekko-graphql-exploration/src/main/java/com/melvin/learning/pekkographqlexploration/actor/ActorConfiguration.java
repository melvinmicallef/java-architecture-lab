package com.melvin.learning.pekkographqlexploration.actor;

import org.apache.pekko.actor.typed.ActorSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorConfiguration {

    @Bean(destroyMethod = "terminate")
    public ActorSystem<SessionActor.Command> sessionActorSystem(){
        return ActorSystem.create(SessionActor.create(100),"session-system");
    }

}
