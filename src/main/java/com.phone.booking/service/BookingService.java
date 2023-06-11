package com.phone.booking.service;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.*;
import akka.stream.Materializer;
import com.phone.booking.messages.Commands;
import com.phone.booking.messages.Messages;
import com.phone.booking.models.Phone;
import com.phone.booking.service.FetchPhoneExternalDetailsService.GetExternalDetails;
import com.phone.booking.service.FetchPhoneExternalDetailsService.PhoneCommand;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.phone.booking.Status.*;

public class BookingService extends AbstractBehavior<Commands.Command> {

    private final Map<String, Phone> phoneStore = new HashMap<>();
    private final Duration askTimeout;
    private final Scheduler scheduler;

    public static Behavior<Commands.Command> create(List<String> phones) {
        Objects.requireNonNull(phones);
        return Behaviors.setup(ctx -> {
            ctx.setLoggerName(BookingService.class.getName());
            ctx.getLog().info("Starting up");
            return new BookingService(ctx, phones);
        });
    }

    private BookingService(ActorContext<Commands.Command> context, List<String> phones) {
        super(context);
        context.getLog().debug("booking booking actor started");
        scheduler = context.getSystem().scheduler();
        askTimeout =  context.getSystem().settings().config().getDuration("my-app.routes.ask-timeout");
        ActorRef<PhoneCommand> externalApiServiceActor = context.spawn(FetchPhoneExternalDetailsService
                        .create(Materializer.createMaterializer(context.getSystem().classicSystem())), "details-api");
        initializePhoneStore( phones, externalApiServiceActor);
    }

    private void initializePhoneStore(List<String> phones,
                                                    ActorRef<PhoneCommand> externalApiServiceActor) {
        phones.stream()
                .map(String::trim)
                .map(model -> Phone.builder().modelName(model).available(true).build())
                .forEach(phone -> {
                        AskPattern.ask(externalApiServiceActor, ref -> new GetExternalDetails(phone, ref), askTimeout, scheduler)
                                .whenComplete((response, exception) -> {
                                    if (response != null)
                                        phoneStore.put(phone.getModelName(), (Phone) response);
                                    else
                                        phoneStore.put(phone.getModelName(), phone);
                                });
                        phoneStore.put(phone.getModelName(), phone);
                });
                //.collect(Collectors.toMap(Phone::getModelName, Function.identity()));
    }

    // This receive handles all possible incoming messages and keeps the state in the actor
    @Override
    public Receive<Commands.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Commands.BookPhone.class, this::bookPhone)
                .onMessage(Commands.GetAllPhones.class, this::getAllPhones)
                .onMessage(Commands.ReturnPhone.class, this::returnPhone)
                .onMessage(Commands.GetPhoneDetails.class, this::getPhoneDetails)
                .build();
    }

    private Behavior<Commands.Command> getPhoneDetails(Commands.GetPhoneDetails msg) {
        getContext().getLog().debug("getPhoneDetails for phones: {}", msg.modelName());
        if (phoneStore.containsKey(msg.modelName()))
            msg.replyTo().tell(new Messages.BookingResponse<>(success, phoneStore.get(msg.modelName())));
        else {
            msg.replyTo().tell(new Messages.BookingResponse<>(notFound, "Phone not found."));
        }
        return Behaviors.same();
    }


    private Behavior<Commands.Command> bookPhone(Commands.BookPhone msg) {
        getContext().getLog().debug("bookPhone request for phone: {}", msg.modelName());
        if (phoneStore.containsKey(msg.modelName())) {
            Phone phone = phoneStore.get(msg.modelName());
            if (phone.isAvailable()) {
                phone.setBookedBy(msg.bookedBy());
                phone.setAvailable(false);
                phone.setBookingDate(LocalDateTime.now().toString());
                phoneStore.put(msg.modelName(), phone);
                msg.replyTo().tell(new Messages.BookingResponse<>(success, "Phone booked successfully."));
            } else {
                msg.replyTo().tell(new Messages.BookingResponse<>(unavailable, "Phone is already booked."));
            }
        } else {
            msg.replyTo().tell(new Messages.BookingResponse<>(notFound, "Phone not found."));
        }
        return Behaviors.same();
    }

    private Behavior<Commands.Command> getAllPhones(Commands.GetAllPhones msg) {
        getContext().getLog().debug("getAllPhones request for all phones");
        msg.replyTo().tell(new Messages.BookingResponse<>(success, new ArrayList<>(phoneStore.values())));
        return Behaviors.same();
    }

    private Behavior<Commands.Command> returnPhone(Commands.ReturnPhone msg) {
        getContext().getLog().debug("returnPhone request for phone: {}", msg.modelName());
        if (phoneStore.containsKey(msg.modelName())) {
            Phone phone = phoneStore.get(msg.modelName());
            phone.setBookedBy(null);
            phone.setBookingDate(null);
            phone.setAvailable(true);
            phoneStore.put(msg.modelName(), phone);
            msg.replyTo().tell(new Messages.BookingResponse<>(success, "Thank you!"));
        } else {
            msg.replyTo().tell(new Messages.BookingResponse<>(notFound, "Phone not found."));
        }
        return Behaviors.same();
    }
}
