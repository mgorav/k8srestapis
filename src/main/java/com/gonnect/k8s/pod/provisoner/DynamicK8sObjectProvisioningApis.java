package com.gonnect.k8s.pod.provisoner;

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

@RestController
public class DynamicK8sObjectProvisioningApis {
    private ApiClient client = null;

    public DynamicK8sObjectProvisioningApis() {
        System.out.println("DynamicKubernetesProvisionController created");
        try {
            client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
        } catch (IOException e) {
            System.err.println("Error while creating Kubernetes APIClient");
            e.printStackTrace();
        }
    }

    // Provision Pod
    @GetMapping({"/provisionpod"})
    public String createpod(@RequestParam(name = "namespace", required = true) final String namespace,
                            @RequestParam(name = "image", required = true) final String image,
                            @RequestParam(name = "podname", required = true) final String podname) {
        CoreV1Api api = new CoreV1Api();
        Map<String, String> label = new HashMap<>();
        label.put("name", podname);
        V1Pod pod = new V1PodBuilder().withNewMetadata().withName(podname).withLabels(label).endMetadata().withNewSpec().addNewContainer().withName(podname).withImage(image).endContainer().endSpec().build();
        try {
            V1Pod newPod = api.createNamespacedPod(namespace, pod, null, "true", null);
            return "Pod Created: " + newPod.toString();
        } catch (ApiException e) {
            e.printStackTrace();
            return e.getResponseBody();
        }
    }

    // Delete Pod
    @GetMapping({"/deletepod"})
    public String deletepod(@RequestParam(name = "namespace", required = true) final String namespace,
                            @RequestParam(name = "podname", required = true) final String podname) {
        CoreV1Api api = new CoreV1Api();
        try {
            V1Status status = api.deleteNamespacedPod(podname, namespace, "true", null, null, null, null, null);
            return "Delete Pod Operation Status" + status.getStatus() + " with message " + status.getMessage();
        } catch (ApiException e) {
            return "Error: " + e.getResponseBody();
        }
    }

    // Get list of pods
    @GetMapping({"/pods"})
    public String getPods() {
        StringBuffer buffer = new StringBuffer();
        CoreV1Api api = new CoreV1Api();
        try {
            V1PodList podlist = api.listNamespacedPod("default", String.valueOf(false), null, null, null, null, null, null, null, null);
            for (V1Pod pod : podlist.getItems()) {
                buffer.append(pod.getMetadata().getName());
                buffer.append("\n");
                System.out.println("Pod Name: " + pod.getMetadata().getName());
            }
            return buffer.toString();
        } catch (ApiException e) {
            System.err.println("Error while getting Pods in Default namespace");
            return "Error: " + e.getResponseBody();
        }
    }

    // Provision ReplicationController
    @GetMapping({"/provisionreplicationcontroller"})
    public String createreplicationcontroller(@RequestParam(name = "namespace", required = true) final String namespace,
                                              @RequestParam(name = "replicationcontrollername", required = true) final String replicationcontrollername,
                                              @RequestParam(name = "image", required = true) final String image,
                                              @RequestParam(name = "replicas", required = true) final int replicas) {
        System.out.println("Provision Replication Controller Data: " + namespace + " " + replicationcontrollername + " " + image);
        CoreV1Api api = new CoreV1Api();
        try {
            Map<String, String> selector = new HashMap<String, String>();
            selector.put("name", replicationcontrollername);
            V1PodTemplateSpec podTemplate = new V1PodTemplateSpecBuilder().withNewMetadata().withName(replicationcontrollername).withLabels(selector).endMetadata().withNewSpec().addNewContainer().withName(replicationcontrollername).withImage(image).endContainer().endSpec().build();
            V1ReplicationController replication = new V1ReplicationControllerBuilder().withNewMetadata().withName(replicationcontrollername).withNamespace(namespace).endMetadata().withNewSpec().withReplicas(Integer.valueOf(replicas)).withTemplate(podTemplate).withSelector(selector).endSpec().build();
            replication = api.createNamespacedReplicationController(namespace, replication, null, "true", null);
            return "Create ReplicationController Operation Output: " + replication.toString();
        } catch (ApiException e) {
            e.printStackTrace();
            return "Error: " + e.getResponseBody();
        }
    }

