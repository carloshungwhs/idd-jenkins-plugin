package com.whitehatsec.idd.jenkins.plugin;

import java.util.List;

public class Configuration {
  private boolean applyDebugHeader;
  private List<String> excludedUrls;
  private List<String> excludedAttackUrls;
  private List<String> excludedFileExtensions;
  private List<String> excludedParameters;
  private List<String> attackModules;
  private boolean disablePassiveModules;
  private List<String> allowedHosts;
  private boolean includeEvidence;
  private String severityReportLevel;
  private String severityFailLevel;
  private List<HostMapping> hosts;

  public boolean isApplyDebugHeader() {
    return applyDebugHeader;
  }

  public void setApplyDebugHeader(boolean applyDebugHeader) {
    this.applyDebugHeader = applyDebugHeader;
  }

  public List<String> getExcludedUrls() {
    return excludedUrls;
  }

  public void setExcludedUrls(List<String> excludedUrls) {
    this.excludedUrls = excludedUrls;
  }

  public List<String> getExcludedAttackUrls() {
    return excludedAttackUrls;
  }

  public void setExcludedAttackUrls(List<String> excludedAttackUrls) {
    this.excludedAttackUrls = excludedAttackUrls;
  }

  public List<String> getExcludedFileExtensions() {
    return excludedFileExtensions;
  }

  public void setExcludedFileExtensions(List<String> excludedFileExtensions) {
    this.excludedFileExtensions = excludedFileExtensions;
  }

  public List<String> getExcludedParameters() {
    return excludedParameters;
  }

  public void setExcludedParameters(List<String> excludedParameters) {
    this.excludedParameters = excludedParameters;
  }

  public List<String> getAttackModules() {
    return attackModules;
  }

  public void setAttackModules(List<String> attackModules) {
    this.attackModules = attackModules;
  }

  public boolean isDisablePassiveModules() {
    return disablePassiveModules;
  }

  public void setDisablePassiveModules(boolean disablePassiveModules) {
    this.disablePassiveModules = disablePassiveModules;
  }

  public List<String> getAllowedHosts() {
    return allowedHosts;
  }

  public void setAllowedHosts(List<String> allowedHosts) {
    this.allowedHosts = allowedHosts;
  }

  public boolean isIncludeEvidence() {
    return includeEvidence;
  }

  public void setIncludeEvidence(boolean includeEvidence) {
    this.includeEvidence = includeEvidence;
  }

  public String getSeverityReportLevel() {
    return severityReportLevel;
  }

  public void setSeverityReportLevel(String severityReportLevel) {
    this.severityReportLevel = severityReportLevel;
  }

  public String getSeverityFailLevel() {
    return severityFailLevel;
  }

  public void setSeverityFailLevel(String severityFailLevel) {
    this.severityFailLevel = severityFailLevel;
  }

  public List<HostMapping> getHosts() {
    return hosts;
  }

  public void setHosts(List<HostMapping> hosts) {
    this.hosts = hosts;
  }
}
