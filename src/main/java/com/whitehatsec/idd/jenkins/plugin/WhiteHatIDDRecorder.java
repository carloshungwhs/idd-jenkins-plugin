package com.whitehatsec.idd.jenkins.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

public class WhiteHatIDDRecorder extends Recorder {
  private boolean useLocalConfig;
  private String harSource;
  private String severityLevel;
  private String severityFailLevel;

  private static final String IDD_HOME = "DIRECTED_DAST_HOME";

  // call on save job config
  @DataBoundConstructor
  public WhiteHatIDDRecorder(boolean useLocalConfig, String harSource, String severityLevel, String severityFailLevel) {
    this.useLocalConfig = useLocalConfig;
    this.harSource = harSource;
    this.severityLevel = severityLevel;
    this.severityFailLevel = severityFailLevel;
  }

  public String getHarSource() {
    return harSource;
  }

  public String getSeverityLevel() {
    return severityLevel;
  }

  public String getSeverityFailLevel() {
    return severityFailLevel;
  }

  public boolean isUseLocalConfig() {
    return useLocalConfig;
  }

  private Configuration readSettings(String fname) throws FileNotFoundException, UnsupportedEncodingException, IOException {
    Gson gson = new Gson();

    try {
      InputStream inputStream = new FileInputStream(fname);
      Reader destFileReader = new InputStreamReader(inputStream, "UTF-8");

      Configuration config = gson.fromJson(destFileReader, Configuration.class);
      destFileReader.close();

      return config;
    } catch (Exception e) {
      throw e;
    }
  }

  private void saveSettings(Configuration config, String fname) throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    try {
      OutputStream fileStream = new FileOutputStream(fname);
      Writer writer = new OutputStreamWriter(fileStream, "UTF-8");

      gson.toJson(config, writer); 
      writer.close();
    } catch (IOException e) {
      throw e;
    }
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {

    EnvVars env = build.getEnvironment(listener);
    FilePath ws = build.getWorkspace();
    if (ws == null) {
      throw new IllegalStateException("workspace does not yet exist for this job " + env.get("JOB_NAME"));
    }

    String iddHome = env.get(IDD_HOME);

    // TODO: Be able to read settings.default.json packaged in hpi
    FilePath srcFilePath = new FilePath(new File(iddHome, "/resources/settings.default.json"));
    FilePath destFilePath = ws.child("settings-jenkins-" + env.get("JOB_NAME") + ".json");

    int res = -1;
    try {
      /*
      String location = Jenkins.get().getPlugin("directed-dast").getWrapper().baseResourceURL.getFile();
      listener.getLogger().println("location = %s" + location);

      FilePath locationFilePath = new FilePath(new File(location));

      FilePath indexFilePath = new FilePath(new File(location, "index.jelly"));
      FilePath settingsFilePath = new FilePath(new File(location, "settings.debug.json"));
      listener.getLogger().println("index.jelly = " + indexFilePath);
      listener.getLogger().println("settingsFilePath = " + settingsFilePath);

      if (indexFilePath.exists()) {
        listener.getLogger().println("index.jelly exists");
      }
      if (settingsFilePath.exists()) {
        listener.getLogger().println("settings.debug.json exists");
      }
      */

      if (destFilePath.exists() && destFilePath.length() > 0) {
        listener.getLogger().println("settings file " + destFilePath);
      } else {
        listener.getLogger().println("copy settings " + srcFilePath + " to " + destFilePath);
        destFilePath.copyFrom(srcFilePath);
      }

      listener.getLogger().println("read settings " + destFilePath);
      Configuration config = readSettings(destFilePath.getRemote());

      // update settings
      config.setSeverityLevel(severityLevel);
      config.setSeverityFailLevel(severityFailLevel);

      listener.getLogger().println("save settings " + destFilePath);
      saveSettings(config, destFilePath.getRemote());

      // on Windows environment variables are converted to all upper case,
      // but no such conversions are done on Unix, so to make this cross-platform,
      // convert variables to all upper cases.
      for (Map.Entry<String,String> e : build.getBuildVariables().entrySet())
        env.put(e.getKey(),e.getValue());

      String cmdLine = String.format("directed-dast-common -settings-file %s %s", destFilePath, harSource);

      res = (launcher.launch().cmdAsSingleString(cmdLine).envs(env).stdout(listener).pwd(ws).start()).join();
      if (res == 0) build.setResult(Result.SUCCESS);
    } catch (IOException e) {
      Util.displayIOException(e, listener);
      Functions.printStackTrace(e, listener.fatalError(Messages.WhiteHatIDDRecorderBuilder_CommandFailed()));
    } catch (InterruptedException e) {
      Functions.printStackTrace(e, listener.fatalError(Messages.WhiteHatIDDRecorderBuilder_JobInterrupted()));
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
    private String severityLevel;
    private String severityFailLevel;

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

    // call on save global config
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
      harSource = json.getString("harSource");
      severityLevel = json.getString("severityLevel");
      severityFailLevel= json.getString("severityFailLevel");

      save();
      return super.configure(req, json);
    }

    public String getHarSource() {
      return harSource;
    }

    public String getSeverityLevel() {
      return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
      this.severityLevel = severityLevel;
    }

    public String defaultSeverityLevel() {
      return Severity.HIGH.level;
    }

    public String getSeverityFailLevel() {
      return severityFailLevel;
    }

    public String defaultSeverityFailLevel() {
      return Severity.NOTE.level;
    }

    public FormValidation doCheckHarPath(@QueryParameter String value) throws IOException, ServletException {
      if (StringUtils.isBlank(value)) {
        return FormValidation.error(Messages.WhiteHatIDDRecorderBuilder_DescriptionImpl_errors_requiredHarPath());
      }
      return FormValidation.ok();
    }

    public ListBoxModel doFillSeverityLevelItems() {
      return fillSeverityItems();
    }

    public ListBoxModel doFillSeverityFailLevelItems() {
      return fillSeverityItems();
    }

    private ListBoxModel fillSeverityItems() {
      ListBoxModel items = new ListBoxModel();
      for (Severity severity : Severity.values()) {
        items.add(severity.name(), severity.level);
      }
      return items;
    }
  }
}
