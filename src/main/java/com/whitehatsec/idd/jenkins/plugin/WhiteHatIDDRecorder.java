package com.whitehatsec.idd.jenkins.plugin;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

public class WhiteHatIDDRecorder extends Recorder {
  private boolean useLocalConfig;
  private String harSource;
  private Integer severityLevel;
  private Integer severityFailLevel;

  private static final String IDD_HOME = "DIRECTED_DAST_HOME";

  @DataBoundConstructor
  public WhiteHatIDDRecorder(boolean useLocalConfig, String harSource, Integer severityLevel, Integer severityFailLevel) {
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
  /*
  public boolean setUseLocalConfig() {
    return useLocalConfig;
  }
  */

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {
    EnvVars env = build.getEnvironment(listener);
    String workspace = env.get("WORKSPACE");
    String iddHome = env.get(IDD_HOME);
    String harSource = StringUtils.isNotBlank(getHarSource()) ? getHarSource() : getDescriptor().getHarSource();

    listener.getLogger().println("WHS => Jenkins Home = " + env.get("JENKINS_HOME"));
    listener.getLogger().println("WHS => workspace = " + workspace);
    listener.getLogger().println("WHS => directed_dast_home = " + iddHome);
    listener.getLogger().println("WHS => HAR Source = " + harSource);

    String iddExe = String.format("%s/target/directed-dast-common", iddHome);
    String harPath = String.format("%s/%s", iddHome, harSource);
    String cmdLine = String.format("%s %s", iddExe, harPath);

    listener.getLogger().println("WHS => harPath = " + harPath);

    FilePath ws = build.getWorkspace();
    if (ws == null) {
      Node node = build.getBuiltOn();
      if (node == null) {
        throw new NullPointerException("no such build node: " + build.getBuiltOnStr());
      }
      throw new NullPointerException("no workspace from node " + node + " which is computer " + node.toComputer() + " and has channel " + node.getChannel());
    }

    int res = -1;
    try {
      EnvVars envVars = build.getEnvironment(listener);
      // on Windows environment variables are converted to all upper case,
      // but no such conversions are done on Unix, so to make this cross-platform,
      // convert variables to all upper cases.
      for (Map.Entry<String,String> e : build.getBuildVariables().entrySet())
        envVars.put(e.getKey(),e.getValue());

      res = (launcher.launch().cmdAsSingleString(cmdLine).envs(envVars).stdout(listener).pwd(ws).start()).join();
      if (res == 0) build.setResult(Result.SUCCESS);
    } catch (IOException e) {
      Util.displayIOException(e, listener);
      Functions.printStackTrace(e, listener.fatalError(Messages.WhiteHatIDDRecorderBuilder_CommandFailed()));
    }
    return res == 0;
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

      save();
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
