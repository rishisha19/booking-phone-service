package com.phone.booking.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Phone {
    private final String modelName;
    private boolean available;
    private String bookingDate;
    private String bookedBy;
    private String technology;
    private String _2g_bands;
    private String _3g_bands;
    private String _4g_bands;
}
