package com.whitehatsec.idd.jenkins.plugin;

import java.io.IOException;
import java.util.ArrayList;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;

public class WhiteHatIDDBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String harSource = "console_test.har";

    @Before
    public void setEnvironmentVariables() throws IOException {
      EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
      EnvVars envVars = prop.getEnvVars();
      envVars.put("DIRECTED_DAST_HOME", "sampleEnvVarValue");
      jenkins.jenkins.getGlobalNodeProperties().add(prop);
    }

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new WhiteHatIDDBuilder(harSource));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new WhiteHatIDDBuilder(harSource), project.getBuildersList().get(0));
    }

    @Test
    public void testConfigRoundtripWithAllValuesSet() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        WhiteHatIDDBuilder builder = new WhiteHatIDDBuilder(harSource);
        builder.setFilterOnSeverity(Severity.CRITICAL.level);
        builder.setFailOnSeverity(Severity.LOW.level);
        ArrayList<WhiteHatIDDHostMapping> hostMapping;
        hostMapping = new ArrayList<WhiteHatIDDHostMapping>();
        hostMapping.add(new WhiteHatIDDHostMapping(true, "www.fromHost.test", "www.toHost.test"));
        builder.setHostMapping(hostMapping);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        WhiteHatIDDBuilder lhs = new WhiteHatIDDBuilder(harSource);
        lhs.setFilterOnSeverity(Severity.CRITICAL.level);
        lhs.setFailOnSeverity(Severity.LOW.level);
        lhs.setHostMapping(hostMapping);
        jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        WhiteHatIDDBuilder builder = new WhiteHatIDDBuilder(harSource);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.SUCCESS, project);
        jenkins.assertLogContains("execute IDD with harSource: " + harSource, build);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "   whsIdd '" + harSource + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("execute IDD with harSource: " + harSource, completedBuild);
    }

    @Test
    public void testScriptedPipelineWithAllValuesSet() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "   whsIdd '" + harSource + "'\n"
                + "   filterOnSeverity: 'Note'\n"
                + "   failOnSeverity: 'Note'\n"
                + "   hostMapping: [\n"
                + "     whsIddHost(enableHostMapping: true, fromHost: 'a', toHost: 'b')\n"
                + "   ]\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("execute IDD with harSource: " + harSource, completedBuild);
    }
}