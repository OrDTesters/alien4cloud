package alien4cloud.deployment;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.secret.SecretProviderConfiguration;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.MaintenanceModeException;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.OperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.tosca.context.ToscaContextualAspect;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.normative.constants.AlienCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.AlienInterfaceTypes;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.utils.NodeTemplateUtils;
import org.alien4cloud.tosca.utils.TopologyUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages operations performed on a running deployment.
 */
@Slf4j
@Service
public class DeploymentRuntimeService {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentContextService deploymentContextService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;
    @Inject
    private ToscaContextualAspect toscaContextualAspect;

    /**
     * Trigger the execution of an operation on a node.
     *
     * @param request the operation's execution description ( see {@link alien4cloud.paas.model.OperationExecRequest})
     * @throws alien4cloud.paas.exception.OperationExecutionException runtime exception during an operation
     */
    public void triggerOperationExecution(OperationExecRequest request, IPaaSCallback<Map<String, String>> callback) throws OperationExecutionException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(request.getApplicationEnvironmentId());
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials = generateSecretConfiguration(deploymentTopology, request.getSecretProviderPluginName(), request.getSecretProviderCredentials());
        orchestratorPlugin.executeOperation(deploymentContextService.buildTopologyDeploymentContext(secretProviderConfigurationAndCredentials, deployment,
                deploymentTopologyService.getLocations(deploymentTopology), deploymentTopology), request, callback);
    }

    private SecretProviderConfigurationAndCredentials generateSecretConfiguration(DeploymentTopology deploymentTopology, String secretProviderPluginName, Object secretProviderCredentials) {
        SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials = new SecretProviderConfigurationAndCredentials();
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(deploymentTopology);
        Map<String, Location> locations = deploymentTopologyService.getLocations(locationIds);
        Optional<Location> firstLocation = locations.values().stream().filter(location -> Objects.equals(secretProviderPluginName, location.getSecretProviderConfiguration().getPluginName())).findFirst();
        if(!firstLocation.isPresent()){
            log.error("Plugin name <" + secretProviderPluginName + "> is not configured by the current location.");
            secretProviderConfigurationAndCredentials = null;
        }else{
            SecretProviderConfiguration configuration = new SecretProviderConfiguration();
            configuration.setConfiguration(firstLocation.get().getSecretProviderConfiguration());
            configuration.setPluginName(secretProviderPluginName);
            secretProviderConfigurationAndCredentials.setSecretProviderConfiguration(configuration);
            secretProviderConfigurationAndCredentials.setCredentials(secretProviderCredentials);
        }
        return secretProviderConfigurationAndCredentials;
    }

    /**
     * Switch an instance in a deployment to maintenance mode. If so the orchestrator should not perform self healing operations for this instance.
     *
     * @param applicationEnvironmentId The id of the application environment.
     * @param nodeTemplateId The id of the node template on which to enable maintenance mode.
     * @param instanceId The id of the instance.
     * @param maintenanceModeOn true if we should enable the maintenance mode, false if we should disable it.
     * @throws MaintenanceModeException In case the operation fails.
     */
    public void switchInstanceMaintenanceMode(String applicationEnvironmentId, String nodeTemplateId, String instanceId, boolean maintenanceModeOn)
            throws MaintenanceModeException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology, null);
        orchestratorPlugin.switchInstanceMaintenanceMode(deploymentContext, nodeTemplateId, instanceId, maintenanceModeOn);
    }

    /**
     * Switch all instances in a deployment to maintenance mode. If so the orchestrator should not perform self healing operations for this instance.
     *
     * @param applicationEnvironmentId The id of the application environment.
     * @param maintenanceModeOn true if we should enable the maintenance mode, false if we should disable it.
     * @throws MaintenanceModeException In case the operation fails.
     */
    public void switchMaintenanceMode(String applicationEnvironmentId, boolean maintenanceModeOn) throws MaintenanceModeException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology, null);
        orchestratorPlugin.switchMaintenanceMode(deploymentContext, maintenanceModeOn);
    }

    /**
     * Scale up/down a node in a topology
     *
     * @param applicationEnvironmentId id of the targeted environment
     * @param nodeTemplateId id of the compute node to scale up
     * @param instances the number of instances to be added (if positive) or removed (if negative)
     */
    public void scale(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, String applicationEnvironmentId, final String nodeTemplateId, int instances, final IPaaSCallback<Object> callback)
            throws OrchestratorDisabledException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        final DeploymentTopology topology = alienMonitorDao.findById(DeploymentTopology.class, deployment.getId());
        toscaContextualAspect.execInToscaContext(() -> {
            doScale(nodeTemplateId, instances, callback, deployment, topology);
            return null;
        }, false, topology);
    }

    private void doScale(final String nodeTemplateId, final int instances, final IPaaSCallback<Object> callback, final Deployment deployment,
            final DeploymentTopology topology) {
        NodeTemplate nodeTemplate = TopologyUtils.getNodeTemplate(topology, nodeTemplateId);

        // Get alien4cloud specific interface to support cluster controller nodes.
        Capability clusterControllerCapability = NodeTemplateUtils.getCapabilityByType(nodeTemplate, AlienCapabilityTypes.CLUSTER_CONTROLLER);
        if (clusterControllerCapability == null) {
            doScaleNode(nodeTemplateId, instances, callback, deployment, topology, nodeTemplate);
        } else {
            triggerClusterManagerScaleOperation(nodeTemplateId, instances, callback, deployment, topology, clusterControllerCapability);
        }
    }

    private void triggerClusterManagerScaleOperation(final String nodeTemplateId, final int instances, final IPaaSCallback<Object> callback,
            final Deployment deployment, final DeploymentTopology topology, Capability clusterControllerCapability) {
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        NodeOperationExecRequest scaleOperationRequest = new NodeOperationExecRequest();
        // Instance id is not specified for cluster control nodes
        scaleOperationRequest.setNodeTemplateName(nodeTemplateId);
        scaleOperationRequest.setInterfaceName(AlienInterfaceTypes.CLUSTER_CONTROL);
        scaleOperationRequest.setOperationName(AlienInterfaceTypes.CLUSTER_CONTROL_OP_SCALE);

        int currentInstances = TopologyUtils.getScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, clusterControllerCapability);
        int expectedInstances = currentInstances + instances;
        log.info("Scaling [ {} ] node from [ {} ] to [ {} ]. Updating runtime topology...", nodeTemplateId, currentInstances, expectedInstances);
        TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, expectedInstances, clusterControllerCapability);
        alienMonitorDao.save(topology);

        scaleOperationRequest.setParameters(Maps.newHashMap());
        scaleOperationRequest.getParameters().put(AlienInterfaceTypes.CLUSTER_CONTROL_OP_SCALE_PARAMS_INSTANCES_DELTA, String.valueOf(instances));
        scaleOperationRequest.getParameters().put(AlienInterfaceTypes.CLUSTER_CONTROL_OP_SCALE_PARAMS_EXPECTED_INSTANCES, String.valueOf(expectedInstances));

        orchestratorPlugin.executeOperation(
                deploymentContextService.buildTopologyDeploymentContext(null, deployment, deploymentTopologyService.getLocations(topology), topology),
                scaleOperationRequest, new IPaaSCallback<Map<String, String>>() {
                    @Override
                    public void onSuccess(Map<String, String> data) {
                        callback.onSuccess(data);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        log.info("Failed to scale [ {} ] node from [ {} ] to [ {} ]. rolling back to {}...", nodeTemplateId, currentInstances,
                                expectedInstances, currentInstances);
                        TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, currentInstances, clusterControllerCapability);
                        alienMonitorDao.save(topology);
                        callback.onFailure(throwable);
                    }
                });
    }

    private void doScaleNode(final String nodeTemplateId, final int instances, final IPaaSCallback<Object> callback, final Deployment deployment,
            final DeploymentTopology topology, NodeTemplate nodeTemplate) {
        final Capability capability = NodeTemplateUtils.getCapabilityByTypeOrFail(nodeTemplate, NormativeCapabilityTypes.SCALABLE);
        final int previousInitialInstances = TopologyUtils.getScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, capability);
        final int newInitialInstances = previousInitialInstances + instances;
        log.info("Scaling [ {} ] node from [ {} ] to [ {} ]. Updating runtime topology...", nodeTemplateId, previousInitialInstances, newInitialInstances);
        TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, newInitialInstances, capability);
        alienMonitorDao.save(topology);

        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, topology, null);
        orchestratorPlugin.scale(deploymentContext, nodeTemplateId, instances, new IPaaSCallback() {
            @Override
            public void onFailure(Throwable throwable) {
                log.info("Failed to scale [ {} ] node from [ {} ] to [ {} ]. rolling back to {}...", nodeTemplateId, previousInitialInstances,
                        newInitialInstances, previousInitialInstances);
                TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, previousInitialInstances, capability);
                alienMonitorDao.save(topology);
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(data);
            }
        });
    }
}