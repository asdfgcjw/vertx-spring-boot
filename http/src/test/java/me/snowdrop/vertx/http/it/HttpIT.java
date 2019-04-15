package me.snowdrop.vertx.http.it;

import java.time.Duration;
import java.util.Properties;

import io.restassured.path.xml.XmlPath;
import io.vertx.core.http.HttpClientOptions;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import static io.restassured.path.xml.XmlPath.CompatibilityMode.HTML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.web.reactive.function.server.RouterFunctions.resources;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.noContent;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

public class HttpIT extends TestBase {

    @After
    public void tearDown() {
        stopServer();
    }

    @Test
    public void shouldGet404Response() {
        startServer();

        HttpStatus status = getWebClient()
            .get()
            .exchange()
            .map(ClientResponse::statusCode)
            .block(Duration.ofSeconds(2));

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void shouldGetEmptyResponse() {
        startServer(NoopRouter.class);

        HttpStatus status = getWebClient()
            .get()
            .exchange()
            .map(ClientResponse::statusCode)
            .block(Duration.ofSeconds(2));

        assertThat(status).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    public void shouldExchangeBodies() {
        startServer(UpperBodyRouter.class);

        String body = getWebClient()
            .post()
            .syncBody("test")
            .retrieve()
            .bodyToMono(String.class)
            .block(Duration.ofSeconds(2));

        assertThat(body).isEqualTo("TEST");
    }

    @Test
    public void shouldGetStaticContent() {
        startServer(StaticRouter.class);

        XmlPath xml = getWebClient()
            .get()
            .uri("static/index.html")
            .retrieve()
            .bodyToMono(String.class)
            .map(body -> new XmlPath(HTML, body))
            .blockOptional(Duration.ofSeconds(2))
            .orElseThrow(() -> new AssertionError("Did not receive a response"));

        assertThat(xml.getString("html.body.div")).isEqualTo("Test div");
    }

    @Test
    public void shouldGetCompressedStaticContent() {
        Properties serverProperties = new Properties();
        serverProperties.setProperty("server.compression.enabled", "true");
        startServer(serverProperties, StaticRouter.class);

        HttpClientOptions clientOptions = new HttpClientOptions()
            .setTryUseCompression(true);

        XmlPath xml = getWebClient(clientOptions)
            .get()
            .uri("static/index.html")
            .header(ACCEPT_ENCODING, "gzip")
            .retrieve()
            .bodyToMono(String.class)
            .map(body -> new XmlPath(HTML, body))
            .blockOptional(Duration.ofSeconds(2))
            .orElseThrow(() -> new AssertionError("Did not receive a response"));

        assertThat(xml.getString("html.body.div")).isEqualTo("Test div");
    }

    @Test
    public void shouldExchangeHeaders() {
        startServer(UpperHeaderRouter.class);

        String text = getWebClient()
            .get()
            .header("text", "test")
            .exchange()
            .map(ClientResponse::headers)
            .map(headers -> headers.header("text"))
            .map(values -> values.get(0))
            .block(Duration.ofSeconds(2));

        assertThat(text).isEqualTo("TEST");
    }

    @Test
    public void shouldExchangeCookies() {
        startServer(UpperCookieRouter.class);

        String text = getWebClient()
            .get()
            .cookie("text", "test")
            .exchange()
            .map(ClientResponse::cookies)
            .map(cookies -> cookies.getFirst("text"))
            .map(ResponseCookie::getValue)
            .block(Duration.ofSeconds(2));

        assertThat(text).isEqualTo("TEST");
    }

    @Configuration
    static class StaticRouter {
        @Bean
        public RouterFunction<ServerResponse> staticRouter() {
            return resources("/**", new ClassPathResource("static"));
        }
    }

    @Configuration
    static class NoopRouter {
        @Bean
        public RouterFunction<ServerResponse> noopRouter() {
            return route()
                .GET("/", request -> noContent().build())
                .build();
        }
    }

    @Configuration
    static class UpperBodyRouter {
        @Bean
        public RouterFunction<ServerResponse> upperBodyRouter() {
            return route()
                .POST("/", request -> {
                    Flux<String> body = request.bodyToFlux(String.class)
                        .map(String::toUpperCase);

                    return ok().body(body, String.class);
                })
                .build();
        }
    }

    @Configuration
    static class UpperCookieRouter {
        @Bean
        public RouterFunction<ServerResponse> upperCookieRouter() {
            return route()
                .GET("/", request -> {
                    String text = request.cookies()
                        .getFirst("text")
                        .getValue()
                        .toUpperCase();
                    ResponseCookie cookie = ResponseCookie.from("text", text).build();

                    return noContent().cookie(cookie).build();
                })
                .build();
        }
    }

    @Configuration
    static class UpperHeaderRouter {
        @Bean
        public RouterFunction<ServerResponse> upperHeaderRouter() {
            return route()
                .GET("/", request -> {
                    String text = request.headers()
                        .header("text")
                        .get(0)
                        .toUpperCase();

                    return noContent().header("text", text).build();
                })
                .build();
        }
    }
}
