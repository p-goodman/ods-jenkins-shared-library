package org.ods.orchestration.scheduler

import com.cloudbees.groovy.cps.NonCPS
import org.ods.orchestration.usecase.LeVADocumentUseCase
import org.ods.orchestration.util.Environment
import org.ods.orchestration.util.MROPipelineUtil
import org.ods.orchestration.util.Project
import org.ods.util.ILogger
import org.ods.util.IPipelineSteps

@SuppressWarnings(['LineLength', 'SpaceAroundMapEntryColon'])
class LeVADocumentScheduler extends DocGenScheduler {

    // Document types per GAMP category
    private static Map GAMP_CATEGORIES = [
        "1": [
            LeVADocumentUseCase.DocumentType.CSD as String,
            LeVADocumentUseCase.DocumentType.RA as String,
            LeVADocumentUseCase.DocumentType.SSDS as String,
            LeVADocumentUseCase.DocumentType.TIP as String,
            LeVADocumentUseCase.DocumentType.TIR as String,
            LeVADocumentUseCase.DocumentType.OVERALL_TIR as String,
            LeVADocumentUseCase.DocumentType.IVP as String,
            LeVADocumentUseCase.DocumentType.IVR as String,
            LeVADocumentUseCase.DocumentType.CFTP as String,
            LeVADocumentUseCase.DocumentType.CFTR as String,
            LeVADocumentUseCase.DocumentType.TCP as String,
            LeVADocumentUseCase.DocumentType.TCR as String,
            LeVADocumentUseCase.DocumentType.DIL as String,
        ],
        "3": [
            LeVADocumentUseCase.DocumentType.CSD as String,
            LeVADocumentUseCase.DocumentType.RA as String,
            LeVADocumentUseCase.DocumentType.SSDS as String,
            LeVADocumentUseCase.DocumentType.TIP as String,
            LeVADocumentUseCase.DocumentType.TIR as String,
            LeVADocumentUseCase.DocumentType.OVERALL_TIR as String,
            LeVADocumentUseCase.DocumentType.IVP as String,
            LeVADocumentUseCase.DocumentType.IVR as String,
            LeVADocumentUseCase.DocumentType.CFTP as String,
            LeVADocumentUseCase.DocumentType.CFTR as String,
            LeVADocumentUseCase.DocumentType.TCP as String,
            LeVADocumentUseCase.DocumentType.TCR as String,
            LeVADocumentUseCase.DocumentType.DIL as String,
            LeVADocumentUseCase.DocumentType.TRC as String,
        ],
        "4": [
            LeVADocumentUseCase.DocumentType.CSD as String,
            LeVADocumentUseCase.DocumentType.RA as String,
            LeVADocumentUseCase.DocumentType.SSDS as String,
            LeVADocumentUseCase.DocumentType.TIP as String,
            LeVADocumentUseCase.DocumentType.TIR as String,
            LeVADocumentUseCase.DocumentType.OVERALL_TIR as String,
            LeVADocumentUseCase.DocumentType.IVP as String,
            LeVADocumentUseCase.DocumentType.IVR as String,
            LeVADocumentUseCase.DocumentType.TCP as String,
            LeVADocumentUseCase.DocumentType.TCR as String,
            LeVADocumentUseCase.DocumentType.CFTP as String,
            LeVADocumentUseCase.DocumentType.CFTR as String,
            LeVADocumentUseCase.DocumentType.DIL as String,
            LeVADocumentUseCase.DocumentType.TRC as String,
        ],
        "5": [
            LeVADocumentUseCase.DocumentType.CSD as String,
            LeVADocumentUseCase.DocumentType.RA as String,
            LeVADocumentUseCase.DocumentType.SSDS as String,
            LeVADocumentUseCase.DocumentType.DTP as String,
            LeVADocumentUseCase.DocumentType.DTR as String,
            LeVADocumentUseCase.DocumentType.OVERALL_DTR as String,
            LeVADocumentUseCase.DocumentType.TIP as String,
            LeVADocumentUseCase.DocumentType.TIR as String,
            LeVADocumentUseCase.DocumentType.OVERALL_TIR as String,
            LeVADocumentUseCase.DocumentType.IVP as String,
            LeVADocumentUseCase.DocumentType.IVR as String,
            LeVADocumentUseCase.DocumentType.CFTP as String,
            LeVADocumentUseCase.DocumentType.CFTR as String,
            LeVADocumentUseCase.DocumentType.TCP as String,
            LeVADocumentUseCase.DocumentType.TCR as String,
            LeVADocumentUseCase.DocumentType.DIL as String,
            LeVADocumentUseCase.DocumentType.TRC as String,
        ]
    ]

