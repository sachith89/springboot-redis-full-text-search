package com.sachith.redis.repository;

import com.google.gson.Gson;
import com.sachith.redis.model.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.UnifiedJedis;

import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostRepository {

    private final UnifiedJedis jedis;

    public Post save(Post post) {

        if (post.getPostId() == null) {
            post.setPostId(UUID.randomUUID().toString());
        }

        Gson gson = new Gson();
        String key = "post:" + post.getPostId();

        jedis.jsonSet(key,gson.toJson(post));

        jedis.sadd("post", key);
         return post;
    }

    public void deleteAll() {
        Set<String> keys = jedis.smembers("post");
        if (!keys.isEmpty()){
            keys.stream().forEach(jedis::jsonDel);
        }
        jedis.del("post");

    }
}
