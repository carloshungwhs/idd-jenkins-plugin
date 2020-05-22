package com.whitehatsec.idd.jenkins.plugin;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

public class WhiteHatIDDRecorder extends Recorder {
  private Boolean useLocalConfig;
  private String harSource;
  private Integer severityLevel;
  private Integer severityFailLevel;

  @DataBoundConstructor
  public WhiteHatIDDRecorder(Boolean useLocalConfig, String harSource, Integer severityLevel, Integer severityFailLevel) {
    this.useLocalConfig = useLocalConfig;
    this.harSource = harSource;
    this.severityLevel = severityLevel;
    this.severityFailLevel = severityFailLevel;
  }

  public String getHarSource() {
    return harSource;
  }

  public Integer getSeverityLevel() {
    return severityLevel;
  }

  public Integer getSeverityFailLevel() {
    return severityFailLevel;
  }

  public boolean isUseLocalConfig() {
    return useLocalConfig;
  }

  //@DataBoundSetter
  public Boolean setUseLocalConfig() {
    return useLocalConfig;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {
    try {
      // Map<String, String> environment = build.getEnvVars();
      //EnvVars environment = build.getEnvironment(listener);
      build.setResult(Result.SUCCESS);
    } catch (InterruptedException e) {
      listener.getLogger().println("WhiteHat Security ID-DAST - Unable to start: " + e.getMessage());
    } catch (IOException e) {
      listener.getLogger().println("WhiteHat Security ID-DAST - Unable to start " + e.getMessage());
    }
    return true;
  }

  @Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
  }

  @Symbol("idd")
  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private String harSource;
    private Integer severityLevel;
    private Integer severityFailLevel;

    public DescriptorImpl() {
      load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return Messages.WhiteHatIDDRecorderBuilder_DescriptorImpl_DisplayName();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
      harSource = json.getString("harSource");
      //severityLevel = json.getInt("severityLevel");
      //severityFailLevel= json.getInt("severityFailLevel");

      return super.configure(req, json);
    }

    public String getHarSource() {
      return harSource;
    }

    public void setHarSource(String harSource) {
      this.harSource = harSource;
    }

    public Integer getSeverityLevel() {
      return severityLevel;
    }

    public void setSeverityLevel(Integer severityLevel) {
      this.severityLevel = severityLevel;
    }

    public Integer getSeverityFailLevel() {
      return severityFailLevel;
    }

    public void setSeverityFailLevel(Integer severityFailLevel) {
      this.severityFailLevel = severityFailLevel;
    }

    public FormValidation doCheckHarPath(@QueryParameter String value) throws IOException, ServletException {
      if (StringUtils.isBlank(value)) {
        return FormValidation.error(Messages.WhiteHatIDDRecorderBuilder_DescriptionImpl_errors_requiredHarPath());
      }
      return FormValidation.ok();
    }
  }
}