    // Document types per GAMP category - for a saas only project
    private static Map GAMP_CATEGORIES_SAAS_ONLY = [
        "3": [
            LeVADocumentUseCase.DocumentType.CSD as String,
            LeVADocumentUseCase.DocumentType.RA as String,
            LeVADocumentUseCase.DocumentType.SSDS as String,
            LeVADocumentUseCase.DocumentType.CFTP as String,
            LeVADocumentUseCase.DocumentType.CFTR as String,
            LeVADocumentUseCase.DocumentType.TCP as String,
            LeVADocumentUseCase.DocumentType.TCR as String,
            LeVADocumentUseCase.DocumentType.DIL as String,
            LeVADocumentUseCase.DocumentType.TRC as String,
        ],
        "4": [
            LeVADocumentUseCase.DocumentType.CSD as String,
            LeVADocumentUseCase.DocumentType.RA as String,
            LeVADocumentUseCase.DocumentType.SSDS as String,
            LeVADocumentUseCase.DocumentType.TCP as String,
            LeVADocumentUseCase.DocumentType.TCR as String,
            LeVADocumentUseCase.DocumentType.CFTP as String,
            LeVADocumentUseCase.DocumentType.CFTR as String,
            LeVADocumentUseCase.DocumentType.DIL as String,
            LeVADocumentUseCase.DocumentType.TRC as String,
        ]
    ]

