package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.domain.WebService;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private HashMap<String, WebService> services = new HashMap<>();
  private DBConnector connector;
  private WebClient client;
  private BackgroundPoller poller;

  @Override
  public void start(Future<Void> startFuture) {
    client = WebClient.create(vertx);
    connector = new DBConnector(vertx);
    poller = new BackgroundPoller(client);
    connector.getServices()
        .setHandler(event -> {
          if (event.succeeded()) {

            event
                .result()
                .getRows()
                .stream()
                .forEach(jsonObject -> services
                    .put(
                        jsonObject.getString("url"),
                        new WebService(jsonObject.getString("service_name"), jsonObject.getString("url"))));
            startServer(startFuture);
          }
        });
  }

  private void startServer(Future<Void> startFuture) {


    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    vertx.setPeriodic(1000 * 60, timerId -> {
      if (!services.isEmpty()) {
        services.entrySet().forEach(
            service ->
            poller
                .pollServices(service.getValue())
                .setHandler(completed -> service.getValue().setStatus(completed.result())));
      }
    });
    setRoutes(router);
    vertx
            .createHttpServer()
            .requestHandler(router)
            .listen(8080, result -> {
              if (result.succeeded()) {
                System.out.println("KRY code test service started");
                startFuture.complete();
              } else {
                startFuture.fail(result.cause());
              }
            });
  }

  private void setRoutes(Router router){
    router.route("/*").handler(StaticHandler.create());
    router.get("/service").handler(request -> {
      List<JsonObject> jsonServices = services
          .entrySet()
          .stream()
          .map(service ->
              new JsonObject()
                  .put("name", service.getValue().getName())
                  .put("url", service.getKey())
                  .put("status", service.getValue().getStatus()))
          .collect(Collectors.toList());
      request.response()
          .putHeader("content-type", "application/json")
          .end(new JsonArray(jsonServices).encode());
    });
    router.post("/service").handler(request -> {

      if (connector != null){
        JsonObject jsonBody = request.getBodyAsJson();
        WebService webService = new WebService(jsonBody.getString("name"), jsonBody.getString("url"));
        connector.addService(webService.getName(), webService.getUrl());
        services.put(jsonBody.getString("url"), webService);
        request.response()
            .putHeader("content-type", "text/plain")
            .setStatusCode(200)
            .end("OK");
      }else{
        request.response()
            .putHeader("content-type", "text/plain")
            .setStatusCode(500)
            .end("Internal Server Error");
      }
    });

    router.delete("/service").handler(request ->{
      if (connector != null){
        JsonObject jsonBody = request.getBodyAsJson();
        String url = jsonBody.getString("url");
        Future<ResultSet> result = connector.deleteService(url);

        result.setHandler(event -> {
          if (event.succeeded()){
            services.remove(url);
            request.response()
                .putHeader("content-type", "text/plain")
                .setStatusCode(200)
                .end("OK");
          }
          else{
            request.response()
                .putHeader("content-type", "text/plain")
                .setStatusCode(400)
                .end("Bad request");
          }
        });
      }
    });
  }
}
