# Hyland Knowledge Discovery Agent Connector

**KD Agent Connector** is an Alfresco Platform (ACS) repository module that exposes a thin, secure REST façade over Hyland Knowledge Discovery *Agent Builder* API.
It lets any Alfresco‑authenticated user

* discover available **AI agents** (`GET /kd/agents`) and
* ask questions to a selected agent (`POST /kd/prompt`),

receiving structured answers plus the source‑document references the agent used.

The module is packaged as a single JAR and is compatible with Alfresco **ACS 23.1 to 25.x** (tested with 25.1 Community). No Share or ADF code is required.

## Why you might care

Skip building a full‑blown middleware: this add‑on already solves OAuth2 token caching, pagination and error handling. If your goal is *"Chat with KD agents from Alfresco"*, this is the *one* component you need.

## Prerequisites

| Component                 | Version     | Notes                                                          |
| ------------------------- | ----------- | -------------------------------------------------------------- |
| Java JDK                  | 21+         | Compiled for 17 – runs fine on modern LTS JDKs.                |
| Maven                     | ≥ 3.8       | Wrapper not included – install Maven locally.                  |
| Alfresco Content Services | 23.1–25.x   | Community or Enterprise.                                       |
| Network access            | HTTPS 443   | Outbound to **api.ai.dev.experience.hyland.com** and your IdP. |

## Build the JAR

```bash
git clone https://github.com/your-org/kd-agent-connector.git
cd kd-agent-connector
mvn clean package -DskipTests
```

The addon is created at
`target/kd-agent-connector-<version>.jar`.

## Configuration

Copy the following properties into *alfresco‑global.properties* (or set them as environment variables):

```properties
# --- KD OAuth 2.0 ---
app.knowledge-discovery.client-id = <your‑client‑id>
app.knowledge-discovery.client-secret = <your‑client‑secret>
app.knowledge-discovery.oauth-url = https://auth.iam.dev.experience.hyland.com/idp

# --- KD API ---
app.knowledge-discovery.api-url = https://api.ai.dev.experience.hyland.com
app.knowledge-discovery.hx-env-id = <environment‑uuid>
```

> **Keep secrets out of VCS** – externalise them via Docker secrets, Kubernetes config‑maps or your preferred secret store.

## REST End‑points

| Method | URL                                       | Auth          | Description                                                                        |
| ------ | ----------------------------------------- | ------------- | ---------------------------------------------------------------------------------- |
| `GET`  | `/alfresco/s/kd/agents?offset=0&limit=20` | Alfresco user | Returns a paginated list of available RAG agents plus counters by type and status. |
| `POST` | `/alfresco/s/kd/prompt`                   | Alfresco user | Invokes the latest version of the given agent.                                     |

## Deployment

Pick the scenario that matches your environment.

### 1. Docker‑based ACS (official images)

1. **Build** the JAR (see above) or grab it from your CI artefact repo.

2. Create an *extensions* folder next to your `docker-compose.yml` and copy the JAR there.

3. Add a volume to the `alfresco` service:

   ```yaml
   services:
     alfresco:
       image: alfresco/alfresco-content-repository-community:25.1
       environment:
         JAVA_OPTS: >-
           -Dapp.knowledge-discovery.client-id=<your-client-id>
           -Dapp.knowledge-discovery.client-secret=<your-client-secret>
           -Dapp.knowledge-discovery.oauth-url=https://auth.iam.dev.experience.hyland.com/idp
           -Dapp.knowledge-discovery.api-url=https://api.ai.dev.experience.hyland.com
           -Dapp.knowledge-discovery.hx-env-id=<environment‑uuid>
       volumes:
         - ./extensions/kd-agent-connector-1.0.jar:/usr/local/tomcat/modules/platform/kd-agent-connector-1.0.jar
   ```

4. Put your `alfresco-global.properties` overrides into `./config`.

5. `docker compose up` – the module is detected at start‑up (`Alfresco modules -> kd-agent-connector`).

### 2. Bare‑metal / classic Tomcat

1. Stop Alfresco.
2. Create `$ALF_HOME/modules/platform` if it does not exist.
3. Copy `kd-agent-connector-<version>.jar` into that directory.
4. Merge the property block above into `$ALF_HOME/tomcat/shared/classes/alfresco-global.properties`.
5. Start Alfresco and check `alfresco.log` for
   `INFO  [org.alfresco.repo.module.ModuleServiceImpl] [main] Installing module 'kd-agent-connector'`.
