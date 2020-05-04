package moon.odyssey.service;

import org.assertj.core.api.Assertions;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import lombok.extern.slf4j.Slf4j;
import moon.odyssey.model.Post;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@RunWith(SpringRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
public class SearchServiceMockTest {

    @MockBean
    private SearchService searchService;

    @Test
    public void _0_init() {
        Assertions.assertThat(searchService).isNotNull();
    }

    @Test
    public void _01_test_OnErrorReturn() {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .onErrorReturn(new Post(22, "fallback22", "body22", 22));

        postFlux.subscribe(post -> log.info(post.toString()));
    }

    @Test
    public void _02_test_OnErrorResumeFunction() {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .onErrorResume(throwable -> {
                                      log.error(throwable.getLocalizedMessage());
                                      return Mono.just(new Post(22, "fallback22", "body22", 22));
                                  });

        postFlux.subscribe(post -> log.info(post.toString()));
    }

    @Test
    public void _03_test_OnErrorResumeType() {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenReturn(Mono.just(new Post(2, "title2", "body2", 2)));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .onErrorResume(
                                      RuntimeException.class
                                      , throwable -> Mono.just(new Post(22, "fallback22", "body22", 22))
                                  );

        postFlux.subscribe(post -> log.info(post.toString()));
    }

    @Test
    public void _04_test_OnErrorResumePredicate() {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .onErrorResume(throwable -> throwable.getLocalizedMessage().contains("Custom")
                                    , throwable -> {log.error(throwable.getLocalizedMessage()); return Mono.empty();}
                                  );

        postFlux
            .subscribe(post -> log.info(post.toString())
                , throwable -> log.error("Subscriber Error with {}", throwable.getLocalizedMessage())
            );
    }

    @Test
    public void _05_test_OnErrorContinue() {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"));

        Mockito.when(searchService.getPost(3))
               .thenReturn(Mono.just(new Post(3, "title3", "body3", 3)));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .onErrorContinue((throwable, o) -> log.error("Error occurred at : {} with {} ", o, throwable.getLocalizedMessage()));

        postFlux.subscribe(post -> log.info(post.toString()));

    }

    @Test
    public void _06_test_OnErrorContinueWithExceptionType() {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"));

        Mockito.when(searchService.getPost(3))
               .thenReturn(Mono.just(new Post(3, "title3", "body3", 3)));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .onErrorContinue(RuntimeException.class, (throwable, o) -> log.error("Error occurred at : {} with {} ", o, throwable.getLocalizedMessage()));

        postFlux.subscribe(post -> log.info(post.toString()));

    }

    @Test
    public void _07_test_OnErrorContinuePredicate() {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"));

        Mockito.when(searchService.getPost(3))
               .thenReturn(Mono.just(new Post(3, "title3", "body3", 3)));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .onErrorContinue(throwable -> throwable.getLocalizedMessage().contains("Mock"), (throwable, o) -> log.error("Error occurred at : {} with {} ", o, throwable.getLocalizedMessage()));

        postFlux.subscribe(post -> log.info(post.toString()));

    }

    @Test
    public void _08_test_OnErrorRetry() {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenReturn(Mono.just(new Post(2, "title2", "body2", 3)));

        Mockito.when(searchService.getPost(3))
               .thenReturn(Mono.just(new Post(3, "title3", "body3", 3)));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .retry()
            ;

        postFlux.subscribe(post -> log.info(post.toString()));

    }

    @Test
    public void _09_test_OnErrorRetryNtimes() {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenReturn(Mono.just(new Post(2, "title2", "body2", 3)));

        Mockito.when(searchService.getPost(3))
               .thenReturn(Mono.just(new Post(3, "title3", "body3", 3)));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .retry(2)
            ;

        postFlux.subscribe(post -> log.info(post.toString()));

    }

    @Test
    public void _10_test_OnErrorRetryWhen() throws InterruptedException {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenReturn(Mono.just(new Post(2, "title2", "body2", 3)));

        Mockito.when(searchService.getPost(3))
               .thenReturn(Mono.just(new Post(3, "title3", "body3", 3)));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .retryWhen(Retry.max(2))
            ;

        CountDownLatch latch = new CountDownLatch(1);

        postFlux
            .subscribe(post -> log.info(post.toString())
                , throwable -> log.error(throwable.getLocalizedMessage())
                , () -> {latch.countDown();}
            );

        latch.await();

    }

    @Test
    public void _11_test_OnErrorRetryWhenFixedDelay() throws InterruptedException {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenReturn(Mono.just(new Post(2, "title2", "body2", 3)));

        Mockito.when(searchService.getPost(3))
               .thenReturn(Mono.just(new Post(3, "title3", "body3", 3)));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2)))
            ;

        CountDownLatch latch = new CountDownLatch(1);

        postFlux
            .subscribe(post -> log.info(post.toString())
                , throwable -> log.error(throwable.getLocalizedMessage())
                , () -> {latch.countDown();}
            );

        latch.await();

    }

    @Test
    public void _12_test_OnErrorRetryWhenBackOff() throws InterruptedException {

        Mockito.when(searchService.getPost(1))
               .thenReturn(Mono.just(new Post(1, "title1", "body1", 1)));

        Mockito.when(searchService.getPost(2))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenThrow(new RuntimeException("Mock Exception"))
               .thenReturn(Mono.just(new Post(2, "title2", "body2", 3)));

        Mockito.when(searchService.getPost(3))
               .thenReturn(Mono.just(new Post(3, "title3", "body3", 3)));

        Flux<Post> postFlux = Flux.just(1, 2, 3)
                                  .flatMap(i -> searchService.getPost(i))
                                  .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
            ;

        CountDownLatch latch = new CountDownLatch(1);

        postFlux
            .subscribe(post -> log.info(post.toString())
                , throwable -> log.error(throwable.getLocalizedMessage())
                , () -> {latch.countDown();}
            );

        latch.await();

    }


}