    // Scale Up/Down ReplicationController
    @GetMapping({"/scalereplicationcontroller"})
    @ResponseBody
    public String scalereplicationcontroller(@RequestParam(name = "namespace", required = true) final String namespace,
                                             @RequestParam(name = "replicationcount", required = true) final String replicationcount,
                                             @RequestParam(name = "rcname", required = true) final String rcname) {
        System.out.println("Scale Replication Controller Data: " + namespace + " " + replicationcount);
        CoreV1Api api = new CoreV1Api();
        try {
            V1Scale scale = new V1ScaleBuilder().withNewMetadata().withNamespace(namespace).withName(rcname).endMetadata().withNewSpec().withReplicas(Integer.getInteger(replicationcount)).endSpec().build();
            scale = api.replaceNamespacedReplicationControllerScale(rcname, namespace, scale, null, null, null);
            return "Scale ReplicationController Operation Output: " + scale.toString();
        } catch (ApiException e) {
            e.printStackTrace();
            return "Error: " + e.getResponseBody();
        }
    }

    // Delete ReplicationController
    @GetMapping({"/deletereplicationcontroller"})
    public String deletereplicationcontroller(@RequestParam(name = "namespace", required = true) final String namespace,
                                              @RequestParam(name = "replicationcontrollername", required = true) final String replicationcontrollername) {
        System.out.println("Delete Replication Controller Data: " + namespace + " " + replicationcontrollername);
// First, scale the number of Pods associated with this ReplicationController to zero
        this.scalereplicationcontroller(namespace, Integer.toString(0), replicationcontrollername);
// Now, delete the ReplicationController
        CoreV1Api api = new CoreV1Api();
        try {
            V1Status status = api.deleteNamespacedReplicationController(replicationcontrollername, namespace, null, null, null, null, null, null);
            return "Delete ReplicationController Operation Status" + status.getStatus() + " with message " + status.getMessage();
        } catch (ApiException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
            return "Error: " + e.getResponseBody();
        }
    }

    // Provision Service
    @GetMapping({"/provisionservice"})
    public String provisionservice(@RequestParam(name = "namespace", required = true) final String namespace,
                                   @RequestParam(name = "name", required = true) final String name,
                                   @RequestParam(name = "type", required = true) final String type,
                                   @RequestParam(name = "protocol", required = true) final String protocol,
                                   @RequestParam(name = "port", required = true) final String port,
                                   @RequestParam(name = "targetport", required = true) final String targetport,
                                   @RequestParam(name = "nodeport", required = false) final String nodeport,
                                   @RequestParam(name = "podselectorkey", required = true) final String podselectorkey,
                                   @RequestParam(name = "podselectorvalue", required = true) final String podselectorvalue) {
        System.out.println("Provision Service Data: " + namespace + " " + podselectorkey + " " + podselectorvalue);
        CoreV1Api api = new CoreV1Api();
        try {
            Map<String, String> selector = new HashMap<String, String>();
            selector.put(podselectorkey, podselectorvalue);
            V1ServicePort serviceport = new V1ServicePortBuilder().withPort(Integer.valueOf(port)).withTargetPort(new IntOrString(Integer.valueOf(targetport))).withProtocol(protocol).build();
            V1Service svc = new V1ServiceBuilder().withNewMetadata().withName(name).withLabels(selector).endMetadata().withNewSpec().withType(type).withSelector(selector).withPorts(serviceport).endSpec().build();
            svc = api.createNamespacedService(namespace, svc, null, "true", null);
            return "Create Service Operation Output: " + svc.toString();
        } catch (ApiException e) {
            e.printStackTrace();
            return "Error: " + e.getResponseBody();
        }
    }

