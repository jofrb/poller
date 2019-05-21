package se.kry.codetest;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import se.kry.codetest.domain.WebService;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

class BackgroundPoller {

  private WebClient webClient;

  public BackgroundPoller(WebClient webClient){
    this.webClient = webClient;
  }

  public Future<String> pollServices(WebService service) {

    Future<String> future = Future.future();

    webClient.getAbs(service.getUrl()).send(event -> {
      if (event.succeeded()) future.complete("Available");
      else future.complete("Unavailable");
    });

    return future;
  }
}
