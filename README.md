# Spring WebFlux Error handling and Retry strategy
---


1. onErrorReturn
---
Simply retur fallback value when any error occurred
```java_holder_method_tree
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
```
- Result
```text
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=22, title=fallback22, body=body22, userId=22)   <-- Fallback value
```
> Returns fallback value and does not subscribe rest of original `Flux`

2. onErrorResume with `Function`
---
```java_holder_method_tree
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
```
- Result
```text
 INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
ERROR 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Mock Exception
 INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=22, title=fallback22, body=body22, userId=22)  <-- Fallback publisher
```
> Subscribe new fallback publisher and does not subscribe rest of original `Flux`

3. onErrorResume with special `Exception` type
---
```java_holder_method_tree
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
```
- Result
```text
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=22, title=fallback22, body=body22, userId=22)  <-- Fallback publisher
```
> Subscribe new fallback publisher when `RuntimeException` occurred and does not subscribe rest of original `Flux`

4. onErrorResume with `Predicate`
---
```java_holder_method_tree
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
```
- Result
```text
 INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
ERROR 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Subscriber Error with Mock Exception          <-- Exception occurred because throwable does not contain "Custom"
```
> Subscribe new fallback publisher when `Predication` is true, and does not subscribe rest of original `Flux`

5. onErrorContinue
---
```java_holder_method_tree
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
```
- Result
```text
 INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
ERROR 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Error occurred at : 2 with Mock Exception      <-- Error consumed with logger
 INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=3, title=title3, body=body3, userId=3) <-- subscription continued
```
> Recover errors by dropping element with `Consumer` and continuing with subsequent elements

6. onErrorContinue with special `Exception` type
---
```java_holder_method_tree
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
                              .onErrorContinue(
                                  RuntimeException.class
                                  , (throwable, o) -> log.error("Error occurred at : {} with {} ", o, throwable.getLocalizedMessage())
                              );

    postFlux.subscribe(post -> log.info(post.toString()));
}
```
- Result
```text
 INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
ERROR 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Error occurred at : 2 with Mock Exception      <-- Error consumed with logger
 INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=3, title=title3, body=body3, userId=3) <-- subscription continued
```
> Recover errors by dropping element when `RuntimeException` occurred and continuing with subsequent elements

7. onErrorContinue with `Predicate`
---
```java_holder_method_tree
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
                              .onErrorContinue(
                                  throwable -> throwable.getLocalizedMessage().contains("Mock")
                                  , (throwable, o) -> log.error("Error occurred at : {} with {} ", o, throwable.getLocalizedMessage())
                              );

    postFlux.subscribe(post -> log.info(post.toString()));
}
```
- Result
```text
 INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
ERROR 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Error occurred at : 2 with Mock Exception      <-- Error consumed with logger
 INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=3, title=title3, body=body3, userId=3) <-- subscription continued
```
> Recover errors by dropping element when `Predication` is true and continuing with subsequent elements

8. retry
---
```java_holder_method_tree
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
```
- Result
```text
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1) <-- Retry with first element
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=2, title=title2, body=body2, userId=3)
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=3, title=title3, body=body3, userId=3)
```
> Re-subscribes to Publisher's sequence if it signals any error, indefinitely.
>> if Publisher keeps generating error, `retry` works infinitely.

9. retry with max times
---
```java_holder_method_tree
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
```
- Result
```text
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1) <-- First retry
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1) <-- Second retry
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=2, title=title2, body=body2, userId=3)
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=3, title=title3, body=body3, userId=3)
```
> Re-subscribes to Publisher's sequence if it signals any error, for a fixed number of times.

10. retryWhen 
---
```java_holder_method_tree
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
            , latch::countDown
        );

    latch.await();
}
```
- Result
```text
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1) <-- First retry
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1) <-- Second retry
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=2, title=title2, body=body2, userId=3)
INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=3, title=title3, body=body3, userId=3)
```
> Retries subscription with special `Retry` strategy.
>> `Retry.max()` is same as `retry()`

11. retryWhen with fixedDelay
---
```java_holder_method_tree
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
            , latch::countDown
        );

    latch.await();
}
```
- Result
```text
17:44:53.511  INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
17:44:55.533  INFO 47694 --- [     parallel-1] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)  <-- First retry after 2 seconds with new subscriber
17:44:57.536  INFO 47694 --- [     parallel-2] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)  <-- Second retry after 2 seconds with new subscriber
17:44:57.536  INFO 47694 --- [     parallel-2] m.odyssey.service.SearchServiceMockTest  : Post(id=2, title=title2, body=body2, userId=3)
17:44:57.536  INFO 47694 --- [     parallel-2] m.odyssey.service.SearchServiceMockTest  : Post(id=3, title=title3, body=body3, userId=3)
```
> Retries subscription with fixed delayed time and maximum number of retry

11. retryWhen with exponential backoff strategy
---
```java_holder_method_tree
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
            , latch::countDown
        );

    latch.await();
}
```
- Result
```text
17:44:57.544  INFO 47694 --- [           main] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1)
17:44:59.794  INFO 47694 --- [     parallel-1] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1) <-- First retry after at least 2 seconds with new subscriber
17:45:02.416  INFO 47694 --- [     parallel-2] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1) <-- Second retry after at least 2 seconds + jitter with new subscriber
17:45:07.570  INFO 47694 --- [     parallel-3] m.odyssey.service.SearchServiceMockTest  : Post(id=1, title=title1, body=body1, userId=1) <-- Third retry after at least 2 seconds + jitter with new subscriber
17:45:07.570  INFO 47694 --- [     parallel-3] m.odyssey.service.SearchServiceMockTest  : Post(id=2, title=title2, body=body2, userId=3)
17:45:07.571  INFO 47694 --- [     parallel-3] m.odyssey.service.SearchServiceMockTest  : Post(id=3, title=title3, body=body3, userId=3)
```
> Retries subscription with exponential backoff strategy
