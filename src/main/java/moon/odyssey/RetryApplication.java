package moon.odyssey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@SpringBootApplication
@EnableWebFlux
@Slf4j
public class RetryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetryApplication.class, args);
    }

    @Bean
    public WebClient webClient() {

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.withDefaults();
        exchangeStrategies
            .messageWriters().stream()
            .filter(LoggingCodecSupport.class::isInstance)
            .forEach(writer -> ((LoggingCodecSupport)writer).setEnableLoggingRequestDetails(true));

        return WebClient.builder()
                        .clientConnector(
                            new ReactorClientHttpConnector(
                                HttpClient
                                    .create()
                                    .tcpConfiguration(
                                        client -> client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120_000)
                                                        .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(180))
                                                                                   .addHandlerLast(new WriteTimeoutHandler(180))
                                                        )
                                    )
                            )
                        )
                        .exchangeStrategies(exchangeStrategies)
                        .filter(ExchangeFilterFunction.ofRequestProcessor(
                            clientRequest -> {
                                log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                                clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.debug("{} : {}", name, value)));
                                return Mono.just(clientRequest);
                            }
                        ))
                        .filter(ExchangeFilterFunction.ofResponseProcessor(
                            clientResponse -> {
                                clientResponse.headers().asHttpHeaders().forEach((name, values) -> values.forEach(value -> log.debug("{} : {}", name, value)));
                                return Mono.just(clientResponse);
                            }
                        ))
                        .defaultHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.3")
                        .build();
    }
}
