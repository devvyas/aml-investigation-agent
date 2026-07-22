# AML Investigation Agent

A multi-agent AML (anti-money-laundering) investigation system. One orchestrator delegates to
three independent sub-agents — KYC, Sanctions, and Network — each running its own LangChain4j
AiService against Claude, to investigate a flagged account and reach one of four verdicts:
`FILE_SAR`, `CLEAR`, `ESCALATE_TO_HUMAN`, or `ENHANCED_DUE_DILIGENCE`.

Built with Java 21, Spring Boot 3, and LangChain4j.

## How the AiServices actually work

This is the part of the codebase most likely to look like it's missing something on first read,
so it's worth explaining directly: interfaces like `KycAgent`, `SanctionsAgent`, `NetworkAgent`,
and `OrchestratorAgent` have **no hand-written implementing class anywhere in this repository.**

Each one is a plain Java interface with a single method, annotated with `@SystemMessage` and
`@UserMessage`. At startup, a `@Configuration` class (e.g. `KycAgentConfig`) calls:

```
AiServices.builder(KycAgent.class)
    .chatLanguageModel(chatModel)
    .tools(kycTools)
    .build();
```

This builds a **JDK dynamic proxy** implementing the interface — the same mechanism behind
`Mockito.mock(SomeInterface.class)`, Spring's `@Transactional` AOP proxies, and Hibernate's
lazy-loading entity proxies. No bytecode is generated for a real class; a runtime
`InvocationHandler` intercepts every call to the interface's methods instead.

When calling code invokes `kycAgent.investigate(accountId)`, here's what actually happens,
entirely inside LangChain4j's library code:

1. The handler reads the method's `@SystemMessage`/`@UserMessage`/`@V` annotations via
   reflection and builds the actual prompt, substituting real parameter values into the
   template.
2. It sends that prompt to the configured `ChatLanguageModel` — the real call to Claude.
3. If Claude's response requests a tool call (e.g. `resolveCorporateStructure`), the handler
   reflectively invokes that method on the registered tools object (`KycTools`) — this is where
   tool methods actually get called; it never happens explicitly in this codebase's own source.
   The result is fed back to Claude as a new message, and the loop repeats until Claude stops
   requesting tools (the ReAct loop).
4. Because the interface method's return type is a domain record (`AgentFindings`), not
   `String`, LangChain4j performs structured-output extraction: it forces a final response
   shaped like the record's JSON schema, and deserializes it into a real instance via Jackson.
5. That instance is what the proxy's `invoke()` returns — indistinguishable, from the caller's
   perspective, from an ordinary method call.

`OrchestratorAgent` is the same mechanism with one difference worth noting: it has no `.tools(...)`
call at all in its config. It's a single reasoning pass over evidence already gathered by the
three sub-agents, not a ReAct loop of its own — see "Synthesis" below.

## Architecture

### Module graph

```
agent-tools-common          (leaf — no dependencies on other modules in this project)
  ├── SubAgentResult        (shared result record crossing the port boundary)
  ├── KycInvestigator       (port)
  ├── SanctionsInvestigator (port)
  └── NetworkInvestigator   (port)

agent-kyc, agent-sanctions, agent-network
  (each depends only on agent-tools-common)
  ├── a private, per-module AiService interface (never exported)
  ├── a private, per-module status enum + AgentFindings record (never exported)
  ├── an integration client (REST for KYC/Sanctions, raw Neo4j driver for Network)
  ├── a @Tool-annotated class delegating to that client
  ├── a Local*Investigator adapter implementing the module's port
  └── @Configuration classes wiring the AiService and (for Network) the Neo4j driver

investigation-store          (leaf — no dependency on agent-tools-common or agent-orchestrator)
  ├── InvestigationEntity / SubAgentFindingEntity (JPA entities, separate from SarVerdict/SubAgentResult)
  ├── InvestigationRepository (Spring Data JPA)
  └── InvestigationStore (the only public entry point — never expose the repository or entities directly)

agent-orchestrator
  ├── compile-scope dependency on agent-tools-common and investigation-store only
  ├── runtime-scope-only dependency on agent-kyc, agent-sanctions, agent-network
  ├── Verdict / SarVerdict (private enum + record the synthesis AiService authors)
  ├── OrchestratorAgent (private AiService, no tools registered)
  ├── InvestigationOrchestrator (the actual fan-out logic — plain Java, not an AiService)
  ├── InvestigationController (POST /api/v1/invoke, GET /api/v1/investigations/{id})
  └── AmlInvestigationApplication (the one module that becomes an executable Spring Boot app)

eval-harness                 [not yet implemented]
```

The dependency direction is one-way and enforced by Maven itself: if `agent-tools-common`
depended on a sub-agent module, and that module also depended back on `agent-tools-common`
(which it must, to implement its port), the build would fail on a circular reference before
anything compiled.

