package org.ods.orchestration.usecase

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ods.services.BitbucketService
import org.ods.util.ILogger
import org.ods.util.IPipelineSteps
import spock.lang.Specification
import util.FixtureHelper
import util.LoggerStub
import util.PipelineSteps
import util.wiremock.BitbucketServiceMock

import static org.assertj.core.api.Assertions.*;

@Slf4j
class BitbucketTraceabilityUseCaseSpec extends Specification {
    private static final String EXPECTED_BITBUCKET_CSV = "expected/bitbucket.csv"

    // Change for local development or CI testing
    private static final Boolean RECORD_WIREMOCK = true
    private static final String BB_URL_TO_RECORD = ""
    private static final String BB_TOKEN = ""
    private static final String PROJECT = "EDPC"

    @Rule
    public TemporaryFolder tempFolder

    BitbucketServiceMock bitbucketServiceMock
    IPipelineSteps steps
    ILogger logger
    BitbucketService bitbucketService

    def setup() {
        log.info "Using temporal folder:${tempFolder.getRoot()}"

        steps = new PipelineSteps()
        steps.env.WORKSPACE = tempFolder.getRoot().absolutePath
        logger = new LoggerStub(log)
        bitbucketServiceMock = new BitbucketServiceMock().setUp("csv").startServer(RECORD_WIREMOCK, BB_URL_TO_RECORD)
        bitbucketService = Spy(
                new BitbucketService(
                        null,
                        bitbucketServiceMock.getWireMockServer().baseUrl(),
                        PROJECT,
                        "passwordCredentialsId",
                        logger))
        bitbucketService.getToken() >> BB_TOKEN
    }

    def cleanup() {
        bitbucketServiceMock.tearDown()
    }

    def "Generate the csv source code review file"() {
        given: "There are two Bitbucket repositories"
        def useCase = new BitbucketTraceabilityUseCase(bitbucketService, steps)

        when: "the source code review file is generated"
        def actualFile = useCase.generateSourceCodeReviewFile()

        then: "the generated file is as the expected csv file"
        reportInfo "Generated csv file:<br/>${readSomeLines(actualFile)}"
        def expectedFile = new FixtureHelper().getResource(EXPECTED_BITBUCKET_CSV)
        assertThat(new File(actualFile)).exists().isFile().hasSameTextualContentAs(expectedFile);
    }

    private String readSomeLines(String filePath){
        File file = new File(filePath)
        def someLines = 3
        String lines = ""
        file.withReader { r -> while( someLines-- > 0 && (( lines += r.readLine() + "<br/>" ) != null));}
        lines += "..."
        return lines
    }
}
