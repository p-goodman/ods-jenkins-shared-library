package org.ods.orchestration.usecase

import groovy.transform.builder.Builder
import groovy.transform.builder.ExternalStrategy
import org.ods.orchestration.util.Project
import org.ods.services.BitbucketService
import org.ods.util.IPipelineSteps

import java.text.SimpleDateFormat

class BitbucketTraceabilityUseCase {
    private static final String CSV_FILE = "source-code-review.csv"
    private static final int PAGE_LIMIT = 10

    private BitbucketService bitbucketService
    private IPipelineSteps steps
    private Project project

    BitbucketTraceabilityUseCase(BitbucketService bitbucketService, IPipelineSteps steps, Project project){
        this.steps = steps
        this.project = project
        this.bitbucketService = bitbucketService
    }

    /**
     * Create a CSV file that contains the following records
     * for every merge event into the integration branch of every ODS component:
     * @return absolutePath of the created file
     */
    @SuppressWarnings(['JavaIoPackageAccess'])
    String generateSourceCodeReviewFile(){
        def file = new File("${steps.env.WORKSPACE}/${CSV_FILE}")

        def token = bitbucketService.getToken()
        List<String> repos = getRepositories()
        repos.each {
            processRepo(token, it, file)
        }
        return file.absolutePath
    }

    private void processRepo(String token, String repo, File file) {
        def nextPage = true
        def nextPageStart = 0
        while (nextPage) {
            def commits = bitbucketService.getCommitsForMainBranch(token, repo, PAGE_LIMIT, nextPageStart)
            if (commits.isLastPage) {
                nextPage = false
            } else {
                nextPageStart = commits.nextPageStart
            }
            processCommits(token, repo, commits, file)
        }
    }

    List<String> getRepositories() {
        List<String> result = []
        this.project.getRepositories().url.each { String repo ->
            if(repo.lastIndexOf("/")>=0){
                repo = repo.substring(repo.lastIndexOf("/") + 1)
            }
            result << repo.replaceAll(".git", "")
        }
        return result
    }

    private void processCommits(String token, String repo, Map commits, File file) {
        commits.values.each { commit ->
            Map mergedPR = bitbucketService.getPRforMergedCommit(token, repo, commit.id)
            // Only changes in PR
            if(mergedPR.values) {
                def record = new Record(getDateWithFormat(commit.committerTimestamp),
                    getAuthor(commit.author),
                    getReviewers(mergedPR.values[0]?.reviewers),
                    mergedPR.values[0]?.links?.self?.getAt(0)?.href,
                    commit.id,
                    repo)
                writeCSVRecord(file, record)
            }
        }
    }

    private void writeCSVRecord(File file, Record record) {
        file << record.toString()
    }

    private Developer getAuthor(Map author) {
        return new Developer(author.name, author.emailAddress)
    }

    private List getReviewers(List reviewers) {
        List<Developer> approvals = []
        reviewers.each {
            if(it.approved) {
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
        static final String CSV = "|"

        String date
        Developer author
        List<Developer> reviewers
        String mergeRequestURL
        String mergeCommitSHA
        String componentName

        @SuppressWarnings(['ParameterCount'])
        Record(String date, Developer author, List<Developer> reviewers, String mergeRequestURL,
               String mergeCommitSHA, String componentName) {
            this.date = date
            this.author = author
            this.reviewers = reviewers
            this.mergeRequestURL = mergeRequestURL
            this.mergeCommitSHA = mergeCommitSHA
            this.componentName = componentName
        }

        String reviewersAsList() {
            String reviewersList = ""
            if (reviewers && reviewers.size()>0) {
                def reviewerListString = reviewers.toString()
                reviewersList = reviewerListString.substring(1, reviewerListString.length() - 1);
            }

            return reviewersList
        }

        @Override
        String toString() {
            return date + CSV + author + CSV + reviewersAsList() + CSV +
                mergeRequestURL + CSV + mergeCommitSHA + CSV + componentName + "\n"
        }
    }
/*
    @Builder(builderStrategy = ExternalStrategy, forClass = Record)
    private class RecordBuilder {}
*/
    private class Developer {
        public static final String FIELD_SEPARATOR = ';'
        String name
        String mail

        Developer(String name, String mail) {
            this.name = name
            this.mail = mail
        }

        @Override
        String toString() {
            return name + FIELD_SEPARATOR + mail
        }
    }
/*
    @Builder(builderStrategy = ExternalStrategy, forClass = Developer)
    private class DeveloperBuilder {}*/
}
