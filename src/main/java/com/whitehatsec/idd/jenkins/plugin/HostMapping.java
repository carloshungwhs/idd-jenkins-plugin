package com.whitehatsec.idd.jenkins.plugin;

public class HostMapping {
	private String from;
  private String to;
  private boolean enable;

  public String getFrom() {
    return this.from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return this.to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public boolean isEnable() {
    return this.enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }
}