package util

import groovy.json.JsonSlurperClassic
import kong.unirest.Unirest
import org.ods.orchestration.service.JiraService

class GenerateJiraJson {

    public static final String JIRA_JSON_FILES = "build/tmp/json/jira"

    static void main(String[] args) {
        def projectKey = ""
        def version = ""
        new GenerateJiraJson().start(projectKey, version)
    }

    void start(projectKey, version) {
        jiraOutputDirExist()
        def service = new JiraJsonService("", "openshift", "openshift")

        generateJsonFile("getDocGenData", service.getDocGenData(projectKey))
        generateJsonFile("getDeltaDocGenData", service.getDeltaDocGenData(projectKey, version))
       // generateJsonFile("getDocGenData", service.getIssueTypeMetadata(projectKey, issueTypeId))
        generateJsonFile("getIssueTypes", service.getIssueTypes(projectKey))
        generateJsonFile("getVersionsForProject", service.getVersionsForProject(projectKey))
        generateJsonFile("getProject", service.getProject(projectKey))
        generateJsonFile("getProjectVersions", service.getProjectVersions(projectKey))
        //generateJsonFile("getLabelsFromIssue", service.getLabelsFromIssue(issueIdOrKey))
        //generateJsonFile("searchByJQLQuery", service.searchByJQLQuery(query))
       // generateJsonFile("getTextFieldsOfIssue", service.getTextFieldsOfIssue(issueIdOrKey, List fields))
        //generateJsonFile("getTextFieldsOfIssue", service.isVersionEnabledForDelta(projectKey, versionName))

    }

    private void generateJsonFile(String fileName, def jsonData) {
        new File("${JIRA_JSON_FILES}/${fileName}").text = jsonData
    }

    private void jiraOutputDirExist() {
        def json_path = new File(JIRA_JSON_FILES)
        if (json_path.exists()) {
            json_path.deleteDir()
        }
    }
}
