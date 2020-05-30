package com.whitehatsec.idd.jenkins.plugin;

public enum Severity {
  CRITICAL("Critical"),
  HIGH("High"),
  MEDIUM("Medium"),
  LOW("Low"),
  NOTE("Note");

  public final String level;

  private Severity(String level) {
    this.level = level;
  }
}