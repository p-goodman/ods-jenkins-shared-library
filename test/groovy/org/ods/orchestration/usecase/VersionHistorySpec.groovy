package org.ods.orchestration.usecase

import com.github.tomakehurst.wiremock.core.Options
import com.github.tomakehurst.wiremock.junit.WireMockRule
import groovy.util.logging.Log
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

@Log
class VersionHistorySpec  extends Specification {

    @Rule
    EnvironmentVariables env = new EnvironmentVariables()

    @Rule
    public TemporaryFolder tempFolder

    @Rule
    public WireMockRule jiraWireMockRule = new WireMockRule(Options.DYNAMIC_PORT);

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
        System.setProperty("java.io.tmpdir", tempFolder.getRoot().absolutePath)
        FileUtils.copyDirectory(new FixtureHelper().getResource("Test-1.pdf").parentFile, tempFolder.getRoot());

        steps = new PipelineSteps()
        pdfUtil = new PDFUtil()
        logger = new LoggerStub(true)

        jenkins = Mock(JenkinsService)
        jenkins.unstashFilesIntoPath(_, _, "SonarQube Report") >> true
        project = Spy(buildProject())
        util = Spy(new MROPipelineUtil(project, steps, null, logger))
        docGen = Mock(DocGenService)
        junit = Spy(new JUnitTestReportsUseCase(project, steps))
        levaFiles = Mock(LeVADocumentChaptersFileService)
        nexus = Mock(NexusService)
        os = Mock(OpenShiftService)
        sq = Mock(SonarQubeUseCase)
        def jiraService = new JiraService("http://localhost:${jiraWireMockRule.port()}", "username", "password")
        jiraUseCase = new JiraUseCase(project, steps, util, jiraService, logger)
        usecase = Spy(new LeVADocumentUseCase(project, steps, util, docGen, jenkins, jiraUseCase, junit, levaFiles, nexus, os, pdfUtil, sq))
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