    // Document types per pipeline phase with an optional lifecycle constraint
    private static Map PIPELINE_PHASES = [
        (MROPipelineUtil.PipelinePhases.INIT): [
            (LeVADocumentUseCase.DocumentType.CSD as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END
        ],
        (MROPipelineUtil.PipelinePhases.BUILD): [
            (LeVADocumentUseCase.DocumentType.DTP as String): MROPipelineUtil.PipelinePhaseLifecycleStage.POST_START,
            (LeVADocumentUseCase.DocumentType.DTR as String): MROPipelineUtil.PipelinePhaseLifecycleStage.POST_EXECUTE_REPO,
            (LeVADocumentUseCase.DocumentType.OVERALL_DTR as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END
        ],
        (MROPipelineUtil.PipelinePhases.DEPLOY): [
            (LeVADocumentUseCase.DocumentType.TIP as String): MROPipelineUtil.PipelinePhaseLifecycleStage.POST_START,
            (LeVADocumentUseCase.DocumentType.TIR as String): MROPipelineUtil.PipelinePhaseLifecycleStage.POST_EXECUTE_REPO
        ],
        (MROPipelineUtil.PipelinePhases.TEST): [
            (LeVADocumentUseCase.DocumentType.SSDS as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END,
            (LeVADocumentUseCase.DocumentType.RA as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END,
            (LeVADocumentUseCase.DocumentType.IVP as String): MROPipelineUtil.PipelinePhaseLifecycleStage.POST_START,
            (LeVADocumentUseCase.DocumentType.IVR as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END,
            (LeVADocumentUseCase.DocumentType.CFTP as String): MROPipelineUtil.PipelinePhaseLifecycleStage.POST_START,
            (LeVADocumentUseCase.DocumentType.TRC as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END,
            (LeVADocumentUseCase.DocumentType.CFTR as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END,
            (LeVADocumentUseCase.DocumentType.DIL as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END,
            (LeVADocumentUseCase.DocumentType.TCP as String): MROPipelineUtil.PipelinePhaseLifecycleStage.POST_START,
            (LeVADocumentUseCase.DocumentType.TCR as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END
        ],
        (MROPipelineUtil.PipelinePhases.RELEASE): [
        ],
        (MROPipelineUtil.PipelinePhases.FINALIZE): [
            (LeVADocumentUseCase.DocumentType.OVERALL_TIR as String): MROPipelineUtil.PipelinePhaseLifecycleStage.PRE_END
        ]
    ]

    // Document types per repository type with an optional phase constraint
    private static Map REPSITORY_TYPES = [
        (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_CODE): [
            (LeVADocumentUseCase.DocumentType.DTR as String): null,
            (LeVADocumentUseCase.DocumentType.TIR as String): null
        ],
        (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_INFRA): [
            (LeVADocumentUseCase.DocumentType.TIR as String): null
        ],
        (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_SAAS_SERVICE): [:],
        (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_SERVICE): [
            (LeVADocumentUseCase.DocumentType.TIR as String): null
        ],
        (MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_TEST): [:]
    ]

    // Document types at the project level which require repositories
    private static List REQUIRING_REPOSITORIES = [
        LeVADocumentUseCase.DocumentType.OVERALL_DTR as String,
        LeVADocumentUseCase.DocumentType.OVERALL_TIR as String,
        LeVADocumentUseCase.DocumentType.CFTR as String,
        LeVADocumentUseCase.DocumentType.IVR as String,
        LeVADocumentUseCase.DocumentType.TCP as String,
        LeVADocumentUseCase.DocumentType.TCR as String
    ]

    // Document types per environment token and label to track with Jira
    @SuppressWarnings('NonFinalPublicField')
    public static Map ENVIRONMENT_TYPE = [
        "D": [
            (LeVADocumentUseCase.DocumentType.CSD as String)    : ["${LeVADocumentUseCase.DocumentType.CSD}"],
            (LeVADocumentUseCase.DocumentType.SSDS as String)   : ["${LeVADocumentUseCase.DocumentType.SSDS}"],
            (LeVADocumentUseCase.DocumentType.RA as String)     : ["${LeVADocumentUseCase.DocumentType.RA}"],
            (LeVADocumentUseCase.DocumentType.TIP as String)    : ["${LeVADocumentUseCase.DocumentType.TIP}_Q",
                                                                    "${LeVADocumentUseCase.DocumentType.TIP}_P"],
            (LeVADocumentUseCase.DocumentType.TIR as String)    : ["${LeVADocumentUseCase.DocumentType.TIR}"],
            (LeVADocumentUseCase.DocumentType.OVERALL_TIR as String)    : ["${LeVADocumentUseCase.DocumentType.TIR}"],
            (LeVADocumentUseCase.DocumentType.IVP as String)    : ["${LeVADocumentUseCase.DocumentType.IVP}_Q",
                                                                    "${LeVADocumentUseCase.DocumentType.IVP}_P"],
            (LeVADocumentUseCase.DocumentType.CFTP as String)   : ["${LeVADocumentUseCase.DocumentType.CFTP}"],
            (LeVADocumentUseCase.DocumentType.TCP as String)    : ["${LeVADocumentUseCase.DocumentType.TCP}"],
            (LeVADocumentUseCase.DocumentType.DTP as String)    : ["${LeVADocumentUseCase.DocumentType.DTP}"],
            (LeVADocumentUseCase.DocumentType.DTR as String)    : ["${LeVADocumentUseCase.DocumentType.DTR}"],
            (LeVADocumentUseCase.DocumentType.OVERALL_DTR as String)    : ["${LeVADocumentUseCase.DocumentType.DTR}"],
        ],
        "Q": [
            (LeVADocumentUseCase.DocumentType.TIR as String)    : ["${LeVADocumentUseCase.DocumentType.TIR}_Q"],
            (LeVADocumentUseCase.DocumentType.OVERALL_TIR as String)    : ["${LeVADocumentUseCase.DocumentType.TIR}_Q"],
            (LeVADocumentUseCase.DocumentType.IVR as String)    : ["${LeVADocumentUseCase.DocumentType.IVR}_Q"],
            (LeVADocumentUseCase.DocumentType.OVERALL_IVR as String)    : ["${LeVADocumentUseCase.DocumentType.IVR}_Q"],
            (LeVADocumentUseCase.DocumentType.CFTR as String)   : ["${LeVADocumentUseCase.DocumentType.CFTR}"],
            (LeVADocumentUseCase.DocumentType.TCR as String)    : ["${LeVADocumentUseCase.DocumentType.TCR}"],
            (LeVADocumentUseCase.DocumentType.TRC as String)    : ["${LeVADocumentUseCase.DocumentType.TRC}"],
            (LeVADocumentUseCase.DocumentType.DIL as String)    : ["${LeVADocumentUseCase.DocumentType.DIL}_Q"]
        ],
        "P": [
            (LeVADocumentUseCase.DocumentType.TIR as String)    : ["${LeVADocumentUseCase.DocumentType.TIR}_P"],
            (LeVADocumentUseCase.DocumentType.OVERALL_TIR as String)    : ["${LeVADocumentUseCase.DocumentType.TIR}_P"],
            (LeVADocumentUseCase.DocumentType.IVR as String)    : ["${LeVADocumentUseCase.DocumentType.IVR}_P"],
            (LeVADocumentUseCase.DocumentType.OVERALL_IVR as String)    : ["${LeVADocumentUseCase.DocumentType.IVR}_P"],
            (LeVADocumentUseCase.DocumentType.DIL as String)    : ["${LeVADocumentUseCase.DocumentType.DIL}_P"]
        ]
    ]

    private final ILogger logger

    LeVADocumentScheduler(Project project, IPipelineSteps steps, MROPipelineUtil util, LeVADocumentUseCase usecase,
        ILogger logger) {
        super(project, steps, util, usecase)
        this.logger = logger
    }

    /**
     * Returns the first environment where a document is generated.
     * This will also be the only one for which the document history is created or updated.
     * Subsequent environments will get copies of the document history for the previous one.
     *
     * @param documentType a document type.
     * @return the first environment for which the given document type is generated.
     */
    @NonCPS
    static String getFirstCreationEnvironment(String documentType) {
        def environment = Environment.values()*.toString().find { env ->
            ENVIRONMENT_TYPE[env].containsKey(documentType)
        }
        return environment
    }

    /**
     * Returns the last environment where a document is generated before the given one.
     * If the document is not generated in any previous environment, the given environment is returned.
     *
     * @param documentType a document type.
     * @param environment the environment for which to find the previous creation environment.
     * @return the last environment where a document is generated before the given one.
     */
    @NonCPS
    static String getPreviousCreationEnvironment(String documentType, String environment) {
        def previousEnvironment = null
        Environment.values()*.toString()
            .takeWhile { it != environment }
            .each { env ->
                if (ENVIRONMENT_TYPE[env].containsKey(documentType)) {
                    previousEnvironment = env
                }
            }
        return previousEnvironment ?: environment
    }

    @NonCPS
    private boolean isProjectOneSAASRepoOnly () {
        if (!(this.project.repositories.findAll{ repo ->
            repo.type == MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_SAAS_SERVICE}).isEmpty()) {
            return (this.project.repositories.findAll{ repo ->
                (repo.type != MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_SAAS_SERVICE &&
                repo.type != MROPipelineUtil.PipelineConfig.REPO_TYPE_ODS_TEST)
            }).isEmpty()
        } else {
            return false
        }
    }

    private boolean isDocumentApplicableForGampCategory(String documentType, String gampCategory) {
        return this.GAMP_CATEGORIES[gampCategory].contains(documentType)
    }

    private boolean isDocumentApplicableForSAASOnlyGampCategory(String documentType, String gampCategory) {
        return this.GAMP_CATEGORIES_SAAS_ONLY[gampCategory].contains(documentType)
    }

    private boolean isDocumentApplicableForPipelinePhaseAndLifecycleStage(String documentType, String phase, MROPipelineUtil.PipelinePhaseLifecycleStage stage) {
        def documentTypesForPipelinePhase = this.PIPELINE_PHASES[phase]
        if (!documentTypesForPipelinePhase) {
            return false
        }

        def result = documentTypesForPipelinePhase.containsKey(documentType)

        // Check if the document type defines a lifecycle stage constraint
        def lifecycleStageConstraintForDocumentType = documentTypesForPipelinePhase[documentType]
        if (lifecycleStageConstraintForDocumentType != null) {
            result = result && lifecycleStageConstraintForDocumentType == stage
        }

        return result
    }

    private boolean isDocumentApplicableForProject(String documentType, String gampCategory, String phase, MROPipelineUtil.PipelinePhaseLifecycleStage stage) {
        def result
        if (isProjectOneSAASRepoOnly()) {
            if (!this.GAMP_CATEGORIES_SAAS_ONLY.keySet().contains(gampCategory)) {
                throw new IllegalArgumentException("Error: unable to assert applicability of document type '${documentType}' for project '${this.project.key}' in phase '${phase}'. The GAMP category '${gampCategory}' is not supported for SAAS systems.")
            }
            result = isDocumentApplicableForSAASOnlyGampCategory(documentType, gampCategory) && isDocumentApplicableForPipelinePhaseAndLifecycleStage(documentType, phase, stage) && isProjectLevelDocument(documentType)
        } else {
            if (!this.GAMP_CATEGORIES.keySet().contains(gampCategory)) {
                throw new IllegalArgumentException("Error: unable to assert applicability of document type '${documentType}' for project '${this.project.key}' in phase '${phase}'. The GAMP category '${gampCategory}' is not supported for non-SAAS systems.")
            }
            result = isDocumentApplicableForGampCategory(documentType, gampCategory) && isDocumentApplicableForPipelinePhaseAndLifecycleStage(documentType, phase, stage) && isProjectLevelDocument(documentType)
        }
        if (isDocumentRequiringRepositories(documentType)) {
            result = result && !this.project.repositories.isEmpty()
        }

        // Applicable for certain document types only if the Jira service is configured in the release manager configuration
        if ([LeVADocumentUseCase.DocumentType.CSD, LeVADocumentUseCase.DocumentType.SSDS, LeVADocumentUseCase.DocumentType.CFTP, LeVADocumentUseCase.DocumentType.CFTR, LeVADocumentUseCase.DocumentType.IVP, LeVADocumentUseCase.DocumentType.IVR, LeVADocumentUseCase.DocumentType.DIL, LeVADocumentUseCase.DocumentType.TCP, LeVADocumentUseCase.DocumentType.TCR, LeVADocumentUseCase.DocumentType.RA, LeVADocumentUseCase.DocumentType.TRC].contains(documentType as LeVADocumentUseCase.DocumentType)) {
            result = result && this.project.services?.jira != null
        }

        return result
    }

    private boolean isDocumentApplicableForRepo(String documentType, String gampCategory, String phase, MROPipelineUtil.PipelinePhaseLifecycleStage stage, Map repo) {
        if (!this.GAMP_CATEGORIES.keySet().contains(gampCategory)) {
            throw new IllegalArgumentException("Error: unable to assert applicability of document type '${documentType}' for project '${this.project.key}' and repo '${repo.id}' in phase '${phase}'. The GAMP category '${gampCategory}' is not supported.")
        }

        return isDocumentApplicableForGampCategory(documentType, gampCategory) && isDocumentApplicableForPipelinePhaseAndLifecycleStage(documentType, phase, stage) && isDocumentApplicableForRepoTypeAndPhase(documentType, phase, repo)
    }

    private boolean isDocumentApplicableForRepoTypeAndPhase(String documentType, String phase, Map repo) {
        def documentTypesForRepoType = this.REPSITORY_TYPES[(repo.type.toLowerCase())]
        if (!documentTypesForRepoType) {
            return false
        }

        def result = documentTypesForRepoType.containsKey(documentType)

        // Check if the document type defines a phase constraint
        def phaseConstraintForDocumentType = documentTypesForRepoType[documentType]
        if (phaseConstraintForDocumentType != null) {
            result = result && phaseConstraintForDocumentType == phase
        }

        return result
    }

    private boolean isDocumentRequiringRepositories(String documentType) {
        return this.REQUIRING_REPOSITORIES.contains(documentType)
    }

    private boolean isProjectLevelDocument(String documentType) {
        return !this.isRepositoryLevelDocument(documentType)
    }

    @SuppressWarnings('UseCollectMany')
    private boolean isRepositoryLevelDocument(String documentType) {
        return this.REPSITORY_TYPES.values().collect { it.keySet() }.flatten().contains(documentType)
    }

    protected boolean isDocumentApplicable(String documentType, String phase, MROPipelineUtil.PipelinePhaseLifecycleStage stage, Map repo = null) {
        def capability = this.project.getCapability('LeVADocs')
        if (!capability) {
            return false
        }

        def gampCategory = capability.GAMPCategory as String
        return !repo
          ? isDocumentApplicableForProject(documentType, gampCategory, phase, stage)
          : isDocumentApplicableForRepo(documentType, gampCategory, phase, stage, repo)
    }

    protected boolean isDocumentApplicableForEnvironment(String documentType, String environment) {
        // in developer preview mode always create
        if (project.isDeveloperPreviewMode()) {
            return true
        }

        return this.ENVIRONMENT_TYPE[environment].containsKey(documentType)
    }

    void run(String phase, MROPipelineUtil.PipelinePhaseLifecycleStage stage, Map repo = null, Map data = null) {
        def documents = this.usecase.getSupportedDocuments()
        def environment = this.project.buildParams.targetEnvironmentToken

        documents.each { documentType ->
            if (this.isDocumentApplicableForEnvironment(documentType, environment)) {
                def args = [repo, data]
                if (this.isDocumentApplicable(documentType, phase, stage, repo)) {
                    def message = "Creating document of type '${documentType}' for project '${this.project.key}'"
                    def debugKey = "docgen-${this.project.key}-${documentType}"
                    if (repo) {
                        message += " and repo '${repo.id}'"
                        debugKey += "-${repo.id}"
                    }
                    message += " in phase '${phase}' and stage '${stage}'"
                    logger.infoClocked("${debugKey}", message)
                    this.util.executeBlockAndFailBuild {
                        try {
                            // Apply args according to the method's parameters length
                            def method = this.getMethodNameForDocumentType(documentType)
                            this.usecase.invokeMethod(method, args as Object[])
                        } catch (e) {
                            throw new IllegalStateException("Error: ${message} has failed: ${e.message}.", e)
                        }
                    }
                    logger.debugClocked("${debugKey}")
                }
            }
        }
    }
}
