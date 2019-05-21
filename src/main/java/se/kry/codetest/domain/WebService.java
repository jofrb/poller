package se.kry.codetest.domain;

import java.sql.Timestamp;
import java.time.Instant;

public class WebService {

  public WebService(String name, String url){
    this.name = name;
    this.url = url;
    this.added = Timestamp.from(Instant.now());
    this.status = "UNKNOWN";
  }

  private String name;
  private String url;
  private Timestamp added;
  private String status;

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public Timestamp getAdded() {
    return added;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
