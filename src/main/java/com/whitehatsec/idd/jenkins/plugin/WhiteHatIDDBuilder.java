package com.whitehatsec.idd.jenkins.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Plugin;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

public class WhiteHatIDDBuilder extends Builder implements SimpleBuildStep {
  private String harSource;
  private String filterOnSeverity = DescriptorImpl.defaultFilterOnSeverity;
  private String failOnSeverity = DescriptorImpl.defaultFailOnSeverity;
  private ArrayList<WhiteHatIDDHostMapping> hostMapping;

  private static final String IDD_HOME = "DIRECTED_DAST_HOME";

  @DataBoundConstructor
  public WhiteHatIDDBuilder(String harSource) {
    this.harSource = harSource;
    this.hostMapping = new ArrayList<WhiteHatIDDHostMapping>();
  }

  public String getHarSource() {
    return harSource;
  }

  public String getFilterOnSeverity() {
    return filterOnSeverity == null ? DescriptorImpl.defaultFilterOnSeverity : filterOnSeverity;
  }

  public String getFailOnSeverity() {
    return failOnSeverity == null ? DescriptorImpl.defaultFailOnSeverity : failOnSeverity;
  }

  public ArrayList<WhiteHatIDDHostMapping> getHostMapping() {
    return hostMapping == null ? new ArrayList<WhiteHatIDDHostMapping>() : hostMapping;
  }

  @DataBoundSetter
  public void setFilterOnSeverity(String filterOnSeverity) {
    this.filterOnSeverity = EnumUtils.isValidEnum(Severity.class, filterOnSeverity.toUpperCase()) ? filterOnSeverity : DescriptorImpl.defaultFilterOnSeverity;
  }

  @DataBoundSetter
  public void setFailOnSeverity(String failOnSeverity) {
    this.failOnSeverity = EnumUtils.isValidEnum(Severity.class, failOnSeverity.toUpperCase()) ? failOnSeverity : DescriptorImpl.defaultFailOnSeverity;
  }

  @DataBoundSetter
  public void setHostMapping(ArrayList<WhiteHatIDDHostMapping> hostMapping) {
    this.hostMapping = hostMapping != null ? new ArrayList<WhiteHatIDDHostMapping>(hostMapping) : new ArrayList<WhiteHatIDDHostMapping>();
  }

  private String getSettingsPath(EnvVars env, FilePath ws, TaskListener listener) throws IOException, InterruptedException {
    String webAppPath = "";
    Plugin plugin = Jenkins.get().getPlugin("directed-dast");
    if (plugin != null) {
      webAppPath = plugin.getWrapper().baseResourceURL.getFile();
    }
    listener.getLogger().println("webapp path " + webAppPath);

    try {
      FilePath srcFilePath = new FilePath(new File(webAppPath, "settings.default.json"));
      if (srcFilePath.exists()) {
        listener.getLogger().println("default settings exists " + srcFilePath);
      } else {
        listener.getLogger().println("default settings does NOT exist " + srcFilePath);
      }

      FilePath destFilePath = ws.child("idd-settings-jenkins-job-" + env.get("JOB_NAME") + ".json");
      if (destFilePath.exists() && destFilePath.length() > 0) {
        listener.getLogger().println("settings file " + destFilePath);
      } else {
        listener.getLogger().println("copy settings " + srcFilePath + " to " + destFilePath);
        destFilePath.copyFrom(srcFilePath);
      }
      return destFilePath.getRemote();
    } catch (IOException|InterruptedException e) {
      throw e;
    }
  }

  private String getHarSourcePath(EnvVars env, FilePath ws, TaskListener listener) throws IOException, InterruptedException {
    try {
      File file = new File(harSource);
      if (file.isAbsolute()) {
        return harSource;
      }
      FilePath harSourceFilePath = ws.child(harSource);
      if (!harSourceFilePath.exists()) {
        listener.getLogger().println("HAR file does NOT exist: " + harSourceFilePath);
        return harSource;
      }
      return harSourceFilePath.getRemote();
    } catch (IOException|InterruptedException e) {
      throw e;
    }
  }

  private Configuration readSettings(String fname) throws IOException {
    Gson gson = new Gson();

    try {
      InputStream inputStream = new FileInputStream(fname);
      Reader destFileReader = new InputStreamReader(inputStream, "UTF-8");

      Configuration config = gson.fromJson(destFileReader, Configuration.class);
      destFileReader.close();

      return config;
    } catch (IOException e) {
      throw e;
    }
  }

