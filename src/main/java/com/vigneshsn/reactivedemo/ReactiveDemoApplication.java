package com.vigneshsn.reactivedemo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class ReactiveDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveDemoApplication.class, args);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }


    @Bean
    RouterFunction<ServerResponse> routes(PostService postService) {
        return route()
                .GET("/posts", serverRequest -> ServerResponse.ok().body(postService.getPost(), Post.class))
                .GET("/comments", serverRequest -> ServerResponse.ok().body(postService.getComments(), Comment.class))
                .build();
    }
}

@Component
@Slf4j
class PostService {
    @Autowired
    WebClient webClient;

    //Scheduler scheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(5));

    Flux<Post> getPost() {
        log.info("getPost method {} ", Thread.currentThread().getName());
        return webClient.get()
                .uri("https://jsonplaceholder.typicode.com/posts")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(Post.class)
                .doOnError(th -> log.error("printing error {}", th.getMessage()))
                .doOnNext(post -> log.info("Post {}", post));
    }


    Flux<Comment> getComments() {
        log.info("getComments method {} ", Thread.currentThread().getName());
        return getPost()
                .take(5)
                .log()
               // .subscribeOn(Schedulers.newParallel("vignesh"))
                .flatMap(post -> {
                            log.info("getComments method inside flatmap {} ", Thread.currentThread().getName());
                            return webClient.get()
                                    .uri("https://jsonplaceholder.typicode.com/comments?postId={postId}", post.id)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .retrieve()
                                    .bodyToFlux(Comment.class);
                        }
                );
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
class Post {
    int id;
    String title;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
class Comment {
    int id;
    int postId;
    String name;
    String body;
}