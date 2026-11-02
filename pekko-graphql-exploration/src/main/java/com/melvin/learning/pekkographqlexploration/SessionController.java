package com.melvin.learning.pekkographqlexploration;

import com.melvin.learning.pekkographqlexploration.actor.SessionActor;
import com.melvin.learning.pekkographqlexploration.actor.SessionActor.Balance;
import com.melvin.learning.pekkographqlexploration.actor.SessionActor.Command;
import lombok.RequiredArgsConstructor;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
@Controller
public class SessionController {

    private final ActorSystem<Command> sessionActorSystem;

    // Resolves the schema's Query.balance field.
    @QueryMapping
    public CompletionStage<Balance> balance() {
        return AskPattern.ask(
                sessionActorSystem,
                replyTo -> new SessionActor.GetBalance(replyTo),
                Duration.ofSeconds(3),
                sessionActorSystem.scheduler());
    }

}
