package it.unipi.myfuture.myfuture_backend.dao.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRedisDao {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


}
