package com.phone.booking.service;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.Materializer;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phone.booking.models.Phone;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class FetchPhoneExternalDetailsService extends AbstractBehavior<FetchPhoneExternalDetailsService.PhoneCommand> {
    private final String API_URL;
    private final String API_TOKEN;
    private final boolean isAVAILABLE;
    public interface PhoneCommand {}

    public record GetExternalDetails(Phone phone, ActorRef<Object> replyTo) implements PhoneCommand { }

    public record PhoneDetails(String model, String technology, String _2g_bands, String _3g_bands, String _4g_bands) { }

    private final Materializer materializer;
    final ObjectMapper mapper;

    public FetchPhoneExternalDetailsService(ActorContext<PhoneCommand> context, Materializer materializer) {
        super(context);
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        API_URL = context.getSystem().settings().config().getString("my-app.fonoapi.api");
        API_TOKEN = context.getSystem().settings().config().getString("my-app.fonoapi.token");
        isAVAILABLE = context.getSystem().settings().config().getBoolean("my-app.fonoapi.available");
        this.materializer = materializer;
    }

    public static Behavior<FetchPhoneExternalDetailsService.PhoneCommand> create(Materializer materializer) {
        return Behaviors.setup(ctx -> {
            ctx.setLoggerName(FetchPhoneExternalDetailsService.class.getName());
            ctx.getLog().info("Starting up");
            return new FetchPhoneExternalDetailsService(ctx, materializer);
        });
    }

    @Override
    public Receive<PhoneCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetExternalDetails.class, this::onGetPhoneDetails)
                .build();
    }

    private Behavior<PhoneCommand> onGetPhoneDetails(GetExternalDetails command) {
        ActorRef<Object> replyTo = command.replyTo();
        getContext().getLog().debug("Fetching details from fonoapi for model: {}", command.phone.getModelName());
        if (isAVAILABLE) {
            fetchPhoneDetailsApi(command.phone)
                    .whenComplete((phoneDetails, ex) -> {
                        if (phoneDetails != null) {
                            replyTo.tell(phoneDetails);
                        } else {
                            getContext().getLog().error("Failed to fetch phone details");
                            replyTo.tell(null);
                        }
                    });
        } else {
            fetchPhoneDetails(command);
        }

        return Behaviors.same();
    }

    private CompletionStage<Phone> fetchPhoneDetailsApi(Phone phone) {
        getContext().getLog().debug("calling fonoapi");
        HttpRequest request = HttpRequest.POST(API_URL)
                .withEntity(ContentTypes.APPLICATION_JSON,
                        ByteString.fromString(String.format("{\"token\":\"" + API_TOKEN + "\",\"device\":\"%s\"}",
                                phone.getModelName())));

        return Http.get(getContext().getSystem()).singleRequest(request)
                .thenCompose(response -> {
                    if (response.status().equals(StatusCodes.OK)) {
                        Unmarshaller<HttpEntity, Phone> unmarshaller = Unmarshaller.entityToString()
                                .thenApply(jsonString -> {

                                    try {
                                        PhoneDetails phoneDetails = mapper.readValue(jsonString, PhoneDetails.class);
                                        phone.set_2g_bands(phoneDetails._2g_bands);
                                        phone.set_3g_bands(phoneDetails._3g_bands);
                                        phone.set_4g_bands(phoneDetails._4g_bands);
                                        phone.setTechnology(phoneDetails.technology);
                                    } catch (JsonProcessingException ignored) {}
                                    return phone;
                                });
                        return unmarshaller.unmarshal(response.entity(), materializer);
                    } else {
                        throw new RuntimeException("Failed to fetch phone details: " + response.status());
                    }
                });
    }

    private void fetchPhoneDetails(GetExternalDetails command){
        try{
            List<PhoneDetails> phoneDetailsList = mapper.readValue(Files.readString(
                    Path.of("fonoData/phoneDetails.json")), new TypeReference<>() {
            });
            phoneDetailsList.stream()
                    .filter(phoneDetails -> phoneDetails.model.equals(command.phone.getModelName()))
                    .findFirst()
                    .ifPresent(phoneDetails -> {
                        command.phone.setTechnology(phoneDetails.technology);
                        command.phone.set_2g_bands(phoneDetails._2g_bands);
                        command.phone.set_3g_bands(phoneDetails._3g_bands);
                        command.phone.set_4g_bands(phoneDetails._4g_bands);
                    });
           command.replyTo.tell(command.phone);
        } catch (Exception exception){
            getContext().getLog().error("Error during reading from file: ", exception);
        }

    }
}