`agent-orchestrator/pom.xml` is where the compile-vs-runtime distinction becomes a real,
enforced constraint rather than just a convention: `agent-kyc`, `agent-sanctions`, and
`agent-network` are declared with `<scope>runtime</scope>`. Those jars are present so Spring
can find and wire the `Local*Investigator` beans at startup, but Maven does not put them on the
compile classpath — this module's own source code is structurally unable to import anything
from those packages, even by accident.

### Ports and adapters

Each sub-agent module exposes only a thin port interface (`KycInvestigator`, etc.) and the
shared `SubAgentResult` type to the rest of the system. Its actual LangChain4j AiService
interface, `@Tool` classes, and integration client all stay private. A `Local*Investigator`
adapter bridges the two — implementing the port by delegating to the private AiService proxy.

This is deliberate preparation for a later split: today everything runs as one deployable, with
each sub-agent bundled as a library jar. When a sub-agent is eventually extracted into its own
service, only its adapter changes — a `Remote*Investigator` calling the new service over HTTP
replaces `Local*Investigator` — while the port interface, and every line of code that depends on
it, stays untouched.

### What a model is allowed to author

Each sub-agent AiService's return type (`AgentFindings`) contains only fields a model can
honestly produce: a domain-specific `status` classification, a `confidence` score, and cited
`findings`. Fields like `agentName` and latency are assembled afterward by the
`Local*Investigator` adapter, which measures or knows them directly — never asked of the model,
since a model has no way to know how long its own call took.

Sub-agents report only observed facts, never a verdict or risk judgment — status values like
`STRUCTURING_DETECTED` or `NO_MATCH` are deliberately distinct from the orchestrator's own
verdict vocabulary. `OrchestratorAgent`'s return type, `SarVerdict`, follows the same principle
one level up: `reviewNote` is always populated, but `sarNarrative` is only populated when the
verdict is `FILE_SAR` and left empty otherwise — not a workaround, since when no SAR is being
filed there genuinely is no SAR narrative to write.

### Synthesis rules

`OrchestratorAgent`'s system message encodes four rules, applied in order:

1. **`FILE_SAR`** — Network's status is `STRUCTURING_DETECTED`, and Sanctions' status is
   `MATCH` or `PARTIAL_MATCH`. Exception: if KYC's confidence is below 0.6, `PARTIAL_MATCH` is
   not sufficient — `MATCH` is required.
2. **`ENHANCED_DUE_DILIGENCE`** — Network's status is `NO_PATTERN_DETECTED`, and Sanctions'
   status is `MATCH`.
3. **`CLEAR`** — Network's status is `NO_PATTERN_DETECTED`, Sanctions' status is `NO_MATCH`,
   and KYC's status is `UBO_DISCLOSED`.
4. **Otherwise, `ESCALATE_TO_HUMAN`** — the default for any combination the first three rules
   don't match, including partial or ambiguous signals from any sub-agent. An unanticipated
   combination degrades to "ask a human" rather than silently defaulting toward `CLEAR` or
   `FILE_SAR`, either of which would be a dangerous failure mode here.

These rules were derived from five golden test cases, not written from a blank page — see
`docs/mentorship-log.md` (not version-controlled) for the full derivation.

### Fan-out and the timeout fallback

`InvestigationOrchestrator` is the actual orchestrator — plain Java, not an AiService. It calls
all three sub-agent ports in parallel via `CompletableFuture.supplyAsync`, each wrapped in its
own `.orTimeout(...)` budget (Network's Cypher traversal gets a longer allowance than KYC's or
Sanctions' REST lookups, since it's a genuinely different call shape).

If any single future fails — whether by timing out or throwing — the orchestrator does not call
`OrchestratorAgent` at all. It builds an `ESCALATE_TO_HUMAN` verdict directly in Java, with
`confidence = 1.0` (full confidence in the *policy* of escalating on incomplete data, not a
claim about the underlying investigation), a `reviewNote` naming exactly which sub-agents
completed and which timed out, and the findings from whichever sub-agents *did* complete —
nothing gets discarded just because one of the three failed. This holds regardless of how many
of the other two succeeded: two solid findings alongside one timeout still isn't treated as
enough to trust an automated verdict.

Only when all three sub-agents succeed does `OrchestratorAgent.synthesize(...)` get called —
the one point in this entire system where a second model call happens on top of the three
sub-agent calls.

## REST API

`POST /api/v1/invoke` is asynchronous, not a blocking call. It creates a `PENDING` investigation
record, kicks off `InvestigationOrchestrator.runInvestigation(...)`, and returns `202 Accepted`
with an investigation ID (and a `Location` header) almost immediately — it does not wait for
the investigation to finish. Binding the HTTP response to how long an investigation takes would
couple this system's behavior to transport-layer timeouts that have nothing to do with the
investigation itself, and risks losing the fallback `reviewNote` entirely if a client's own
connection times out before the server-side work does.

