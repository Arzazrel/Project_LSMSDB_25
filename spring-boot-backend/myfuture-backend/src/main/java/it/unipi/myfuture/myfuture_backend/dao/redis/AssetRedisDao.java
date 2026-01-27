package it.unipi.myfuture.myfuture_backend.dao.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssetRedisDao {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Update the current price (last price). Write on Master (6379)
     *
     *
     * @param symbol
     * @param price
     */
    public void updateCurrentPrice(String symbol, double price) {
        redisTemplate.opsForValue().set("asset:" + symbol + ":price", price);
    }

    // Metodo per aggiungere campioni intraday (ZSET)

    /**
     *
     *
     * @param symbol
     * @param price
     */
    public void addIntradayPrice(String symbol, double price) {
        long timestamp = System.currentTimeMillis();
        redisTemplate.opsForZSet().add("asset:" + symbol + ":intraday", price, timestamp);
    }
}