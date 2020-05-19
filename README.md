## Basic Concept

There are two way to connect to k8s apis server:
1. In cluster
2. Out cluster

By "In cluster", I mean, running Spring Boot/Java K8s APIs running inside k8s cluster. The user of the APIs connect to these apis via standard REST approach and perform k8s object operations. These operations will be just like kubectl but with a twist. The twist is, we will be using REST APIs - no need to worry about k8s yaml synatx etc.
NOTE: We can still use "Out cluster" approach also. In this case we need to connect to k8s api server via standard kubeconfig.

Furher, k8s service account need to be setup properly with appropriate role and we assign that role to Spring boot/java REST APIs. Below is an example of creating service account and roll.

Simple!. Let's rock and roll. 


## Authorize java to create k8s object

```bash
kubectl apply -f serviceaccount.yaml
```
serviceaccount.yaml
````yaml
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: k8s-provisioning-apis-account
  namespace: default
---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRoleBinding
metadata:
  name: test-rbac
subjects:
  - kind: ServiceAccount
    # Reference to upper's `metadata.name`
    name: test-service-account
    # Reference to upper's `metadata.namespace`
    namespace: default
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: rbac.authorization.k8s.io
````