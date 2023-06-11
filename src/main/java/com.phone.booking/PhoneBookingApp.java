package com.phone.booking;

import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.server.Route;
import com.phone.booking.messages.Commands;
import com.phone.booking.routes.BookingsRoute;
import com.phone.booking.service.BookingService;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletionStage;

public class PhoneBookingApp {
    // #start-http-server
    static void startHttpServer(Route route, ActorSystem<?> system) {
        CompletionStage<ServerBinding> futureBinding =
                Http.get(system).newServerAt("localhost", 8080).bind(route);

        futureBinding.whenComplete((binding, exception) -> {
            if (binding != null) {
                InetSocketAddress address = binding.localAddress();
                system.log().debug("Server online at http://{}:{}/",
                        address.getHostString(),
                        address.getPort());
            } else {
                system.log().error("Failed to bind HTTP endpoint, terminating system", exception);
                system.terminate();
            }
        });
    }
    // #start-http-server

    public static void main(String[] args) {
        //#server-bootstrapping
        Behavior<NotUsed> rootBehavior = Behaviors.setup(context -> {
            ActorRef<Commands.Command> bookingServiceActor =
                    context.spawn(BookingService.create(context.getSystem().settings()
                                    .config().getStringList("my-app.phones")), "BookingService");

            BookingsRoute bookingsRoute = new BookingsRoute(context.getSystem(), bookingServiceActor);
            startHttpServer(bookingsRoute.routes(), context.getSystem());

            return Behaviors.empty();
        });

        // boot up server using the route as defined below
        ActorSystem.create(rootBehavior, "phone-booking");
        //#server-bootstrapping
    }
}
