package com.sachith.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sachith.redis.model.Post;
import com.sachith.redis.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;

import java.util.Arrays;

@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class SpringbootRedisFullTextSearchApplication {

    private final PostRepository postRepository;
    private final UnifiedJedis jedis;
    @Value("classpath:data.json")
    private Resource resource;

    public static void main(String[] args) {
        SpringApplication.run(SpringbootRedisFullTextSearchApplication.class, args);
    }

    @Bean
    CommandLineRunner loadData() {
        return args -> {
            //delete all
            postRepository.deleteAll();
            try {
                jedis.ftDropIndex("post-idx");
            } catch (Exception e) {
                log.info(e.getMessage());
            }

            String data = new String(resource.getInputStream().readAllBytes());
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Post[] post = objectMapper.readValue(data, Post[].class);

            Arrays.stream(post).forEach(postRepository::save);

            Schema schema = new Schema()
                    .addField(new Schema.Field(FieldName.of("$.content").as("content"), Schema.FieldType.TEXT, true, false))
                    .addField(new Schema.TextField(FieldName.of("$.title").as("title")))
                    .addField(new Schema.Field(FieldName.of("$.tags[*]").as("tags"), Schema.FieldType.TAG))
                    .addField(new Schema.Field(FieldName.of("$.views").as("views"), Schema.FieldType.NUMERIC, false, true));

            IndexDefinition indexDefinition = new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"post:"});

            jedis.ftCreate("post-idx", IndexOptions.defaultOptions().setDefinition(indexDefinition), schema);

        };
    }

}
