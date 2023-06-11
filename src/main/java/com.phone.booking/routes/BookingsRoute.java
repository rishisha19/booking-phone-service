package com.phone.booking.routes;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.directives.RouteAdapter;
import com.phone.booking.messages.Commands;
import com.phone.booking.messages.Messages;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

@Slf4j
public class BookingsRoute extends AllDirectives {

    private final ActorRef<Commands.Command> bookingServiceActor;
    private final Duration askTimeout;
    private final Scheduler scheduler;

    public BookingsRoute(ActorSystem<?> system, ActorRef<Commands.Command> bookingServiceActor) {
        system.log().debug("Initializing routes");
        this.bookingServiceActor = bookingServiceActor;
        scheduler = system.scheduler();
        askTimeout = system.settings().config().getDuration("my-app.routes.ask-timeout");
    }

    public Route routes() {
        return pathPrefix("phones", () ->
                concat(
                        pathEndOrSingleSlash(() ->
                                concat(
                                        get(() -> onSuccess(getAllPhones(), this::handleResponse)
                                        )
                                )
                        ),
                        path("book", () ->
                                    put(() ->
                                            entity(Jackson.unmarshaller(Messages.BookingRequest.class), request ->
                                                    onSuccess(bookPhone(request), this::handleResponse)
                                            )
                                    )
                                ),
                        path(PathMatchers.segment("return").slash(PathMatchers.segment()), modelName ->
                                put(() ->
                                        onSuccess(returnPhone(modelName), this::handleResponse)
                                )),
                        path(PathMatchers.segment(), modelName ->
                                get(() -> onSuccess(getPhoneDetails(modelName), this::handleResponse)
                                )
                        )
                )
        );
    }

    private CompletionStage<Messages.BookingResponse<?>> getAllPhones() {
        log.debug("getAllPhones");
        return AskPattern.ask(bookingServiceActor, Commands.GetAllPhones::new, askTimeout, scheduler);
    }

    private CompletionStage<Messages.BookingResponse<?>> getPhoneDetails(String modelName) {
        return AskPattern.ask(bookingServiceActor, ref -> new Commands.GetPhoneDetails(modelName, ref), askTimeout, scheduler);
    }

    private CompletionStage<Messages.BookingResponse<?>> bookPhone(Messages.BookingRequest request) {
        return AskPattern.ask(bookingServiceActor, ref ->
                new Commands.BookPhone(request.modelName(), request.bookedBy(), ref), askTimeout, scheduler);
    }

    private CompletionStage<Messages.BookingResponse<?>> returnPhone(String modelName) {

        return AskPattern.ask(bookingServiceActor, ref -> new Commands.ReturnPhone(modelName, ref), askTimeout, scheduler);
    }

    private RouteAdapter handleResponse(Messages.BookingResponse<?> bookingResponse) {
        return switch (bookingResponse.status()) {
            case success -> complete(StatusCodes.OK, bookingResponse, Jackson.marshaller());
            case notFound -> complete(StatusCodes.NOT_FOUND, bookingResponse, Jackson.marshaller());
            case unavailable -> complete(StatusCodes.BAD_REQUEST, bookingResponse, Jackson.marshaller());
            default -> complete(StatusCodes.INTERNAL_SERVER_ERROR, bookingResponse, Jackson.marshaller());
        };
    }

}