  private Configuration updateHostMappingSettings(Configuration config) {
    LinkedHashMap<String, HostMapping> map = new LinkedHashMap<>();

    for (WhiteHatIDDHostMapping hm: hostMapping) {
      if (StringUtils.isBlank(hm.getFromHost()) || StringUtils.isBlank(hm.getToHost())) {
        continue;
      }
      HostMapping h = new HostMapping();
      h.setEnable(hm.getEnableHostMapping());
      h.setFrom(hm.getFromHost());
      h.setTo(hm.getToHost());
      map.put(hm.getFromHost(), h);
    }
    config.setHosts(new LinkedList<HostMapping>(map.values()));

    config.setFilterOnSeverity(filterOnSeverity);
    config.setFailOnSeverity(failOnSeverity);

    return config;
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

  private void invokeIDD(String settingsPath, String harSourcePath, EnvVars env, FilePath workspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException, AbortException {
    try {
      listener.getLogger().println("execute IDD with harSource: " + harSourcePath);
      String cmdLine = String.format("%s/target/directed-dast-common -settings-file %s %s", env.get(IDD_HOME), settingsPath, harSourcePath);
      int exitCode = (launcher.launch().cmdAsSingleString(cmdLine).envs(env).stdout(listener).pwd(workspace).start()).join();
      listener.getLogger().println("IDD returns exit code: " + exitCode);

      if (exitCode != 0) {
        throw new AbortException("IDD did not succeed");
      }
    } catch (Exception e) {
      throw e;
    }
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {

    EnvVars env = run.getEnvironment(listener);
    if (workspace == null) {
      throw new IllegalStateException("workspace does not yet exist for this job " + env.get("JOB_NAME"));
    }

    if (StringUtils.isBlank(env.get(IDD_HOME))) {
      throw new InterruptedException("required env variable is not set yet: " + IDD_HOME);
    }

    try {
      String settingsPath = getSettingsPath(env, workspace, listener);

      listener.getLogger().println("read settings " + settingsPath);
      Configuration config = readSettings(settingsPath);

      config = updateHostMappingSettings(config);

      listener.getLogger().println("save settings " + settingsPath);
      saveSettings(config, settingsPath);

      listener.getLogger().println("env var " + IDD_HOME + " is " + env.get(IDD_HOME));

      String harSourcePath = getHarSourcePath(env, workspace, listener);
      File file = new File(harSourcePath);

      if (file.isDirectory()) {
        listener.getLogger().println("harSource is a directory: " + harSourcePath);

        try (Stream<Path> walk = Files.walk(Paths.get(harSourcePath))) {
          List<String> result = walk.map(x -> x.toString())
            .filter(f -> f.endsWith(".har")).collect(Collectors.toList());
          result.forEach(harPath -> {
            try {
              invokeIDD(settingsPath, harPath, env, workspace, launcher, listener);
            } catch (AbortException e) {
              run.setResult(Result.FAILURE);
              throw new RuntimeException(e);
            } catch (IOException e) {
              Util.displayIOException(e, listener);
              Functions.printStackTrace(e, listener.fatalError("command execution failed when harSource is a dir"));
              throw new RuntimeException(e);
            } catch (InterruptedException e) {
              Functions.printStackTrace(e, listener.fatalError("job interrupted when harSource is a dir"));
              throw new RuntimeException(e);
            }
          });
        } catch(IOException e) {
          throw e;
        }
      } else {
        listener.getLogger().println("harSource is a file: " + harSourcePath);
        invokeIDD(settingsPath, harSourcePath, env, workspace, launcher, listener);
      }
    } catch (AbortException e) {
      run.setResult(Result.FAILURE);
      throw e;
    } catch (IOException e) {
      Util.displayIOException(e, listener);
      Functions.printStackTrace(e, listener.fatalError("command execution failed"));
      throw e;
    } catch (InterruptedException e) {
      Functions.printStackTrace(e, listener.fatalError("job interrupted"));
      throw e;
    }
  }

  @Symbol("whsIdd")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    public static final String defaultFilterOnSeverity = Severity.HIGH.level;
    public static final String defaultFailOnSeverity = Severity.NOTE.level;

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return Messages.WhiteHatIDDBuilder_DescriptorImpl_DisplayName();
    }

    public FormValidation doCheckHarSource(@QueryParameter String value) throws IOException, ServletException {
      if (StringUtils.isBlank(value)) {
        return FormValidation.error(Messages.WhiteHatIDDBuilder_DescriptionImpl_errors_requiredHarSource());
      }
      return FormValidation.ok();
    }

    public ListBoxModel doFillFilterOnSeverityItems() {
      return fillSeverityItems();
    }

    public ListBoxModel doFillFailOnSeverityItems() {
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
