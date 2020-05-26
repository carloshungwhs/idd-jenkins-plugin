package com.whitehatsec.idd.jenkins.plugin;

import java.io.IOException;
import java.util.List;
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
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

public class WhiteHatIDDRecorder extends Recorder {
  //private String iddHome;
  private Boolean useLocalConfig;
  private String harSource;
  private Integer severityLevel;
  private Integer severityFailLevel;

  private static final String IDD_HOME = "DIRECTED_DAST_HOME";

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
/*
  public void setHarSource(String harSource) {
    this.harSource = harSource;
  }
*/
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
  public Boolean setUseLocalConfig() {
    return useLocalConfig;
  }
  */

  /*
  public String getIddHome() {
    return this.iddHome;
  }

  public void setIddHome(String iddHome) {
    this.iddHome = iddHome;
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
    //build.setResult(Result.SUCCESS);

    FilePath ws = build.getWorkspace();
    if (ws == null) {
      Node node = build.getBuiltOn();
      if (node == null) {
        throw new NullPointerException("no such build node: " + build.getBuiltOnStr());
      }
      throw new NullPointerException("no workspace from node " + node + " which is computer " + node.toComputer() + " and has channel " + node.getChannel());
    }
    FilePath script=null;
    int r = -1;
    try {
      /*
      try {
        script = createScriptFile(ws);
      } catch (IOException e) {
        Util.displayIOException(e,listener);
        Functions.printStackTrace(e, listener.fatalError(Messages.CommandInterpreter_UnableToProduceScript()));
        return false;
      }
      */

      try {
        EnvVars envVars = build.getEnvironment(listener);
        // on Windows environment variables are converted to all upper case,
        // but no such conversions are done on Unix, so to make this cross-platform,
        // convert variables to all upper cases.
        for (Map.Entry<String,String> e : build.getBuildVariables().entrySet())
          envVars.put(e.getKey(),e.getValue());

        r = (launcher.launch().cmdAsSingleString(cmdLine).envs(envVars).stdout(listener).pwd(ws).start()).join();
        //r = join(launcher.launch().cmd(buildCommandLine(script)).envs(envVars).stdout(listener).pwd(ws).start());

        /*
        if (isErrorlevelForUnstableBuild(r)) {
          build.setResult(Result.UNSTABLE);
          r = 0;
        }
        */
      } catch (IOException e) {
        Util.displayIOException(e, listener);
        Functions.printStackTrace(e, listener.fatalError("error" /*Messages.CommandInterpreter_CommandFailed()*/));
      }
      build.setResult(Result.SUCCESS);
      return r==0;
    } finally {
      listener.fatalError("error"/*Messages.CommandInterpreter_UnableToDelete(script)*/);
      /*
      try {
        if (script!=null)
          script.delete();
      } catch (IOException e) {
        if (r==-1 && e.getCause() instanceof ChannelClosedException) {
          // JENKINS-5073
          // r==-1 only when the execution of the command resulted in IOException,
          // and we've already reported that error. A common error there is channel
          // losing a connection, and in that case we don't want to confuse users
          // by reporting the 2nd problem. Technically the 1st exception may not be
          // a channel closed error, but that's rare enough, and JENKINS-5073 is common enough
          // that this suppressing of the error would be justified
          //LOGGER.log(Level.FINE, "Script deletion failed", e);
        } else {
          Util.displayIOException(e,listener);
          Functions.printStackTrace(e, listener.fatalError("error"/*Messages.CommandInterpreter_UnableToDelete(script)));
        }
      } catch (Exception e) {
        Functions.printStackTrace(e, listener.fatalError("error"/*Messages.CommandInterpreter_UnableToDelete(script)));
      }
      */
    }

    /*
    EnvVars en = build.getEnvironment(listener);
    String envVarValue = env.get("ENTER_ENV_VAR_HERE");
    String expandedDbUrl = env.expand(dbUrl);
    WORKSPACE
    */
  /*
    try {
      // Map<String, String> environment = build.getEnvVars();
      //EnvVars environment = build.getEnvironment(listener);
      build.setResult(Result.SUCCESS);
    } catch (InterruptedException e) {
      listener.getLogger().println("WhiteHat Security ID-DAST - Unable to start: " + e.getMessage());
    } catch (IOException e) {
      listener.getLogger().println("WhiteHat Security ID-DAST - Unable to start " + e.getMessage());
    }
  */
    //return true;
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

    public String getEnvIddHome() {
      //DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperty1 = Jenkins.get().getGlobalNodeProperties();
      /*
      for (NodeProperty nodeProperty: Jenkins.get().getGlobalNodeProperties()) {
        Environment environment = nodeProperty.setUp(AbstractBuild.this, l, listener);
      }
      */

      DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = Jenkins.get().getGlobalNodeProperties();
      List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class);

      EnvVars envVars = null;

      if (envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0) {
        return "";
      }
      envVars = envVarsNodePropertyList.get(0).getEnvVars();
      return envVars.get(IDD_HOME);

      //for (NodeProperty nodeProperty: Jenkins.get().getGlobalNodeProperties()) {
        /*
        Environment environment = nodeProperty.setUp(AbstractBuild.this, l, listener);
        if (environment != null) {
            buildEnvironments.add(environment);
        }
      }
      */
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
