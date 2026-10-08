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

---

## ADVANCED KUBERNETES TOPICS (Added Recently)

### 1. API Versions (API Groups)
Kubernetes categorizes its features into API Groups to manage updates safely:
*   **`v1` (Core)**: The original, fundamental building blocks (`Pod`, `Service`, `Namespace`, `ConfigMap`, `Secret`).
*   **`apps/v1`**: Manages application lifecycles and scaling (`Deployment`, `StatefulSet`, `DaemonSet`).
*   **`networking.k8s.io/v1`**: External networking and routing (`Ingress`).
*   **`autoscaling/v2`**: The modern auto-scaler supporting CPU, Memory, and Custom Metrics (`HorizontalPodAutoscaler`).

### 2. Advanced Compute Resources
*   **StatefulSet**: Used for databases (MySQL, Redis). Unlike Deployments, Pods get sticky, permanent identities (e.g., `mysql-0`) and attach to persistent storage securely across reboots.
*   **DaemonSet**: Ensures exactly one copy of a Pod runs on *every single physical Node* in the cluster (used for logging or monitoring agents).

### 3. Advanced Storage & Scaling
*   **VolumeClaimTemplates**: Used in StatefulSets to automatically request persistent virtual hard drives (e.g., `1Gi`) for databases.
*   **HorizontalPodAutoscaler (HPA)**: Automatically scales the number of `replicas` up or down based on metrics (like CPU hitting 80%).
*   **Resource Limits**: Mandatory for HPA to work. You must define CPU and Memory `requests` (minimum needed) and `limits` (absolute maximum allowed) to protect the cluster.
    *   *Best Practice:* Inject ConfigMap and Secret into containers simultaneously using `envFrom: [configMapRef, secretRef]`.

### 4. Networking Deep Dive
*   **`port`**: The port the *Service* itself listens on internally.
*   **`targetPort`**: The actual port your Java/Node/Python application is running on *inside* the container.
*   **`nodePort`**: Punches a hole through the physical server's firewall to allow direct external access (e.g., `http://localhost:30002`).

### 5. Two `spec` Blocks Explained
In a Deployment or StatefulSet, you will often see `spec:` twice:
1.  **Outer `spec` (The Manager)**: Configures the Deployment itself (how many replicas, update strategy).
2.  **Inner `spec` (Under `template`)**: The blueprint for the Pod. Configures exactly how the container should run (Image, Ports, Environment Variables, Resource Limits).

---

## Real-World Microservice Example (API Gateway with HPA)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: ems-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: ashishujjwal/apigateway:latest
        envFrom:
        - configMapRef:
            name: ems-config
        resources:
          requests:
            cpu: "50m"
            memory: "256Mi"
          limits:
            cpu: "200m"
            memory: "384Mi"
        ports:
        - containerPort: 9090
---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: ems-prod
spec:
  selector:
    app: api-gateway
  ports:
    - port: 9090
      targetPort: 9090
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-gateway-ingress
  namespace: ems-prod
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-gateway
            port:
              number: 9090
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-autoscaler
  namespace: ems-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 1
  maxReplicas: 3
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 80
```


[ Internet ]
     ↓
1. Standalone NGINX        ◀── Edge / Security / DMZ (External to K8s)
     ↓
2. K8s Ingress Controller  ◀── Cluster Gatekeeper (Routes external traffic into the cluster)
     ↓
3. API Gateway             ◀── App Entry & Auth (e.g., Spring Cloud Gateway / Zuul)
     ↓ (Routes directly via K8s DNS)
4. K8s Services            ◀── Network Abstraction Layer (The internal phonebook & internal load balancer)
     ↓ (Load balances to)
5. Microservice Pods       ◀── Your Code (Dynamic instances scaling up/down)

---

## ARCHITECTURE Q&A (The Eureka vs Kubernetes Duel)

### Q1: Why do we have both Eureka and Kubernetes Services? Isn't it redundant?
**A:** Yes, it is a massive architectural redundancy! Kubernetes has its own built-in Service Discovery (ClusterIP). We keep Eureka in this stack strictly for **"Lift-and-Shift"** learning. In the real world, companies use this hybrid setup when transitioning legacy Spring Cloud apps to Kubernetes because rewriting the code to remove Eureka dependencies would take months.

### Q2: So how do Kubernetes Services and Eureka work together?
**A:** They actually don't work in sync. Eureka **overrides** Kubernetes for internal traffic!
1. When a Pod starts, it bypasses the K8s Service and registers its direct IP with Eureka.
2. The API Gateway asks Eureka for the IP, and routes traffic **directly to the Pod IP**.
3. The internal K8s `Services` sit idle for those microservices. K8s Services are only actively used for the Ingress (to find the Gateway), to find Eureka itself, and to find Databases.

### Q3: What happens if a Pod crashes? (The Eureka Danger Window)
**A:** Because Pods are highly destructive (ephemeral), if a Pod crashes, Kubernetes replaces it instantly. However, **Eureka relies on a 30-second heartbeat**. For up to 30-90 seconds, Eureka doesn't know the pod is dead and keeps telling the API Gateway to route traffic to the dead IP, resulting in `502 Bad Gateway` errors for users. 
*(If we used pure Kubernetes routing instead of Eureka, this delay would be 0 seconds).*

### Q4: How do we fix this Danger Window without deleting Eureka?
**A:** We inject aggressive heartbeat settings into the Spring Boot apps via our Kubernetes `ConfigMap`. We set the heartbeat to 5 seconds and eviction to 10 seconds. This shrinks the danger window from 90 seconds down to <10 seconds.
```yaml
  EUREKA_INSTANCE_LEASERENEWALINTERVALINSECONDS: "5"
  EUREKA_INSTANCE_LEASEEXPIRATIONDURATIONINSECONDS: "10"
  EUREKA_SERVER_EVICTIONINTERVALTIMERINMS: "5000"
