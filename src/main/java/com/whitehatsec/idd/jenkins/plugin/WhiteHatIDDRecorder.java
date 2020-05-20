package com.whitehatsec.idd.jenkins.plugin;

import hudson.tasks.Recorder;

public class WhiteHatIDDRecorder extends Recorder {

  private String harPath;
  private String severityLevel;
  private String severityFailLevel;

  public String getHarPath() {
    return harPath;
  }

  public void setHarPath(String harPath) {
    this.harPath = harPath;
  }

  public String getSeverityLevel() {
    return severityLevel;
  }

  public void setSeverityLevel(String severityLevel) {
    this.severityLevel = severityLevel;
  }

  public String getSeverityFailLevel() {
    return severityFailLevel;
  }

  public void setSeverityFailLevel(String severityFailLevel) {
    this.severityFailLevel = severityFailLevel;
  }

}