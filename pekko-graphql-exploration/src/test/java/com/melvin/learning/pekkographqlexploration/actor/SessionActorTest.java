package com.melvin.learning.pekkographqlexploration.actor;

import com.melvin.learning.pekkographqlexploration.actor.SessionActor.Balance;
import com.melvin.learning.pekkographqlexploration.actor.SessionActor.BetResult;
import com.melvin.learning.pekkographqlexploration.actor.SessionActor.Command;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionActorTest {

    @Test
    void callGetBalance_returnInitialBalance() {
        // Start a real actor system for this test.
        final ActorTestKit testKit = ActorTestKit.create();
        try {
            // Starts an actor using our behavior; returns its address.
            final ActorRef<Command> session = testKit.spawn(SessionActor.create(100));

            // A test recipient that accepts Balance messages.
            final TestProbe<Balance> replies =
                    testKit.createTestProbe(SessionActor.Balance.class);

            // Ask for the balance, supplying the recipient's address.
            session.tell(new SessionActor.GetBalance(replies.getRef()));

            // Wait for a reply and check its value.
            replies.expectMessage(new SessionActor.Balance(100));


        } finally {
            // Stops the actors and releases the system's resources.
            testKit.shutdownTestKit();
        }
    }

    @Test
    void callPlaceBet_acceptedBetReducesBalance() {
        final ActorTestKit testKit = ActorTestKit.create();
        try {
            final ActorRef<Command> session = testKit.spawn(SessionActor.create(100));

            final TestProbe<Balance> balanceTestProbe =
                    testKit.createTestProbe(SessionActor.Balance.class);
            final TestProbe<BetResult> betResultTestProbe =
                    testKit.createTestProbe(SessionActor.BetResult.class);

            session.tell(new SessionActor.PlaceBet(20, betResultTestProbe.getRef()));
            betResultTestProbe.expectMessage(new SessionActor.BetResult(true,80,null));

            session.tell(new SessionActor.GetBalance(balanceTestProbe.getRef()));
            balanceTestProbe.expectMessage(new SessionActor.Balance(80));
        } finally {
            testKit.shutdownTestKit();
        }
    }

    @Test
    void callPlaceBet_betGreaterThanBalance_declinedBetAndBalanceNotDeductd() {
        final ActorTestKit testKit = ActorTestKit.create();
        try {
            final ActorRef<Command> session = testKit.spawn(SessionActor.create(100));

            final TestProbe<Balance> balanceTestProbe =
                    testKit.createTestProbe(SessionActor.Balance.class);
            final TestProbe<BetResult> betResultTestProbe =
                    testKit.createTestProbe(SessionActor.BetResult.class);

            session.tell(new SessionActor.PlaceBet(110, betResultTestProbe.getRef()));
            betResultTestProbe.expectMessage(new SessionActor.BetResult(false,100,"INSUFFICIENT_CREDITS"));

            session.tell(new SessionActor.GetBalance(balanceTestProbe.getRef()));
            balanceTestProbe.expectMessage(new SessionActor.Balance(100));
        } finally {
            testKit.shutdownTestKit();
        }
    }

    @Test
    void placeBet_concurrentBetsCannotOverspend() throws Exception {
        final ActorTestKit testKit = ActorTestKit.create();

        try (final ExecutorService callers = Executors.newFixedThreadPool(20)) {
            final ActorRef<SessionActor.Command> session =
                    testKit.spawn(SessionActor.create(100));

            final TestProbe<SessionActor.BetResult> betReplies =
                    testKit.createTestProbe(SessionActor.BetResult.class);

            final ActorRef<SessionActor.BetResult> replyTo = betReplies.getRef();

            // Wait until all callers are ready, then release them together.
            final CountDownLatch ready = new CountDownLatch(20);
            final CountDownLatch start = new CountDownLatch(1);
            final List<Future<Void>> submissions = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                submissions.add(callers.submit(() -> {
                    ready.countDown();

                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start signal timed out");
                    }

                    session.tell(new SessionActor.PlaceBet(10, replyTo));
                    return null;
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Callers did not become ready");
            start.countDown();

            // Confirm every caller sent its message without failing.
            for (final Future<Void> submission : submissions) {
                submission.get(5, TimeUnit.SECONDS);
            }

            int accepted = 0;
            int rejected = 0;

            // Only this test thread reads from the probe.
            for (int i = 0; i < 20; i++) {
                final SessionActor.BetResult result =
                        betReplies.receiveMessage(Duration.ofSeconds(5));

                if (result.accepted()) {
                    accepted++;
                } else {
                    rejected++;
                    assertEquals("INSUFFICIENT_CREDITS", result.reason());
                }
            }

            assertEquals(10, accepted);
            assertEquals(10, rejected);

            final TestProbe<SessionActor.Balance> balanceReplies =
                    testKit.createTestProbe(SessionActor.Balance.class);

            session.tell(new SessionActor.GetBalance(balanceReplies.getRef()));
            balanceReplies.expectMessage(new SessionActor.Balance(0));
        } finally {
            testKit.shutdownTestKit();
        }
    }

}