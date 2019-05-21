package se.kry.codetest.migrate;

import io.vertx.core.Vertx;
import se.kry.codetest.DBConnector;

public class DBMigration {

  public static String serviceTableName = "service";

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    DBConnector connector = new DBConnector(vertx);

    connector.query("CREATE TABLE IF NOT EXISTS " + serviceTableName + " (url VARCHAR(128) NOT NULL)").setHandler(done -> {
      if (done.succeeded()) {
        connector.query("ALTER TABLE " + serviceTableName + " ADD service_name VARCHAR(128)").setHandler(addServiceName -> {
          if (addServiceName.succeeded()) {
            connector.query("ALTER TABLE " + serviceTableName + " ADD time_added time").setHandler(addTimestamp -> {
              if (addTimestamp.succeeded()) {
                System.out.println("completed db migrations");
              } else {
                done.cause().printStackTrace();
              }
                  vertx.close(shutdown -> System.exit(0));
                }
            );
          } else {
            done.cause().printStackTrace();
          }
          vertx.close(shutdown -> System.exit(0));
        });
      } else {
        vertx.close(shutdown -> System.exit(0));
      }
    });
  }
}