```

### Q5: Eureka is just a phonebook. Who does the load balancing when there are 3 Replicas?
**A:** **Client-Side Load Balancing**.
1. The API Gateway asks Eureka for addresses.
2. Eureka returns a list of all 3 Pod IPs: `[10.1.2.3, 10.1.2.4, 10.1.2.5]`.
3. The **Spring Cloud LoadBalancer** (running inside the API Gateway's Java code) uses a Round-Robin algorithm to pick an IP and route the traffic.
*(In contrast, pure Kubernetes uses Server-Side Load Balancing via Kube-Proxy).*

### Q6: Why use a Standalone NGINX if we already have an Ingress Controller inside Kubernetes?
**A:** The Standalone NGINX sits outside the cluster in the DMZ, while the Ingress sits inside the cluster. We use both for:
1. **Security (The Bouncer):** The Standalone NGINX acts as a Web Application Firewall (WAF) to block hackers and DDoS attacks *before* they ever touch the Kubernetes cluster.
2. **Multi-Cluster Routing:** Large companies have multiple K8s clusters and legacy servers. The outer NGINX decides which specific data center or cluster the traffic should go to.
3. **Caching:** It serves heavy static files (images, CSS) instantly, saving K8s CPU power for actual API logic.
*Summary: Standalone NGINX asks "Are you safe, and which cluster do you need?" while Ingress asks "You made it inside, which specific microservice do you need?"*

### Q7: If we decide to remove Eureka, do we have to rewrite all our Java code?
**A:** No! Thanks to the **12-Factor App methodology** and Spring Boot's flexibility, we can disable Eureka and switch to pure Kubernetes routing purely through environment variables in our `ConfigMap`:
1. **Disable the Client:** Inject `EUREKA_CLIENT_ENABLED: "false"`. Spring Boot will instantly ignore Eureka.
2. **Override the Routes:** The API Gateway's `application.yml` uses `lb://EMPLOYEE-SERVICE`. We can override these routes at runtime using environment variables:
```yaml
  SPRING_CLOUD_GATEWAY_ROUTES_0_URI: "http://employee-service:8081"
  SPRING_CLOUD_GATEWAY_ROUTES_1_URI: "http://address-service:8082"
  SPRING_CLOUD_GATEWAY_ROUTES_2_URI: "http://auth-service:8083"
```
*Result:* The Gateway ignores its internal code, reads the Kubernetes ConfigMap, and routes traffic directly via lightning-fast Kubernetes DNS. No Java code changes required!

### Q8: How exactly do those `SPRING_CLOUD_GATEWAY_ROUTES_*` variables work internally?
**A:** Spring Boot has a powerful built-in feature called **Relaxed Binding**. When the API Gateway starts, it reads the Kubernetes ConfigMap and automatically translates the environment variables back into YAML paths. 
Because your Java `application.yml` has a list of routes, the environment variable `SPRING_CLOUD_GATEWAY_ROUTES_0_URI` perfectly maps to array index `0` (`spring.cloud.gateway.routes[0].uri`). 
Spring Boot secretly reaches into index 0 of your configuration and completely overwrites `lb://EMPLOYEE` with `http://employee-service:8081` in memory before the application even begins accepting traffic!