`GET /api/v1/investigations/{id}` retrieves the current state — `404` if the ID doesn't exist,
`200` with the full `InvestigationSummary` (status, verdict, confidence, review note, SAR
narrative, and every sub-agent's findings) otherwise.

## Persistence

`investigation-store` persists the full investigation, not just the final verdict — all three
`SubAgentResult`s are stored alongside it, since a regulator needs to see what evidence drove a
decision, not only the decision itself.

Its entities (`InvestigationEntity`, `SubAgentFindingEntity`) are deliberately separate types
from `SarVerdict`/`SubAgentResult`, for the same reason `AgentFindings` is separate from
`SubAgentResult`: a JPA `@Entity` has constraints (a no-arg constructor, mutable fields, an
identity key) driven by persistence mechanics, not by what a model can author. `InvestigationStore`
is the only class other modules are allowed to call — never the repository or the entities
directly.

Schema is managed by Flyway (`ddl-auto: validate` — Hibernate checks the schema matches, never
generates it), with the migration at
`investigation-store/src/main/resources/db/migration/V1__create_investigations_and_findings.sql`.

## Modules

| Module | Depends on (compile) | Depends on (runtime only) | Purpose |
|---|---|---|---|
| `agent-tools-common` | — | — | Shared ports and `SubAgentResult` |
| `agent-kyc` | `agent-tools-common` | — | Corporate structure and beneficial ownership |
| `agent-sanctions` | `agent-tools-common` | — | Watchlist, PEP, and adverse media screening |
| `agent-network` | `agent-tools-common` | — | Transaction graph traversal and structuring detection |
| `investigation-store` | — | — | Persistence: investigations and sub-agent findings |
| `agent-orchestrator` | `agent-tools-common`, `investigation-store` | `agent-kyc`, `agent-sanctions`, `agent-network` | Fan-out, synthesis, REST API — the executable app |

## Building

Requires JDK 21 and Maven.

```
mvn compile
```

To build the runnable application:

```
mvn package -pl agent-orchestrator -am
java -jar agent-orchestrator/target/agent-orchestrator-0.1.0-SNAPSHOT.jar
```

Two things worth knowing if you're extending this build:

- **`agent-orchestrator` is the only module with `spring-boot-maven-plugin`'s `repackage` goal
  bound to it**, and that binding is explicit (`<executions>`), not automatic — this project
  imports `spring-boot-dependencies` as a BOM for versions, not `spring-boot-starter-parent` as
  the actual Maven parent, so the automatic goal binding that comes with the starter-parent
  doesn't apply here. Without the explicit `<executions>` block, `mvn package` silently produces
  a thin jar with no bundled dependencies instead of a runnable one.
- **Every module's own `application.yml` is renamed to a unique classpath name**
  (`agent-kyc.yml`, `agent-sanctions.yml`, `agent-network.yml`, `investigation-store.yml`), and
  `agent-orchestrator`'s `application.yml` explicitly imports all four via `spring.config.import`.
  Bundled into one executable jar, four files all named `application.yml` would collide on the
  classpath — Spring Boot resolves that to one location, not a merge, so only one of the four
  would load, based on classpath ordering nobody controls.

## Configuration

`agent-orchestrator`'s `application.yml` imports each module's config; the environment
variables below are what those files actually read.

| Variable | Used by | Required |
|---|---|---|
| `ANTHROPIC_API_KEY` | `agent-kyc`, `agent-sanctions`, `agent-network`, `agent-orchestrator` | yes, no default |
| `CORPORATE_REGISTRY_BASE_URL` | `agent-kyc` | no, defaults to `http://localhost:8081` |
| `SANCTIONS_PROVIDER_BASE_URL` | `agent-sanctions` | no, defaults to `http://localhost:8082` |
| `NEO4J_URI` | `agent-network` | no, defaults to `bolt://localhost:7687` |
| `NEO4J_USERNAME` | `agent-network` | no, defaults to `neo4j` |
| `NEO4J_PASSWORD` | `agent-network` | yes, no default |
| `INVESTIGATION_DB_URL` | `investigation-store` | no, defaults to `jdbc:postgresql://localhost:5432/aml_investigations` |
| `INVESTIGATION_DB_USERNAME` | `investigation-store` | no, defaults to `aml` |
| `INVESTIGATION_DB_PASSWORD` | `investigation-store` | yes, no default |

`agent-orchestrator`'s own executor sizing and per-sub-agent timeout budgets
(`aml.orchestrator.executor.*`, `aml.orchestrator.timeout.*`) are set directly in its
`application.yml` rather than read from environment variables, since they're tuning parameters
rather than secrets or environment-specific endpoints.

## Status

All six application modules (`agent-tools-common`, `agent-kyc`, `agent-sanctions`,
`agent-network`, `investigation-store`, `agent-orchestrator`) are implemented, compile together,
and package into a working executable Spring Boot jar.

Not yet built: `eval-harness` (Layer 1 deterministic tests plus Layer 2 pass-rate-scored
calibration checks). Also not yet built: any real or stub implementation of the corporate
registry, sanctions provider, or a seeded Neo4j instance — running this system end-to-end today
would fail on every external call, since nothing is listening at any of the configured URLs yet.
