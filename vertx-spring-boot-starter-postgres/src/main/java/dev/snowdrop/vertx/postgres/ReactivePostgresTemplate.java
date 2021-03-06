package dev.snowdrop.vertx.postgres;

import java.util.List;

import io.vertx.axle.sqlclient.Row;
import io.vertx.axle.sqlclient.Tuple;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactivePostgresTemplate {

    Flux<Row> query(String sql);

    Flux<Row> preparedQuery(String sql);

    Flux<Row> preparedQuery(String sql, Tuple arguments);

    Flux<Row> preparedBatch(String sql, List<Tuple> batch);

    <T> Flux<Row> save(T entity);
}
