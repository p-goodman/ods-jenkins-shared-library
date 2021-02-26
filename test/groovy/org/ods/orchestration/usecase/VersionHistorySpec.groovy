package org.ods.orchestration.usecase

import com.github.tomakehurst.wiremock.core.Options
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.rules.TemporaryFolder
import org.ods.orchestration.service.DocGenService
import org.ods.orchestration.service.JiraService
import org.ods.orchestration.service.LeVADocumentChaptersFileService
import org.ods.orchestration.util.DocumentHistory
import org.ods.orchestration.util.MROPipelineUtil
import org.ods.orchestration.util.PDFUtil
import org.ods.orchestration.util.Project
import org.ods.services.GitService
import org.ods.services.JenkinsService
import org.ods.services.NexusService
import org.ods.services.OpenShiftService
import org.ods.util.ILogger
import org.ods.util.IPipelineSteps
import org.ods.util.Logger
import spock.lang.Specification
import util.FixtureHelper
import util.LoggerStub
import util.PipelineSteps
import static com.github.tomakehurst.wiremock.client.WireMock.*

@Slf4j
class VersionHistorySpec  extends Specification {

    @Rule
    EnvironmentVariables env = new EnvironmentVariables()

    @Rule
    public TemporaryFolder tempFolder

    @Rule
    public WireMockRule jiraServer = new WireMockRule(
        (WireMockConfiguration.wireMockConfig().withRootDirectory("test/resources/wiremock").dynamicPort()))

    Project project
    IPipelineSteps steps
    MROPipelineUtil util
    DocGenService docGen
    JenkinsService jenkins
    JiraUseCase jiraUseCase
    JUnitTestReportsUseCase junit
    LeVADocumentChaptersFileService levaFiles
    NexusService nexus
    OpenShiftService os
    PDFUtil pdfUtil
    SonarQubeUseCase sq
    LeVADocumentUseCase usecase
    ILogger logger
    DocumentHistory docHistory

    def setup() {
        log.info "Using temporal folder:${tempFolder.getRoot()}"

        jenkins = Mock(JenkinsService)
        nexus = Mock(NexusService)
        os = Mock(OpenShiftService)
        sq = Mock(SonarQubeUseCase)
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10000"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10000.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10001"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10001.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10102"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10102.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10200"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10200.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10201"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10201.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10202"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10202.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10203"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10203.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10204"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10204.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10205"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10205.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/createmeta/NET/issuetypes/10206"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/issueTypes-10206.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/api/2/issue/NET-123?fields=customfield_10222"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/customField_10222.json")));
        jiraServer.stubFor(
            get(urlEqualTo("/rest/platform/1.0/docgenreports/NET"))
                .willReturn(aResponse().withStatus(200)
                    .withBodyFile("jira/docgenreports.json")));

        System.setProperty("java.io.tmpdir", tempFolder.getRoot().absolutePath)
        FileUtils.copyDirectory(new FixtureHelper().getResource("Test-1.pdf").parentFile, tempFolder.getRoot());

        steps = new PipelineSteps()
        pdfUtil = new PDFUtil()
        logger = new LoggerStub(true)

        jenkins.unstashFilesIntoPath(_, _, "SonarQube Report") >> true
        project = buildProject() //Spy(buildProject())
        util = new MROPipelineUtil(project, steps, null, logger) // TODO Spy(new MROPipelineUtil(project, steps, null, logger))
        docGen = new DocGenService("http://localhost") //TODO Mock(DocGenService)
        junit = new JUnitTestReportsUseCase(project, steps) // TODO Spy(new JUnitTestReportsUseCase(project, steps))
        levaFiles = new LeVADocumentChaptersFileService(steps) // TODO Mock(LeVADocumentChaptersFileService)
        def jiraService = new JiraService("http://localhost:${jiraServer.port()}", "username", "password")
        jiraUseCase = new JiraUseCase(project, steps, util, jiraService, logger)
        usecase = new LeVADocumentUseCase(project, steps, util, docGen, jenkins, jiraUseCase, junit, levaFiles, nexus, os, pdfUtil, sq)
        project.load(Mock(GitService), jiraUseCase)
    }

    def buildProject() {
        steps.env.BUILD_ID = "1"
        steps.env.WORKSPACE = "${tempFolder.getRoot().absolutePath}/workspace"

        def project = new Project(steps, new Logger(steps, true), [:]).init()
        project.data.buildParams = [:]
        project.data.buildParams.targetEnvironment = "dev"
        project.data.buildParams.targetEnvironmentToken = "D"
        project.data.buildParams.version = "WIP"
        project.data.buildParams.releaseStatusJiraIssueKey = "NET-123"
        project.getOpenShiftApiUrl() >> 'https://api.dev-openshift.com'
        return project
    }

    def "create CFTP"() {
        given:
        jiraUseCase = Spy(new JiraUseCase(project, steps, util, Mock(JiraService), logger))
        usecase = Spy(new LeVADocumentUseCase(project, steps, util, docGen, jenkins, jiraUseCase, junit, levaFiles, nexus, os, pdfUtil, sq))

        // Argument Constraints
        def documentType = LeVADocumentUseCase.DocumentType.CFTP as String

        // Stubbed Method Responses
        def chapterData = ["sec1": [content: "myContent", status: "DONE"]]
        def uri = "http://nexus"
        def documentTemplate = "template"
        def watermarkText = "WATERMARK"

        when:
        usecase.createCFTP()

        then:
        true
    }
}
