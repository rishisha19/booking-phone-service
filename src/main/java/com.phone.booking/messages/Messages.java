package com.phone.booking.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.phone.booking.Status;

public class Messages {

    public record BookingRequest(String modelName, String bookedBy) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookingResponse<T>(Status status, T data){ }
}
