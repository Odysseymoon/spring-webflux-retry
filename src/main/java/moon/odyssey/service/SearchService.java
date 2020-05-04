package moon.odyssey.service;

import moon.odyssey.model.Post;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SearchService {

    Flux<Post> getAllPosts();

    Mono<Post> getPost(Integer postId);
}
