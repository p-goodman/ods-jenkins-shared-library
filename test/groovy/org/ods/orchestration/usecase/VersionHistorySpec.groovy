package org.ods.orchestration.usecase

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
import util.PipelineSteps

class VersionHistorySpec  extends Specification {

    @Rule
    EnvironmentVariables env = new EnvironmentVariables()

    @Rule
    public TemporaryFolder tempFolder

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
    PDFUtil pdf
    SonarQubeUseCase sq
    LeVADocumentUseCase usecase
    Logger logger
    DocumentHistory docHistory

    def setup() {
        steps = Spy(util.PipelineSteps)
        util = Mock(MROPipelineUtil)
        docGen = Mock(DocGenService)
        jenkins = Mock(JenkinsService)
        junit = Spy(new JUnitTestReportsUseCase(project, steps))
        levaFiles = Mock(LeVADocumentChaptersFileService)
        nexus = Mock(NexusService)
        os = Mock(OpenShiftService)
        pdf = Mock(PDFUtil)
        sq = Mock(SonarQubeUseCase)
        logger = Mock(Logger)
        def git = Mock(GitService)
        project = Spy(createProject())

        def jira = Mock(JiraService)
        jiraUseCase = Spy(new JiraUseCase(project, steps, util, jira, logger))

        project.load(git, jiraUseCase)
        project.data.buildParams = [:]
        project.data.buildParams.targetEnvironment = "dev"
        project.data.buildParams.targetEnvironmentToken = "D"
        project.data.buildParams.version = "WIP"
        usecase = Spy(new LeVADocumentUseCase(project, steps, util, docGen, jenkins, jiraUseCase, junit, levaFiles, nexus, os, pdf, sq))
        project.getOpenShiftApiUrl() >> 'https://api.dev-openshift.com'


        jenkins.unstashFilesIntoPath(_, _, "SonarQube Report") >> true
    }

    static Project createProject() {
        def steps = new PipelineSteps()
        steps.env.WORKSPACE = ""
        def config = [:]
        return new Project(steps, new Logger(steps, true), config)
    }

    def "create CFTP"() {
        given:
        jiraUseCase = Spy(new JiraUseCase(project, steps, util, Mock(JiraService), logger))
        usecase = Spy(new LeVADocumentUseCase(project, steps, util, docGen, jenkins, jiraUseCase, junit, levaFiles, nexus, os, pdf, sq))

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
