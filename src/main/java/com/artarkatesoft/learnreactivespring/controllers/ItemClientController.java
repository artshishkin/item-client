package com.artarkatesoft.learnreactivespring.controllers;

import com.artarkatesoft.learnreactivespring.domain.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.artarkatesoft.learnreactivespring.constants.ItemConstants.ITEM_END_POINT_V1;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ItemClientController {

    private String itemServerUrl;

    private WebClient webClient;

    @Value("${itemclient.itemserver.url}")
    public void setItemServerUrl(String itemServerUrl) {
        this.itemServerUrl = itemServerUrl;
        webClient = WebClient.create(itemServerUrl);
    }

    @GetMapping("/client/retrieve")
    public Flux<Item> getAllItemsUsingRetrieve() {
        return webClient.get().uri(ITEM_END_POINT_V1)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(Item.class)
                .log("Items in Client Project retrieve");
    }

    @GetMapping("/client/exchange")
    public Flux<Item> getAllItemsUsingExchange() {
        return webClient.get().uri(ITEM_END_POINT_V1)
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMapMany(clientResponse -> clientResponse.bodyToFlux(Item.class))
                .log("Items in Client Project exchange");
    }

    @GetMapping("/client/retrieve/{id}")
    public Mono<Item> getOneItemUsingRetrieve(@PathVariable String id) {
        return webClient.get().uri(ITEM_END_POINT_V1 + "/{id}", id)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Item.class)
                .log("Client Project retrieve Single Item");
    }

    @GetMapping("/client/exchange/{id}")
    public Mono<Item> getOneItemUsingExchange(@PathVariable String id) {
        return webClient.get().uri(ITEM_END_POINT_V1 + "/{id}", id)
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap(clientResponse -> clientResponse.bodyToMono(Item.class))
                .log("Client Project exchange Single Item");
    }

    @GetMapping("/client/post")
    public Mono<Item> postOneItem(@RequestParam(required = false) String description, @RequestParam(required = false) Double price) {
        Item newItem = new Item(null, description, price);
        return webClient.post().uri(ITEM_END_POINT_V1)
                .accept(APPLICATION_JSON)
                .bodyValue(newItem)
                .retrieve()
                .bodyToMono(Item.class)
                .log("Client Project post new Item");
    }

    @PostMapping("/client/createItem")
    public Mono<Item> createItem(@RequestBody Item item) {
        return webClient.post().uri(ITEM_END_POINT_V1)
                .accept(APPLICATION_JSON)
                .bodyValue(item)
                .retrieve()
                .bodyToMono(Item.class)
                .log("Client Project post new Item");
    }

    @PutMapping("/client/updateItem/{id}")
    public Mono<Item> updateItem(@RequestBody Item item, @PathVariable String id) {
        return webClient.put().uri(ITEM_END_POINT_V1 + "/{id}", id)
                .accept(APPLICATION_JSON)
                .bodyValue(item)
                .retrieve()
                .bodyToMono(Item.class)
                .log("Client Project update Item");
    }

    @DeleteMapping("/client/deleteItem/{id}")
    public Mono<Void> deleteItem(@PathVariable String id) {
        return webClient.delete().uri(ITEM_END_POINT_V1 + "/{id}", id)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class)
                .log("Client Project delete Item");
    }

    @GetMapping("/client/retrieve/error")
    public Flux<Item> retrieveError() {
        return webClient.get().uri(ITEM_END_POINT_V1 + "/runtimeException")
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        HttpStatus::is5xxServerError,
                        clientResponse -> clientResponse
                                .bodyToMono(String.class)
                                .map(RuntimeException::new)
                )
                .bodyToFlux(Item.class);
    }

    @GetMapping("/client/exchange/error")
    public Flux<Item> exchangeError() {
        return webClient.get().uri(ITEM_END_POINT_V1 + "/runtimeException")
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMapMany(clientResponse -> {
                    if (clientResponse.statusCode().is5xxServerError()) {
                        Mono<String> messageMono = clientResponse.bodyToMono(String.class);
                        return messageMono.flatMap(message -> {
                            log.error("Error message is: " + message);
                            return Mono.error(new RuntimeException(message));
                        });
                    } else {
                        return clientResponse.bodyToFlux(Item.class);
                    }
                });
    }

}