    // Delete Service
    @GetMapping({"/deleteservice"})
    public String deleteservice(@RequestParam(name = "namespace", required = true) final String namespace,
                                @RequestParam(name = "name", required = true) final String name) {
        System.out.println("Delete Service Data: " + namespace + " " + name);
        CoreV1Api api = new CoreV1Api();
        try {
            V1Status status = api.deleteNamespacedService(name, namespace, "true", null, null, null, null, null);
            return "Delete Service Operation Status" + status.getStatus() + " with message " + status.getMessage();
        } catch (ApiException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
            return "Error: " + e.getResponseBody();
        }
    }

    // Provision ConfigMap
    @GetMapping({"/provisionconfigmap"})
    public String provisionconfigmap(@RequestParam Map<String, String> params) {
        Iterator<String> iter = params.keySet().iterator();
        String namespace = null;
        String name = null;
        Map<String, String> mapdata = new HashMap<String, String>();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (key.equals("namespace")) {
                namespace = params.get(key);
            } else {
                if (key.equals("name")) {
                    name = params.get(key);
                } else {
                    mapdata.put(key, params.get(key));
                }
            }
            System.out.println("Provision ConfigMap Data: " + key + " " + params.get(key));
        }
        CoreV1Api api = new CoreV1Api();
        try {
            Map<String, String> selector = new HashMap<String, String>();
            selector.put("name", name);
            V1ConfigMap configMap = new V1ConfigMapBuilder().withNewMetadata().withName(name).withNamespace(namespace).withLabels(selector).endMetadata().withData(mapdata).build();
            configMap = api.createNamespacedConfigMap(namespace, configMap, null, "true", null);
            return "Provision ConfigMap Operation Output" + configMap.toString();
        } catch (ApiException e) {
            e.printStackTrace();
            return "Error: " + e.getResponseBody();
        }
    }

    // Delete ConfigMap
    @GetMapping({"/deleteconfigmap"})
    public String deleteconfigmap(@RequestParam(name = "namespace", required = true) final String namespace,
                                  @RequestParam(name = "name", required = true) final String name) {
        System.out.println("Delete ConfigMap Data: " + namespace + " " + name);
        CoreV1Api api = new CoreV1Api();
        try {
            V1Status status = api.deleteNamespacedConfigMap(name, namespace, "true", null, null, null, null, null);
            return "Delete ConfigMap Operation Status" + status.getStatus() + " with message " + status.getMessage();
        } catch (ApiException e) {
            e.printStackTrace();
            return "Error: " + e.getResponseBody();
        }
    }

    // Provision Job
    @GetMapping({"/provisionjob"})
    public String createjob(@RequestParam(name = "namespace", required = true) final String namespace,
                            @RequestParam(name = "image", required = true) final String image,
                            @RequestParam(name = "jobname", required = true) final String jobname,
                            @RequestParam(name = "configmapname", required = false) final String configmapname,
                            @RequestParam(name = "commands", required = false) final List<String> commands,
                            @RequestParam(name = "arguments", required = false) final List<String> arguments,
                            @RequestParam Map<String, String> params) {
        System.out.println("Provision Job called");
        BatchV1Api api = new BatchV1Api();
        Map<String, String> label = new HashMap<String, String>();
        label.put("name", jobname);
//V1Job job = new V1JobBuilder().withKind("Job").withNewMetadata().withGenerateName(jobname).withLabels(label).endMetadata().withNewSpec().withNewTemplate().withNewSpec().addNewContainer().withName(jobname).withImage(image).endContainer().withRestartPolicy("Never").endSpec().endTemplate().withTtlSecondsAfterFinished(new Integer(0)).endSpec().build();
        V1JobBuilder jobBuilder = new V1JobBuilder().withKind("Job").withNewMetadata().withGenerateName(jobname).withLabels(label).endMetadata();
        if (configmapname != null && !configmapname.equals("")) {
            System.out.println("ConfigMap name provided -> " + configmapname);
            List<V1EnvFromSource> envFrom = new ArrayList<>();
            V1ConfigMapEnvSource configMapRef = new V1ConfigMapEnvSourceBuilder().withName(configmapname).build();
            V1EnvFromSource envSource = new V1EnvFromSourceBuilder().withConfigMapRef(configMapRef).build();
            envFrom.add(envSource);
            if (commands != null) {
                System.out.println("Commands Provided");
                Iterator<String> iter = commands.iterator();
                while (iter.hasNext()) {
                    System.out.println("Command -> " + iter.next());
                }
                if (arguments != null && !arguments.equals("")) {
                    System.out.println("Arguments Provided");
                    jobBuilder = jobBuilder.withNewSpec().withNewTemplate().withNewSpec().addNewContainer().withName(jobname).withImage(image).withCommand(commands).withArgs(arguments).withEnvFrom(envFrom).endContainer().withRestartPolicy("Never").endSpec().endTemplate().withTtlSecondsAfterFinished(new Integer(0)).endSpec();
                } else {
                    System.out.println("Arguments NOT Provided");
                    jobBuilder = jobBuilder.withNewSpec().withNewTemplate().withNewSpec().addNewContainer().withName(jobname).withImage(image).withCommand(commands).withEnvFrom(envFrom).endContainer().withRestartPolicy("Never").endSpec().endTemplate().withTtlSecondsAfterFinished(new Integer(0)).endSpec();
                }
            } else {
                System.out.println("Commands NOT Provided");
                jobBuilder = jobBuilder.withNewSpec().withNewTemplate().withNewSpec().addNewContainer().withName(jobname).withImage(image).withEnvFrom(envFrom).endContainer().withRestartPolicy("Never").endSpec().endTemplate().withTtlSecondsAfterFinished(new Integer(0)).endSpec();
            }
        } else {
            System.out.println("No configmapname provided, so, add all parameters in the Environment Variable !!");
            List<V1EnvVar> env = new ArrayList<V1EnvVar>();
            Iterator<String> iter = params.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                String value = params.get(key);
                System.out.println("Env Data: " + key + " with value: " + value);
                env.add(new V1EnvVarBuilder().withNewName(key).withNewValue(value).build());
            }
            if (commands != null) {
                System.out.println("Commands Provided");
                Iterator<String> iter1 = commands.iterator();
                while (iter1.hasNext()) {
                    System.out.println("Command -> " + iter1.next());
                }
                if (arguments != null && !arguments.equals("")) {
                    System.out.println("Arguments Provided");
                    jobBuilder = jobBuilder.withNewSpec().withNewTemplate().withNewSpec().addNewContainer().withName(jobname).withImage(image).withCommand(commands).withArgs(arguments).withEnv(env).endContainer().withRestartPolicy("Never").endSpec().endTemplate().withTtlSecondsAfterFinished(new Integer(0)).endSpec();
                } else {
                    System.out.println("Arguments NOT Provided");
                    jobBuilder = jobBuilder.withNewSpec().withNewTemplate().withNewSpec().addNewContainer().withName(jobname).withImage(image).withCommand(commands).withEnv(env).endContainer().withRestartPolicy("Never").endSpec().endTemplate().withTtlSecondsAfterFinished(new Integer(0)).endSpec();
                }
            } else {
                System.out.println("Commands NOT Provided");
                jobBuilder = jobBuilder.withNewSpec().withNewTemplate().withNewSpec().addNewContainer().withName(jobname).withImage(image).withEnv(env).endContainer().withRestartPolicy("Never").endSpec().endTemplate().withTtlSecondsAfterFinished(new Integer(0)).endSpec();
            }
        }
        try {
            V1Job job = api.createNamespacedJob(namespace, jobBuilder.build(), null, "true", null);
            return "Job Created: " + job.toString();
        } catch (ApiException e) {
            e.printStackTrace();
            return e.getResponseBody();
        }
    }

}

