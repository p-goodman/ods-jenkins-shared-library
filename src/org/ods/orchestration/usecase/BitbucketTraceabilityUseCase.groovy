package org.ods.orchestration.usecase

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import org.ods.orchestration.util.Project
import org.ods.services.BitbucketService
import org.ods.util.IPipelineSteps

import java.text.SimpleDateFormat

class BitbucketTraceabilityUseCase {

    private static final String JSON_FILE = "source-code-review.json"
    private static final int PAGE_LIMIT = 10

    private final BitbucketService bitbucketService
    private final IPipelineSteps steps
    private final Project project

    BitbucketTraceabilityUseCase(BitbucketService bitbucketService, IPipelineSteps steps, Project project) {
        this.steps = steps
        this.project = project
        this.bitbucketService = bitbucketService
    }

    /**
     * Create a JSON file that contains the following records
     * for every merge event into the integration branch of every ODS component:
     * @return absolutePath of the created file
     */
    @SuppressWarnings(['JavaIoPackageAccess'])
    String generateSourceCodeReviewFile() {
        def file = new File("${steps.env.WORKSPACE}/${JSON_FILE}")
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()

        def token = bitbucketService.getToken()
        List<Map> repos = getRepositories()
        def result = []
        repos.each {
            result += (processRepo(token, it)?:[])
        }
        writeJSONFile(file, result)

        return file.absolutePath
    }


    /**
     * Read an existing JSON file and parse the info to obtain an structured List of data.
     *
     * @param filePath The json
     * @return List of commits
     */
    @SuppressWarnings(['JavaIoPackageAccess'])
    List<Map> readSourceCodeReviewFile(String filePath) {
        def file = new File(filePath)
        def jsonSlurper = new JsonSlurperClassic()

        return jsonSlurper.parse(file)
    }

    private List<Record> processRepo(String token, Map repo) {
        def nextPage = true
        def nextPageStart = 0
        def result = []
        while (nextPage) {
            def commits = bitbucketService.getCommitsForMainBranch(token, repo.repo, PAGE_LIMIT, nextPageStart)
            if (commits.isLastPage) {
                nextPage = false
            } else {
                nextPageStart = commits.nextPageStart
            }
            result += (processCommits(token, repo, commits)?:[])
        }

        return result
    }

    private List<Map> getRepositories() {
        List<Map> result = []
        this.project.getRepositories().each { repository ->
            result << [repo: "${project.data.metadata.id.toLowerCase()}-${repository.id}", branch: repository.branch]
        }
        return result
    }

    private List<Record> processCommits(String token, Map repo, Map commits) {
        def result = []
        commits.values.each { commit ->
            Map mergedPR = bitbucketService.getPRforMergedCommit(token, repo.repo, commit.id)
            // Only changes in PR and destiny integration branch
            if (mergedPR.values
                && mergedPR.values[0].toRef.displayId == repo.branch) {
                def record = new Record(getDateWithFormat(commit.committerTimestamp),
                    getAuthor(commit.author),
                    getReviewers(mergedPR.values[0].reviewers),
                    mergedPR.values[0].links.self[(0)].href,
                    commit.id,
                    repo.repo)
                result << record
            }
        }
        return result
    }

    @SuppressWarnings(['JavaIoPackageAccess'])
    private void writeJSONFile(File file, List data) {
        def json = JsonOutput.toJson(data)
        def jsonPretty = JsonOutput.prettyPrint(json)
        file.write(jsonPretty)
    }

    private Developer getAuthor(Map author) {
        return new Developer(author.name, author.emailAddress)
    }

    private List getReviewers(List reviewers) {
        List<Developer> approvals = []
        reviewers.each {
            if (it.approved) {
                approvals << new Developer(it.user.name, it.user.emailAddress)
            }
        }

        return approvals
    }

    private String getDateWithFormat(Long timestamp) {
        Date dateObj =  new Date(timestamp)
        return new SimpleDateFormat('yyyy-MM-dd', Locale.getDefault()).format(dateObj)
    }

    private class Record {

        String commitDate
        Developer author
        List<Developer> reviewers
        String mergeRequestURL
        String mergeCommitSHA
        String componentName

        @SuppressWarnings(['ParameterCount'])
        Record(String date, Developer author, List<Developer> reviewers, String mergeRequestURL,
               String mergeCommitSHA, String componentName) {
            this.commitDate = date
            this.author = author
            this.reviewers = reviewers
            this.mergeRequestURL = mergeRequestURL
            this.mergeCommitSHA = mergeCommitSHA
            this.componentName = componentName
        }

    }

    private class Developer {

        String name
        String mail

        Developer(String name, String mail) {
            this.name = name
            this.mail = mail
        }

    }

}
