package com.artarkatesoft.learnreactivespring.controllers;

import com.artarkatesoft.learnreactivespring.constants.ItemConstants;
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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.artarkatesoft.learnreactivespring.constants.ItemConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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
    void getOneItemUsingRetrieve() {
    }

    @Test
    void getOneItemUsingExchange() {
    }

    @Test
    void postOneItem() {
    }

    @Test
    void createItem() {
    }

    @Test
    void testCreateItem() {
    }

    @Test
    void deleteItem() {
    }
}