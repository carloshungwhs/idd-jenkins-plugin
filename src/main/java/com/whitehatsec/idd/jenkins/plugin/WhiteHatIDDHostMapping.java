package com.whitehatsec.idd.jenkins.plugin;

import java.io.Serializable;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class WhiteHatIDDHostMapping  extends AbstractDescribableImpl<WhiteHatIDDHostMapping> implements Serializable, ExtensionPoint {
  private static final long serialVersionUID = 1L;

  private Boolean enableHostMapping;
  private String fromHost;
  private String toHost;

  @DataBoundConstructor
  public WhiteHatIDDHostMapping(Boolean enableHostMapping, String fromHost, String toHost) {
    this.enableHostMapping = enableHostMapping;
    this.fromHost = fromHost;
    this.toHost = toHost;
  }

  public Boolean getEnableHostMapping() {
    return enableHostMapping;
  }

  public String getFromHost() {
    return fromHost;
  }

  public String getToHost() {
    return toHost;
  }

  @Symbol("whsIddHost")
  @Extension
  public static class WhiteHatIDDHostMappingDescriptorImpl extends Descriptor<WhiteHatIDDHostMapping> {
    @Override
    public String getDisplayName() {
      return Messages.WhiteHatIDDHostMappingBuilder_DescriptorImpl_DisplayName();
    }
  }
}