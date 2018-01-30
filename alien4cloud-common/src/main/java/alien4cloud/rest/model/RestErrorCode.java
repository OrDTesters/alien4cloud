package alien4cloud.rest.model;

/**
 * Error codes for rest services.
 */
public enum RestErrorCode {
    // Maintenance
    MAINTENANCE(0),

    // Authentication
    AUTHENTICATION_REQUIRED_ERROR(100),
    AUTHENTICATION_FAILED_ERROR(101),
    UNAUTHORIZED_ERROR(102),

    // CSAR processing errors
    CSAR_PARSING_ERROR(200),
    CSAR_INVALID_ERROR(201),
    CSAR_RELEASE_IMMUTABLE(202),

    // Indexing global error.
    INDEXING_SERVICE_ERROR(300),

    // Plugin errors
    PLUGIN_USED_ERROR(350),
    MISSING_PLUGIN_ERROR(351),
    INVALID_PLUGIN_CONFIGURATION(352),
    MISSING_PLUGIN_DESCRIPTOR_FILE_EXCEPTION(353),

    // Cloud errors
    CLOUD_DISABLED_ERROR(370),
    NODE_OPERATION_EXECUTION_ERROR(371),
    DEPLOYMENT_NAMING_POLICY_ERROR(372),
    MAINTENANCE_MODE_ERROR(373),
    EMPTY_META_PROPERTY_ERROR(374),
    SCALING_ERROR(375),
    ORCHESTRATOR_LOCATION_SUPPORT_VIOLATION(376),

    // User errors
    DELETE_LAST_ADMIN_USER_ERROR(390),
    DELETE_LAST_ADMIN_ROLE_ERROR(391),

    // Repository service error
    REPOSITORY_SERVICE_ERROR(400),

    // Global errors
    UNCATEGORIZED_ERROR(500),
    ILLEGAL_PARAMETER(501),
    ALREADY_EXIST_ERROR(502),
    IMAGE_UPLOAD_ERROR(503),
    NOT_FOUND_ERROR(504),
    ILLEGAL_STATE_OPERATION(505),
    INTERNAL_OBJECT_ERROR(506),
    DELETE_REFERENCED_OBJECT_ERROR(507),
    RESOURCE_USED_ERROR(508),
    UNSUPPORTED_OPERATION_ERROR(509),

    // Application handling errors : code 600+
    APPLICATION_UNDEPLOYMENT_ERROR(602),
    APPLICATION_DEPLOYMENT_ERROR(601),
    INVALID_DEPLOYMENT_SETUP(603),
    APPLICATION_ENVIRONMENT_ERROR(604),
    APPLICATION_VERSION_ERROR(605),
    INVALID_APPLICATION_ENVIRONMENT_ERROR(606),
    APPLICATION_ENVIRONMENT_DEPLOYED_ERROR(607),
    UPDATE_RELEASED_APPLICATION_VERSION_ERROR(608),
    APPLICATION_DEPLOYMENT_IO_ERROR(609),
    LAST_APPLICATION_VERSION_ERROR(610),
    MISSING_APPLICATION_VERSION_ERROR(611),
    CANNOT_UPDATE_DEPLOYED_ENVIRONMENT(612),
    DEPLOYMENT_PAAS_ID_CONFLICT(613),
    INVALID_DEPLOYMENT_TOPOLOGY(614),
    APPLICATION_CSAR_VERSION_ALREADY_EXIST(615),
    INVALID_NAME(618),
    RUNTIME_WORKFLOW_ERROR(619),

    // PaaS errors : code 650+
    COMPUTE_CONFLICT_NAME(650),
    CONFLICT_BETWEEN_DELETABLE_OPTION_AND_VOLUME_ID(651),

    // Git csar import errors: code 680+
    GIT_IMPORT_FAILED(680),
    GIT_CONFLICT_ERROR(681),
    GIT_STATE_ERROR(682),

    // Component handling errors : code 700+
    COMPONENT_MISSING_ERROR(700),

    // Editor errors
    EDITOR_CONCURRENCY_ERROR(750),
    EDITOR_IO_ERROR(751),

    // Topology management errors.
    // Node template properties handling errors
    PROPERTY_CONSTRAINT_VIOLATION_ERROR(800),
    PROPERTY_CONSTRAINT_MATCH_ERROR(801),
    PROPERTY_MISSING_ERROR(802),
    VERSION_CONFLICT_ERROR(803),
    PROPERTY_TYPE_VIOLATION_ERROR(804),
    PROPERTY_REQUIRED_VIOLATION_ERROR(805),
    PROPERTY_UNKNOWN_VIOLATION_ERROR(806),
    UPDATE_AN_RELEASED_TOPOLOGY_ERROR(807),
    ELEMENT_NAME_PATTERN_CONSTRAINT(808),
    // bounds on the requirements or capabilities
    UPPER_BOUND_REACHED(810),
    LOWER_BOUND_NOT_SATISFIED(811),
    PROPERTY_DEFINITION_MATCH_ERROR(812),
    //
    CYCLIC_TOPOLOGY_TEMPLATE_REFERENCE_ERROR(820),
    RELEASE_REFERENCING_SNAPSHOT(830),
    VERSION_USED(831),
    BAD_WORKFLOW_OPERATION(850),
    RECOVER_TOPOLOGY(860),
    NOT_VISIBLE_TOPOLOGY(870);

    private final int code;

    private RestErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}