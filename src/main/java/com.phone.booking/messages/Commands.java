package com.phone.booking.messages;

import akka.actor.typed.ActorRef;
import com.phone.booking.models.Phone;

import java.util.List;

public class Commands {

    public interface Command { }

    public record GetAllPhones(ActorRef<Messages.BookingResponse<?>> replyTo) implements Command { }

    public record GetPhoneDetails(String modelName, ActorRef<Messages.BookingResponse<?>> replyTo) implements Command { }

    public record BookPhone(String modelName, String bookedBy, ActorRef<Messages.BookingResponse<?>> replyTo) implements Command { }

    public record ReturnPhone(String modelName, ActorRef<Messages.BookingResponse<?>> replyTo) implements Command { }

}
