package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import se.kry.codetest.migrate.DBMigration;


public class DBConnector {

    private final String DB_PATH = "poller.db";
    private final SQLClient client;

    public DBConnector(Vertx vertx) {
        JsonObject config = new JsonObject()
                .put("url", "jdbc:sqlite:" + DB_PATH)
                .put("driver_class", "org.sqlite.JDBC")
                .put("max_pool_size", 30);

        client = JDBCClient.createShared(vertx, config);
    }

    public Future<ResultSet> query(String query) {
        return query(query, new JsonArray());
    }


    public Future<ResultSet> query(String query, JsonArray params) {
        if (query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }

        Future<ResultSet> queryResultFuture = Future.future();

        client.queryWithParams(query, params, result -> {
            if (result.failed()) {
                queryResultFuture.fail(result.cause());
            } else {
                queryResultFuture.complete(result.result());
            }
        });
        return queryResultFuture;
    }

    public Future<ResultSet> addService(String name, String url) {
        try {
            if (url.length() <= 128) {
                String sqlStatement = String.format("INSERT INTO service (url, service_name) values ('%s', '%s')", url, name);
                return query(sqlStatement);
            } else throw new RuntimeException("url String to long");
        } catch (RuntimeException e) {
            e.printStackTrace();
            Debugger.breaker();
        }
        return null;
    }

    public Future<ResultSet> getServices() {
        String sqlStatement = "SELECT * FROM service"; // Only viable while database is within resonable bounds
        return query(sqlStatement);
    }

    public Future<ResultSet> deleteService(String url) {
        try {
            String sqlStatement = String.format("DELETE FROM " + DBMigration.serviceTableName + " WHERE url='%s'", url);
            return query(sqlStatement);

        } catch (RuntimeException e) {
            e.printStackTrace();
            Debugger.breaker();
        }
        return null;
    }
}
