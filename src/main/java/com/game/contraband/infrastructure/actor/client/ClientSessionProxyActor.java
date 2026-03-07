package com.game.contraband.infrastructure.actor.client;

import com.game.contraband.infrastructure.actor.client.ClientSessionActor.ClientSessionCommand;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef;

public class ClientSessionProxyActor extends AbstractBehavior<ClientSessionCommand> {

    public static Behavior<ClientSessionCommand> create(EntityRef<ClientSessionCommand> entityRef) {
        return Behaviors.setup(context -> new ClientSessionProxyActor(context, entityRef));
    }

    private final EntityRef<ClientSessionCommand> entityRef;

    private ClientSessionProxyActor(
            ActorContext<ClientSessionCommand> context,
            EntityRef<ClientSessionCommand> entityRef
    ) {
        super(context);
        this.entityRef = entityRef;
    }

    @Override
    public Receive<ClientSessionCommand> createReceive() {
        return newReceiveBuilder().onAnyMessage(this::onForwardMessage)
                                  .build();
    }

    private Behavior<ClientSessionCommand> onForwardMessage(ClientSessionCommand command) {
        entityRef.tell(command);
        return this;
    }
}
