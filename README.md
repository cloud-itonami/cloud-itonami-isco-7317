# cloud-itonami-isco-7317

Open Occupation Blueprint for **ISCO-08 7317**: Handicraft Workers in Wood, Basketry and Related Materials.

This repository designs a forkable OSS business for a wood-and-basketry handicraft workshop scheduling and logistics coordination practice: a workshop scheduling and supply-coordination robot manages crew/task records under a governor-gated actor, so a wood/basketry handicraft workshop keeps its own operating records instead of renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/woodbasketry/` implements the
`WoodBasketryActor` as a `langgraph.graph/state-graph`
(`woodbasketry.actor`) wired to a `Wood & Basketry Handicraft Workshop
Advisor` (`woodbasketry.advisor`) and an independent
`WoodBasketryGovernor` (`woodbasketry.governor`), following the
itonami actor pattern (ADR-2607121000): `:intake -> :advise -> :govern
-> :decide -+-> :commit (:ok?) +-> :request-approval (:escalate?,
human-in-the-loop interrupt) +-> :hold (:hard?)`. 21 tests / 45
assertions green (`clojure -M:test`). HARD invariants (always hold,
never overridable): worker provenance, workshop provenance,
no-actuation (`:effect` must be `:propose`), a closed op-allowlist
(`:log-work-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would directly finalize a craft-fabrication-execution
decision (e.g. deciding to proceed with a specific carving, weaving or
finishing step) or override a workshop safety officer's judgment.
Always-escalate paths (human sign-off regardless of confidence,
mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a workshop scheduling/logistics coordination robot performs crew scheduling, task/materials-usage/progress-record logging and wood/basketry-materials supply-order coordination for a handicraft workshop, under an actor that proposes actions and an independent **WoodBasketryGovernor** that gates them. The governor never
dispatches hardware itself, never performs the craft fabrication work in the workshop, and never finalizes a craft-fabrication-execution decision or overrides a workshop safety officer's judgment; `:high`/`:safety-critical` actions (such as a flagged cut-hazard/dust-exposure/repetitive-strain-injury concern, or an above-threshold supply order) require human sign-off. **This actor coordinates workshop scheduling/logistics only — it never performs the craft fabrication work itself.**

## Core Contract

```text
crew roster + workshop registration + safety-reporting policy
        |
        v
Wood & Basketry Handicraft Workshop Advisor -> WoodBasketryGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
a craft-fabrication-execution decision, override a workshop safety officer's
judgment, suppress an operating record, or disclose sensitive data without
governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7317`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
