package com.phone.booking.routes;


//#test-top

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import com.phone.booking.messages.Commands;
import com.phone.booking.service.BookingService;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BookingsRouteTest extends JUnitRouteTest {

    private ActorTestKit testKit;
    ActorSystem<Void> system;
    ActorRef<Commands.Command> bookingServiceActor;
    TestProbe<Commands.Command> probe;
    TestRoute appRoute;

    @Before
    public void setup() {

            System.out.println("Initializing");
            system = Adapter.toTyped(system());
            probe = TestProbe.create(system);
            testKit = ActorTestKit.create(system);
            bookingServiceActor = testKit.spawn(BookingService.create(system.settings().config().getStringList("my-app.phones")));
            BookingsRoute bookingsRoute = new BookingsRoute(system, bookingServiceActor);
            appRoute = testRoute(bookingsRoute.routes());
    }

    @After
    public void tearDown() {

        system.terminate();
        testKit.shutdownTestKit();
    }

    @Test
    public void testCase1_GetAllPhones() {
        appRoute.run(HttpRequest.GET("/phones"))
                .assertStatusCode(StatusCodes.OK)
                .assertEntity("{\"data\":[{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"Motorola Nexus 6\",\"technology\":\"\"},{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"Nokia 3310\",\"technology\":\"\"},{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"Samsung Galaxy S9\",\"technology\":\"\"},{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"Oneplus 9\",\"technology\":\"\"},{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"iPhone X\",\"technology\":\"\"},{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"Apple iPhone 11\",\"technology\":\"\"},{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"2x Samsung Galaxy S8\",\"technology\":\"\"},{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"Apple iPhone 13\",\"technology\":\"\"},{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"Apple iPhone 12\",\"technology\":\"\"}],\"status\":\"success\"}");
    }

    @Test
    public void testCase2_GetPhoneDetailsByModelName() {
        appRoute.run(HttpRequest.GET("/phones/Samsung%20Galaxy%20S9"))
                .assertStatusCode(StatusCodes.OK)
                .assertEntity("{\"data\":{\"_2g_bands\":\"\",\"_3g_bands\":\"\",\"_4g_bands\":\"\",\"available\":true,\"modelName\":\"Samsung Galaxy S9\",\"technology\":\"\"},\"status\":\"success\"}");
    }

    @Test
    public void testCase3_GetPhoneDetails_NotFound_By_InvalidModelName() {
        appRoute.run(HttpRequest.GET("/phones/Samsung%20Galaxy"))
                .assertStatusCode(StatusCodes.NOT_FOUND)
                .assertEntity("{\"data\":\"Phone not found.\",\"status\":\"notFound\"}");
    }

    @Test
    public void testCase4_BookPhone() {
        appRoute.run(HttpRequest.PUT("/phones/book")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(), "{\"bookedBy\":\"John Doe\", \"modelName\": \"Motorola Nexus 6\"}"))
                .assertStatusCode(StatusCodes.OK)
                .assertEntity("{\"data\":\"Phone booked successfully.\",\"status\":\"success\"}");
    }

    @Test
    public void testCase5_AlreadyBookedPhone() {
        appRoute.run(HttpRequest.PUT("/phones/book")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(), "{\"bookedBy\":\"John Doe\", \"modelName\": \"Motorola Nexus 6\"}"))
                .assertStatusCode(StatusCodes.OK)
                .assertEntity("{\"data\":\"Phone booked successfully.\",\"status\":\"success\"}");

        appRoute.run(HttpRequest.PUT("/phones/book")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),"{\"bookedBy\":\"John Doe\", \"modelName\": \"Motorola Nexus 6\"}"))
                .assertStatusCode(StatusCodes.BAD_REQUEST)
                .assertEntity("{\"data\":\"Phone is already booked.\",\"status\":\"unavailable\"}");
    }

    @Test
    public void testCase5_BookPhone_InvalidPhone(){
        appRoute.run(HttpRequest.PUT("/phones/book")
                        .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),"{\"bookedBy\":\"John Doe\", \"modelName\": \"Invalid Model\"}"))
                .assertStatusCode(StatusCodes.NOT_FOUND)
                .assertEntity("{\"data\":\"Phone not found.\",\"status\":\"notFound\"}");
    }

    @Test
    public void testCase6_ReturnPhone_InvalidPhone(){
        appRoute.run(HttpRequest.PUT("/phones/return/Invalid%20Model"))
                .assertStatusCode(StatusCodes.NOT_FOUND)
                .assertEntity("{\"data\":\"Phone not found.\",\"status\":\"notFound\"}");
    }

    @Test
    public void testCase7_ReturnPhone(){
        appRoute.run(HttpRequest.PUT("/phones/return/Motorola%20Nexus%206"))
                .assertStatusCode(StatusCodes.OK)
                .assertEntity("{\"data\":\"Thank you!\",\"status\":\"success\"}");
    }

}