package it.unipi.myfuture.myfuture_backend.dao.mongo;

import it.unipi.myfuture.myfuture_backend.enums.CounterType;
import it.unipi.myfuture.myfuture_backend.model.Counter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * DAO responsible for managing counters used to generate sequential IDs.
 * Uses atomic findAndModify to ensure consistency.
 */
@Repository
public class CounterDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Atomically increments and returns the next value of a counter.
     *
     * @param counterId identifier of the counter (e.g. "user_id", "transaction_id")
     * @return next sequential value
     */
    public long getNextSequence(CounterType counterId) {

        Query query = new Query(Criteria.where("_id").is(counterId.toString()));

        Update update = new Update().inc("seq", 1);

        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(true);

        Counter counter = mongoTemplate.findAndModify(
                query,
                update,
                options,
                Counter.class
        );

        return counter.getSeq();
    }
}