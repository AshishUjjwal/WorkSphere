# Kubernetes (K8s) Detailed Notes

## What is Kubernetes?
Kubernetes (often abbreviated as K8s) is an open-source container orchestration platform that automates the deployment, scaling, and management of containerized applications.

## Architecture Overview
A Kubernetes cluster consists of a set of worker machines, called **Nodes**, that run containerized applications. Every cluster has at least one worker node. The worker nodes host the **Pods** that are the components of the application workload. The **Control Plane (Master Node)** manages the worker nodes and the Pods in the cluster.

### Control Plane Components
*   **kube-apiserver**: The front end of the Kubernetes control plane. It exposes the Kubernetes API.
*   **etcd**: Consistent and highly-available key value store used as Kubernetes' backing store for all cluster data.
*   **kube-scheduler**: Watches for newly created Pods with no assigned node, and selects a node for them to run on.
*   **kube-controller-manager**: Runs controller processes (e.g., Node controller, Job controller, Endpoints controller).
*   **cloud-controller-manager**: Embeds cloud-specific control logic.

### Node Components
*   **kubelet**: An agent that runs on each node in the cluster. It makes sure that containers are running in a Pod.
*   **kube-proxy**: A network proxy that runs on each node in your cluster, maintaining network rules on nodes.
*   **Container Runtime**: The software that is responsible for running containers (e.g., containerd, CRI-O, Docker Engine).

---

## Core Concepts

*   **Pod**: The smallest and simplest Kubernetes object. A Pod represents a set of running containers on your cluster. Usually, one container per Pod, but can be multiple if they are tightly coupled.
*   **ReplicaSet**: Ensures that a specified number of pod replicas are running at any given time.
*   **Deployment**: Provides declarative updates for Pods and ReplicaSets. You describe a desired state in a Deployment, and the Deployment Controller changes the actual state to the desired state at a controlled rate. (This is the most common way to run apps).
*   **Service**: An abstract way to expose an application running on a set of Pods as a network service. Types include `ClusterIP` (internal), `NodePort` (exposed on a static port on each node), and `LoadBalancer` (uses a cloud provider's load balancer).
*   **Namespace**: Provides a mechanism for isolating groups of resources within a single cluster. (e.g., `default`, `kube-system`).
*   **ConfigMap**: An API object used to store non-confidential data in key-value pairs. Pods can consume ConfigMaps as environment variables, command-line arguments, or as configuration files in a volume.
*   **Secret**: Similar to ConfigMaps but is specifically intended to hold a small amount of sensitive data such as passwords, OAuth tokens, and SSH keys.
*   **Volume**: A directory containing data, accessible to the containers in a pod. Outlives any containers that run within the Pod.
*   **Ingress**: An API object that manages external access to the services in a cluster, typically HTTP. Ingress may provide load balancing, SSL termination, and name-based virtual hosting.

---

## Essential `kubectl` Commands

`kubectl` is the command line tool for Kubernetes.

### 1. Basic Cluster Information
*   **Get cluster info:**
    ```bash
    kubectl cluster-info
    ```
*   **List nodes:**
    ```bash
    kubectl get nodes
    ```
*   **Detailed node info:**
    ```bash
    kubectl describe node <node-name>
    ```

### 2. Working with Resources (Get & Describe)
*   **List all pods in the current namespace:**
    ```bash
    kubectl get pods
    ```
*   **List pods in all namespaces:**
    ```bash
    kubectl get pods -A  # or --all-namespaces
    ```
*   **List pods with more details (IP, Node):**
    ```bash
    kubectl get pods -o wide
    ```
*   **Describe a specific pod (useful for troubleshooting):**
    ```bash
    kubectl describe pod <pod-name>
    ```
*   **List deployments, services, replica sets:**
    ```bash
    kubectl get deployments
    kubectl get services (or svc)
    kubectl get rs
    ```
*   **Get all resources in a namespace:**
    ```bash
    kubectl get all -n <namespace-name>
    ```

### 3. Creating and Managing Resources
*   **Apply a configuration from a YAML or JSON file:**
    ```bash
    kubectl apply -f ./my-manifest.yaml
    ```
*   **Apply a directory of manifest files:**
    ```bash
    kubectl apply -f ./my-dir/
    ```
*   **Create a resource imperatively (less common for prod):**
    ```bash
    kubectl create deployment nginx --image=nginx
    ```
*   **Delete a resource by file name:**
    ```bash
    kubectl delete -f ./my-manifest.yaml
    ```
*   **Delete a specific pod/deployment:**
    ```bash
    kubectl delete pod <pod-name>
    kubectl delete deployment <deployment-name>
    ```

### 4. Interacting with Pods (Debugging)
*   **View logs for a container in a pod:**
    ```bash
    kubectl logs <pod-name>
    ```
*   **Stream logs for a container (like `tail -f`):**
    ```bash
    kubectl logs -f <pod-name>
    ```
*   **View logs for a specific container in a multi-container pod:**
    ```bash
    kubectl logs <pod-name> -c <container-name>
    ```
*   **Execute a command inside a running pod:**
    ```bash
    kubectl exec -it <pod-name> -- /bin/sh  # or /bin/bash
    ```
*   **Forward a local port to a port on the pod (for testing access):**
    ```bash
    kubectl port-forward pod/<pod-name> <local-port>:<pod-port>
    # Example: kubectl port-forward pod/my-nginx-pod 8080:80
    ```

### 5. Scaling and Rollouts
*   **Scale a deployment to a specific number of replicas:**
    ```bash
    kubectl scale deployment <deployment-name> --replicas=3
    ```
*   **Check the status of a rollout:**
    ```bash
    kubectl rollout status deployment/<deployment-name>
    ```
*   **View rollout history:**
    ```bash
    kubectl rollout history deployment/<deployment-name>
    ```
*   **Undo a deployment (rollback to previous version):**
    ```bash
    kubectl rollout undo deployment/<deployment-name>
    ```
*   **Restart a deployment (forces new pods to be created):**
    ```bash
    kubectl rollout restart deployment/<deployment-name>
    ```

### 6. Configuration and Secrets
*   **Create a ConfigMap from a file or literal value:**
    ```bash
    kubectl create configmap my-config --from-file=path/to/bar
    kubectl create configmap my-config --from-literal=key1=config1
    ```
*   **Create a Secret:**
    ```bash
    kubectl create secret generic my-secret --from-literal=password=mypassword
    ```

### 7. Node and Cluster Troubleshooting
*   **View cluster events:**
    ```bash
    kubectl get events --sort-by='.metadata.creationTimestamp'
    ```
*   **Show resource usage (CPU/Memory) for nodes (requires metrics-server):**
    ```bash
    kubectl top nodes
    ```
*   **Show resource usage (CPU/Memory) for pods (requires metrics-server):**
    ```bash
    kubectl top pods
    ```

---

## Example YAML Manifest (Deployment)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.14.2
        ports:
        - containerPort: 80
```
