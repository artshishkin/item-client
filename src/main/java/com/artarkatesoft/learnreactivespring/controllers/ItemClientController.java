package com.artarkatesoft.learnreactivespring.controllers;

import com.artarkatesoft.learnreactivespring.constants.ItemConstants;
import com.artarkatesoft.learnreactivespring.domain.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@RestController
@RequiredArgsConstructor
public class ItemClientController {

    @Value("${itemclient.itemserver.url}")
    private String itemServerUrl;

    private WebClient webClient;

    @PostConstruct
    private void init() {
        webClient = WebClient.create(itemServerUrl);
    }

    @GetMapping("/client/retrieve")
    public Flux<Item> getAllItemsUsingRetrieve() {
        return webClient.get().uri(ItemConstants.ITEM_END_POINT_V1)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(Item.class)
                .log("Items in Client Project retrieve");
    }

    @GetMapping("/client/exchange")
    public Flux<Item> getAllItemsUsingExchange() {
        return webClient.get().uri(ItemConstants.ITEM_END_POINT_V1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .flatMapMany(clientResponse -> clientResponse.bodyToFlux(Item.class))
                .log("Items in Client Project exchange");
    }

    @GetMapping("/client/retrieve/{id}")
    public Mono<Item> getOneItemUsingRetrieve(@PathVariable String id) {
        return webClient.get().uri(ItemConstants.ITEM_END_POINT_V1 + "/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Item.class)
                .log("Client Project retrieve Single Item");
    }

    @GetMapping("/client/exchange/{id}")
    public Mono<Item> getOneItemUsingExchange(@PathVariable String id) {
        return webClient.get().uri(ItemConstants.ITEM_END_POINT_V1 + "/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .flatMap(clientResponse -> clientResponse.bodyToMono(Item.class))
                .log("Client Project exchange Single Item");
    }

}
