# AML Investigation Agent

A multi-agent AML (anti-money-laundering) investigation system. One orchestrator delegates to
three independent sub-agents — KYC, Sanctions, and Network — each running its own LangChain4j
AiService against Claude, to investigate a flagged account and reach one of four verdicts:
`FILE_SAR`, `CLEAR`, `ESCALATE_TO_HUMAN`, or `ENHANCED_DUE_DILIGENCE`.

Built with Java 21, Spring Boot 3, and LangChain4j.

## How the AiServices actually work

This is the part of the codebase most likely to look like it's missing something on first read,
so it's worth explaining directly: interfaces like `KycAgent`, `SanctionsAgent`, and
`NetworkAgent` have **no hand-written implementing class anywhere in this repository.**

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

agent-orchestrator           [not yet implemented]
investigation-store          [not yet implemented]
eval-harness                 [not yet implemented]
```

The dependency direction is one-way and enforced by Maven itself: if `agent-tools-common`
depended on a sub-agent module, and that module also depended back on `agent-tools-common`
(which it must, to implement its port), the build would fail on a circular reference before
anything compiled.

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

Each AiService's return type (`AgentFindings`) contains only fields a model can honestly
produce: a domain-specific `status` classification, a `confidence` score, and cited `findings`.
Fields like `agentName` and latency are assembled afterward by the `Local*Investigator` adapter,
which measures or knows them directly — never asked of the model, since a model has no way to
know how long its own call took.

Sub-agents report only observed facts, never a verdict or risk judgment — status values like
`STRUCTURING_DETECTED` or `NO_MATCH` are deliberately distinct from the orchestrator's own
verdict vocabulary. Only the orchestrator's synthesis step, reasoning over all three sub-agents'
findings at once, is allowed to reach `FILE_SAR` / `CLEAR` / `ESCALATE_TO_HUMAN` /
`ENHANCED_DUE_DILIGENCE`.

## Modules

| Module | Depends on | Purpose |
|---|---|---|
| `agent-tools-common` | — | Shared ports and `SubAgentResult` |
| `agent-kyc` | `agent-tools-common` | Corporate structure and beneficial ownership |
| `agent-sanctions` | `agent-tools-common` | Watchlist, PEP, and adverse media screening |
| `agent-network` | `agent-tools-common` | Transaction graph traversal and structuring detection |

## Building

Requires JDK 21 and Maven.

```
mvn compile
```

## Configuration

Each sub-agent module reads its own `application.yml`, expecting these environment variables:

| Variable | Used by | Required |
|---|---|---|
| `ANTHROPIC_API_KEY` | all sub-agent modules | yes, no default |
| `CORPORATE_REGISTRY_BASE_URL` | `agent-kyc` | no, defaults to `http://localhost:8081` |
| `SANCTIONS_PROVIDER_BASE_URL` | `agent-sanctions` | no, defaults to `http://localhost:8082` |
| `NEO4J_URI` | `agent-network` | no, defaults to `bolt://localhost:7687` |
| `NEO4J_USERNAME` | `agent-network` | no, defaults to `neo4j` |
| `NEO4J_PASSWORD` | `agent-network` | yes, no default |

## Status

Sub-agent modules (`agent-kyc`, `agent-sanctions`, `agent-network`) are implemented and compile
together. Not yet built: `investigation-store` (persistence), `agent-orchestrator` (fan-out and
synthesis), and `eval-harness`.
