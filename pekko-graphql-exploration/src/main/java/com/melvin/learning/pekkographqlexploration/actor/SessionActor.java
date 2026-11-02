package com.melvin.learning.pekkographqlexploration.actor;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

public class SessionActor extends AbstractBehavior<SessionActor.Command> {

    // Allowed incoming message types
    public sealed interface Command permits GetBalance,PlaceBet {
    }

    // Reply containing the session's credits
    public record Balance(int credits) {
    }

    // Request containing the address to send the reply to.
    public record GetBalance(ActorRef<Balance> replyTo) implements Command {
    }

    // Outcome containing acceptance, remaining credits, and rejection reason.
    public record BetResult(
            boolean accepted,
            int balance,
            String reason) {}

    // Request to spend credits, including the reply address.
    public record PlaceBet(int amount, ActorRef<BetResult> replyTo)
            implements Command {}

    // Mutable state accessed only by this actor's message handlers.
    private int balance;

    // Creates a fresh behavior when Pekko starts the actor.
    public static Behavior<Command> create(final int initialCredits) {
        if (initialCredits < 0) {
            throw new IllegalArgumentException(
                    "Initial credits must not be negative");
        }

        return Behaviors.setup(
                context -> new SessionActor(context, initialCredits));
    }

    // Pekko supplies the context; we supply the starting credits.
    private SessionActor(
            final ActorContext<Command> context, final int initialCredits) {

        super(context);
        this.balance = initialCredits;
    }

    // Maps incoming message types to their handlers.
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetBalance.class, this::onGetBalance)
                .onMessage(PlaceBet.class, this::onPlaceBet)
                .build();
    }

    // Sends the balance to the request's reply address.
    private Behavior<Command> onGetBalance(final GetBalance command) {
        command.replyTo().tell(new Balance(balance));
        return this;
    }

    // Checks and deducts credits while processing one message.
    private Behavior<Command> onPlaceBet(final PlaceBet command) {
        if (command.amount() <= 0) {
            command.replyTo().tell(
                    new BetResult(false, balance, "INVALID_AMOUNT"));
            return this;
        }

        if (command.amount() > balance) {
            command.replyTo().tell(
                    new BetResult(false, balance, "INSUFFICIENT_CREDITS"));
            return this;
        }

        balance = balance - command.amount();

        command.replyTo().tell(new BetResult(true, balance, null));
        return this;
    }
}
