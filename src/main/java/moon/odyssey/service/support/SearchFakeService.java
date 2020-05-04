package moon.odyssey.service.support;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moon.odyssey.model.Post;
import moon.odyssey.service.SearchService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchFakeService implements SearchService {

    private final WebClient webClient;

    public Flux<Post> getAllPosts() {

        return
            webClient.mutate()
                     .baseUrl("https://jsonplaceholder.typicode.com")
                     .build()
                     .get()
                     .uri("/posts")
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .onStatus(status -> status.is4xxClientError() || status.is5xxServerError()
                         , clientResponse -> clientResponse.bodyToMono(String.class).map(body -> new RuntimeException(body)))
                     .bodyToFlux(Post.class)
                     .log()
            ;
    }

    public Mono<Post> getPost(Integer postId) {

        return
            webClient.mutate()
                     .baseUrl("https://jsonplaceholder.typicode.com")
                     .build()
                     .get()
                     .uri("/posts/{ID}"
                        , postId
                     )
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .onStatus(status -> status.is4xxClientError() || status.is5xxServerError()
                         , clientResponse -> clientResponse.bodyToMono(String.class).map(body -> new RuntimeException(body)))
                     .bodyToMono(Post.class)
                     .log()
            ;
    }


}
