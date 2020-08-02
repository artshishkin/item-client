package com.artarkatesoft.learnreactivespring.controllers;

import com.artarkatesoft.learnreactivespring.domain.Item;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.artarkatesoft.learnreactivespring.constants.ItemConstants.ITEM_END_POINT_V1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.*;

@ExtendWith(MockitoExtension.class)
class ItemClientControllerTest {

    private static MockWebServer mockBackEnd;

    private Item defaultItem;
    private Flux<Item> repositoryFlux;
    private List<Item> itemsInRepo;
    private ObjectMapper objectMapper;
    private ItemClientController controller;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s",
                mockBackEnd.getPort());
        controller = new ItemClientController();
        controller.setItemServerUrl(baseUrl);

        defaultItem = new Item("MyId", "desc4", 123.99);
        itemsInRepo = IntStream
                .rangeClosed(1, 5)
                .mapToObj(i -> new Item("id" + i, "desc" + i, (double) (i * 111)))
                .collect(Collectors.toList());
        itemsInRepo.add(defaultItem);
        repositoryFlux = Flux.fromIterable(itemsInRepo);
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllItemsUsingRetrieve() throws JsonProcessingException, InterruptedException {
        //given
        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(itemsInRepo))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        //when
        Flux<Item> itemFlux = controller.getAllItemsUsingRetrieve();

        //then
        StepVerifier.create(itemFlux)
                .expectSubscription()
                .expectNextCount(6)
                .verifyComplete();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertEquals("GET", recordedRequest.getMethod());
        assertEquals(ITEM_END_POINT_V1, recordedRequest.getPath());
    }

    @Test
    void getAllItemsUsingExchange() throws JsonProcessingException, InterruptedException {
        //given
        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(itemsInRepo))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        //when
        Flux<Item> itemFlux = controller.getAllItemsUsingExchange();

        //then
        StepVerifier.create(itemFlux)
                .expectSubscription()
                .thenConsumeWhile(
                        item -> true,
                        item -> assertThat(item).isIn(itemsInRepo))
                .verifyComplete();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertEquals("GET", recordedRequest.getMethod());
        assertEquals(ITEM_END_POINT_V1, recordedRequest.getPath());
    }

    @Test
    void getOneItemUsingRetrieve() throws JsonProcessingException, InterruptedException {
        //given
        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(defaultItem))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        //when
        Mono<Item> itemMono = controller.getOneItemUsingRetrieve("MyId");

        //then
        StepVerifier.create(itemMono)
                .expectSubscription()
                .expectNext(defaultItem)
                .verifyComplete();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1 + "/MyId");
    }

    @Test
    void getOneItemUsingExchange() throws JsonProcessingException, InterruptedException {
        //given
        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(defaultItem))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        //when
        Mono<Item> itemMono = controller.getOneItemUsingExchange("MyId");

        //then
        StepVerifier.create(itemMono)
                .expectSubscription()
                .expectNext(defaultItem)
                .verifyComplete();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1 + "/MyId");
    }

    @Test
    void postOneItem() throws JsonProcessingException, InterruptedException {
        //given
        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(defaultItem))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .addHeader(LOCATION, "http://localhost:8080/v1/item/MyId"));
        Item itemSent = new Item(null, "descToSet", 9.01);
        //when
        Mono<Item> itemMono = controller.postOneItem("descToSet", 9.01);

        //then
        StepVerifier.create(itemMono)
                .expectSubscription()
                .expectNext(defaultItem)
                .verifyComplete();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1);
        String bodyString = recordedRequest.getBody().readString(StandardCharsets.UTF_8);
        Item itemRead = objectMapper.readValue(bodyString, Item.class);
        assertThat(itemRead).isEqualTo(itemSent);
    }

    @Test
    void createItem() throws JsonProcessingException, InterruptedException {
        //given
        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(defaultItem))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .addHeader(LOCATION, "http://localhost:8080/v1/item/MyId"));
        Item itemSent = new Item(null, "descToSet", 9.01);
        //when
        Mono<Item> itemMono = controller.createItem(itemSent);

        //then
        StepVerifier.create(itemMono)
                .expectSubscription()
                .expectNext(defaultItem)
                .verifyComplete();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1);
        String bodyString = recordedRequest.getBody().readString(StandardCharsets.UTF_8);
        Item itemRead = objectMapper.readValue(bodyString, Item.class);
        assertThat(itemRead).isEqualTo(itemSent);
    }

    @Test
    void updateItem() throws JsonProcessingException, InterruptedException {
        //given
        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(defaultItem))
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        Item itemSent = new Item("MyId", "descToSet", 9.01);
        //when
        Mono<Item> itemMono = controller.updateItem(itemSent, "MyId");

        //then
        StepVerifier.create(itemMono)
                .expectSubscription()
                .expectNext(defaultItem)
                .verifyComplete();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("PUT");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1 + "/MyId");
        String bodyString = recordedRequest.getBody().readString(StandardCharsets.UTF_8);
        Item itemRead = objectMapper.readValue(bodyString, Item.class);
        assertThat(itemRead).isEqualTo(itemSent);
    }

    @Test
    void deleteItem_Present() throws InterruptedException {
        //given
        mockBackEnd.enqueue(new MockResponse()
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        //when
        Mono<Void> itemMono = controller.deleteItem("MyId");

        //then
        StepVerifier.create(itemMono)
                .expectSubscription()
                .verifyComplete();

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1 + "/MyId");
    }

    @Test
    void deleteItem_Absent() throws InterruptedException {
        //given
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value())
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        //when
        Mono<Void> itemMono = controller.deleteItem("MyId");

        //then
        StepVerifier.create(itemMono)
                .verifyErrorSatisfies(
                        ex -> assertThat(ex)
                                .hasMessageContaining("Client received an error: ")
                                .isInstanceOf(ResponseStatusException.class)
                );

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1 + "/MyId");
    }

    @Test
    void deleteItem_ServerError() throws InterruptedException {
        //given
        String errorMessage = "Some Server Error";
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody(errorMessage)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE));
        //when
        Mono<Void> itemMono = controller.deleteItem("MyId");

        //then
        StepVerifier.create(itemMono)
                .verifyErrorSatisfies(
                        ex -> assertThat(ex)
                                .hasMessage(errorMessage)
                                .isInstanceOf(RuntimeException.class)
                );

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1 + "/MyId");
    }

    @Test
    void errorEndpointUsingRetrieve_text() throws JsonProcessingException, InterruptedException {
        //given
        String errorMessage = "Runtime Exception Occurred";
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody(errorMessage)
                .addHeader(CONTENT_TYPE, TEXT_PLAIN));
        //when
        Flux<Item> itemFlux = controller.retrieveError();

        //then
        StepVerifier.create(itemFlux)
                .expectSubscription()
                .verifyErrorSatisfies(
                        ex -> assertThat(ex).
                                hasMessage(errorMessage).
                                isInstanceOf(RuntimeException.class)
                );

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1 + "/runtimeException");
    }

    @Test
    void errorEndpointUsingRetrieve_json() throws JsonProcessingException, InterruptedException {
        //given
        String errorMessage = "Runtime Exception Occurred";
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody(errorMessage)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON));
        //when
        Flux<Item> itemFlux = controller.retrieveError();

        //then
        StepVerifier.create(itemFlux)
                .expectSubscription()
                .verifyErrorSatisfies(
                        ex -> assertThat(ex).
                                hasMessage(errorMessage).
                                isInstanceOf(RuntimeException.class)
                );

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1 + "/runtimeException");
    }

    @Test
    void errorEndpointUsingExchange_json() throws JsonProcessingException, InterruptedException {
        //given
        String errorMessage = "Runtime Exception Occurred";
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody(errorMessage)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON));
        //when
        Flux<Item> itemFlux = controller.exchangeError();

        //then
        StepVerifier.create(itemFlux)
                .expectSubscription()
                .verifyErrorSatisfies(
                        ex -> assertThat(ex).
                                hasMessage(errorMessage).
                                isInstanceOf(RuntimeException.class)
                );

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo(ITEM_END_POINT_V1 + "/runtimeException");
    }
}
