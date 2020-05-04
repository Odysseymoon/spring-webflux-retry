package moon.odyssey.service;

import org.assertj.core.api.Assertions;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import moon.odyssey.model.Post;
import reactor.test.StepVerifier;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
public class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    @Test
    public void _0_init() {
        Assertions.assertThat(searchService).isNotNull();
    }

    @Test
    public void _1_getPosts_should_return_AllPosts() {

        StepVerifier.create(searchService.getAllPosts())
                    .thenConsumeWhile(post -> {
                        log.info("##### {}", post);
                        Assertions.assertThat(post).isInstanceOf(Post.class);
                        return true;
                    })
                    .verifyComplete();
    }

    @Test
    public void _2_getPostById_should_return_Post() {

        StepVerifier.create(searchService.getPost(1))
                    .thenConsumeWhile(post -> {
                        log.info("##### {}", post);
                        Assertions.assertThat(post).isInstanceOf(Post.class);
                        return true;
                    })
                    .verifyComplete();

    }

}