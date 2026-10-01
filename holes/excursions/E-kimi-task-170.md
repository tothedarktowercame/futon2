# E-kimi-task-170 — pattern-stage reading kimi-2-1790890069 (7 patterns)

**Requisition:** completed — 2026-10-01T21:31:07Z, job invoke-1790890069897-29868-a6084cbd, state done

Clocked in by pattern-stage-read-loop for kimi-2 on 2026-10-01 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

You are labelling design patterns for a count of what stage of an Active Inference control loop Joe's work touched. Label each pattern below from ITS OWN TEXT only (IF / HOWEVER / THEN / BECAUSE, else the `! conclusion`), never from its filename or directory.

kind (one):
- practice: a way of working a person or agent performs (review, plan, record, route, check).
- subject: what the work is about; content, a design decision, a domain technique (math formalisation techniques are always subject, even if imperative).
- mixed: genuinely both.

stage (one), with the gloss the existing 689 labels use:
- perceive: observes or exposes state
- believe: records, represents, or revises what is known
- evaluate: weighs consequences, options, or limits
- select: sets a choice, priority, or admission condition
- act: implements or executes the work
- assurance: makes a claim independently checkable or preserves its trace
- coordination: routes work, roles, or communication between parties
- none: the text states no control-stage operation (a lookup/encoding record, a bare meta-tag)

stage-mode: functional (the pattern PERFORMS that stage), topical (it is ABOUT that stage), or functional-and-topical.
confidence: high (clear from the text), medium, low.
node: optional, only if the text itself names an R-node (e.g. "R9"); else omit.
quote-field: IF, HOWEVER, THEN, BECAUSE or CONCLUSION.
quote: copy a span of 6-40 words EXACTLY from that field of the source shown (it is checked character for character after collapsing whitespace). Do not paraphrase, do not add ellipses.
rationale: one sentence: why this stage, citing the quote.

Answer: write /tmp/claude17/pattern-stage-read/kimi-2-1790890069.answer.json as a JSON array, one object per pattern:
  {"id": ..., "kind": ..., "stage": ..., "stage-mode": ..., "confidence": ..., "node": optional, "quote-field": ..., "quote": ..., "rationale": ...}
Rewrite the file after each pattern and check it parses: python3 -c "import json;print(len(json.load(open('/tmp/claude17/pattern-stage-read/kimi-2-1790890069.answer.json'))))"
Do all 7. Do not write anywhere else, do not commit, do not publish. Nobody needs belling; end with one line: the answer path and the count.


---
## vsatlas/peeragogical-infrastructures
source: futon3/library/vsatlas/peeragogical-infrastructures.flexiarg

```
@flexiarg vsatlas/peeragogical-infrastructures
@title Peeragogical Infrastructures
@audience educators, peer-learning designers
@tone analytic
@style design-pattern
@sigils [👍/门 🌀/田]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L7-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: 
  Embed scaffolds for co-authorship, negotiation of meaning, stewardship, and joint decision-making inside the manifold.

  + context: You are designing a complex knowledge ecosystem that actively teaches contributors how to collaborate, negotiate meaning, and share responsibility, not just create content. This does not apply to short, one-off workshops without ongoing community formation.

  + IF:
    Contributors learn media-making but struggle with collaborative coordination and shared responsibility.

  + HOWEVER:
    The platform teaches creation but not co-governance.

  + THEN:
    Embed scaffolds for co-authorship, negotiation of meaning, stewardship, and joint decision-making inside the manifold.

  + BECAUSE:
    Peeragogical competence is required to maintain shared worlds and enable social imagination.

  + NEXT-STEPS:
    next[Add one explicit co-authorship ritual to the onboarding flow.]
    next[Define a stewardship checkpoint that requires shared decision-making.]
    next[Define success signals (e.g., shared decisions recorded) and a failure signal (e.g., coordination breakdown persists).]
```


---
## war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships
source: futon3/library/war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships.flexiarg

```
@flexiarg war-room/wr-11-external-applications-carry-predecessor-exemplar-relationships
@title WR-11: External Applications Carry Predecessor-Exemplar Relationships Back Into The Stack
@sigils [⚖/令]
@audience futon stack operators, agents, mission authors
@tone foundational
@style pattern
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L6-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: Promote an external application to an operational blueprint when an inward mission names its artefact as a predecessor, and carry that predecessor-exemplar relationship back into the spine with bulletin-grade visibility.

  + context: War Room decision WR-11 (2026-05-17). Operator: Joe. Strengthened from bulletin-9 draft 1.

  + IF:
    An external application produces an artefact that an inward mission's HEAD explicitly names as a predecessor.

  + HOWEVER:
    Treating the relationship as merely an external citation leaves the application classified as a substrate deliverable even though the inward pilot is being authored from it.

  + THEN:
    Graduate the application from substrate deliverable to operational blueprint, give the graduation bulletin-grade visibility, and write the predecessor-exemplar relationship to the spine.

  + BECAUSE:
    The predecessor project is the exemplar being ported inward, so its operational shape is part of the stack's own next move rather than adjacent external work.

    + evidence: The v3-runner work produced the UKRN-S commissioning sheet and `v11_year1_scenarios_for_jacobs.xlsx`; M-interim-director names that apparatus as the predecessor exemplar for the inward-pointed pilot.
```


---
## war-room/wr-22-logic-model-before-code-is-a-sanctioned-verify-method
source: futon3/library/war-room/wr-22-logic-model-before-code-is-a-sanctioned-verify-method.flexiarg

```
@flexiarg war-room/wr-22-logic-model-before-code-is-a-sanctioned-verify-method
@title WR-22: Logic-Model-Before-Code Is A Sanctioned VERIFY Method
@sigils [⚖/令]
@audience futon stack operators, agents, mission authors
@references [mission-coherence/logic-model-before-code social/tension-before-code]
@tone foundational
@style pattern
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L6-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: Sanction executable logic models over adversarial traces as a VERIFY method for design invariants before implementation code or its tests exist.

  + context: War Room decision WR-22 (2026-05-31). Operator: Joe. The method is distinct from the sibling tension-before-code discipline, which is tests-first.

  + IF:
    A mission must verify that its design invariants are mutually satisfiable and enforceable before building the implementation or its tests.

  + HOWEVER:
    A logic model cannot settle empirical or substrate-dependent guarantees, while a tests-first method cannot verify the design before the code and its tests exist.

  + THEN:
    Execute the design as a logic model over abstract adversarial traces, and carve out every empirical or substrate-dependent guarantee into a separate spike.

  + BECAUSE:
    The model identifies exactly which invariants can be checked structurally and exactly which residue still requires contact with the real substrate.

    + evidence: The first reuse in M-aif2 VERIFY Stage A returned verified true for four of four invariants, shrank the Stage B empirical spike to its irreducible core, and surfaced the tension-polarity finding cleanly.
```


---
## writing-coherence/cross-section-claim-drift
source: futon3/library/writing-coherence/cross-section-claim-drift.flexiarg

```
@flexiarg writing-coherence/cross-section-claim-drift
@title Use One Canonical Wording for the Same Claim
@keywords claim-drift, cross-section, consistency, canonical-wording, inadvertent-paraphrase
@audience paper authors, reviewers
@tone technical
@style pattern
@references [writing-coherence/paraphrase-drift]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L9-no-source-check.edn (L6-L10 sweep, 2026-09-05)


! conclusion: When the same factual claim is made in two sections of a paper using different wording, the reader cannot tell whether the difference is a refinement, a qualification, or an inconsistency.  Reviewers default to inconsistency.  A load-bearing factual claim should have one canonical wording, used verbatim wherever the claim is invoked, with deliberate variation only when the variation is itself doing argumentative work.

  + context: Section-level rewrites — done at different times, often in response to different reviewer comments — produce drift that the writer doesn't notice because each section locally reads well.  Methods says "X for purposes A, B, C"; Limitations says "X for purposes A, B"; Discussion says "X for purpose A".  Each is plausible in isolation; together they read as the writer not having decided what the claim is.  Distinct from `paraphrase-drift` (intra-paragraph elegant restatement that fails to advance): operates at the section level and concerns *factual* alignment, not rhetorical advancement.

  + IF:
    The pattern operates on the axis: local fluency in each section ↔ factual coherence across the paper.
    Irreducible: a load-bearing factual claim either has a canonical formulation in the paper or it does not.  Drift cannot simultaneously be (a) inadvertent paraphrase and (b) deliberate argumentative variation.

  + HOWEVER:
    Real costs: a single canonical wording lets the reader recognise repeat invocation as repeat invocation.  Variation forces the reader to reconcile the variants and decide which is authoritative.
    Active dynamic: the pull recurs in any paper rewritten in passes, especially when different reviewers prompt edits in different sections.

    + FAILURE-MODES:
      - Inadvertent narrowing: Limitations restates the claim with one element dropped, suggesting (without intending to) that the dropped element doesn't apply there.
      - Inadvertent widening: Discussion restates the claim with one element added, suggesting a stronger claim than the paper supports.
      - Stylistic paraphrase: the writer rephrases for variety, treating the claim as prose rather than as a fixed proposition.
      - Vocabulary drift across sections: the same entity is named differently in different sections ("training input", "the training course", "the T3 programme"), so the reader cannot tell whether they are the same.

  + THEN: Operate an explicit move on the named axis (see substructure below); steward the pattern against the declared discipline.
    + COMPOSITIONS:
      (1) Establish the canonical wording: in the section where the claim does its primary work, settle on one phrasing.  Treat that phrasing as the load-bearing version.
      (2) Quote-and-reuse elsewhere: when the same claim is invoked in another section, use the canonical wording verbatim, or refer back to its first statement.
      (3) Mark deliberate variation: where a section needs to *qualify* the canonical claim ("subject to X", "in the limit of Y"), make the qualification explicit; do not let the variation read as paraphrase.
      (4) Audit during integration: when finalising a draft, list every load-bearing claim and the sections where it appears; check that each appearance uses the canonical wording or marks its variation.

    + CHECK:
      Engaged when: every restatement of a load-bearing claim either uses the canonical wording verbatim or visibly marks its variation as deliberate.  Failing when: the same claim appears in three sections with three subtly different formulations and no signal that any variation is intentional.
```


---
## writing-coherence/paraphrase-drift
source: futon3/library/writing-coherence/paraphrase-drift.flexiarg

```
@flexiarg writing-coherence/paraphrase-drift
@title Every Sentence Must Advance
@keywords paraphrase, restatement, development, advancement, elegant-drift, semicolon-parallel
@audience paper authors, reviewers
@tone technical
@style pattern
@references [writing-coherence/triad-inflation writing-coherence/throat-clearing-close writing-coherence/meta-lede]
@provenance no grounding source found for this pattern; deterministic search receipt: runs/L9-no-source-check.edn (L6-L10 sweep, 2026-09-05)

! conclusion: Each sentence in a paragraph must add something the preceding sentences did not.  A sentence that re-expresses a prior claim in prettier language — however elegant the rephrasing — is drift, not development.  The paragraph ends in roughly the same place it started, and the reader has absorbed two or three beautiful versions of a single idea rather than one idea plus two or three consequences of it.

  + context: Paraphrase drift is the most literate failure mode in AI-assisted prose.  It reads as sophisticated because each restatement reaches for a new metaphor — "the surface is a transcript", "evidence of a nervous system", "a rendering medium" — and the cumulative effect is of depth.  But a reader who tracks what the paragraph claims, sentence by sentence, finds the sentences say the same thing.  A good paragraph states the claim once and then spends its remaining sentences on what follows from it: a mechanism, a consequence, a contrast, a complication.

  + IF:
    The pattern operates on the axis: restatement ↔ development.
    Irreducible: a single sentence cannot simultaneously restate the prior sentence and advance beyond it.  The choice is local and binary.

  + HOWEVER:
    Real costs: restatement is easier, prettier, and feels safer — each new phrasing reduces the risk that one phrasing lands badly.  Development is harder because it commits to a next claim that must itself be defended.
    Active dynamic: the pull recurs at every sentence boundary, especially after a sentence the writer is pleased with.

    + FAILURE-MODES:
      - Metaphor stack: "the surface is a transcript; paint as evidence of a nervous system; a record of its own making" — three images, one claim.
      - Semicolon-pair chains: the same contrast re-rendered as "X does A; Y does B" three times with different As and Bs that mean the same thing.
      - Elegant-close drift: the paragraph's last sentence is its most beautiful restatement rather than its strongest consequence.
      - Synonymous parallelism: "Not only X, but also Y" where X and Y are the same observation in different words.

  + THEN: Operate an explicit move on the named axis (see substructure below); steward the pattern against the declared discipline.
    + COMPOSITIONS:
      (1) Develop: replace a restatement sentence with a sentence that names the mechanism, consequence, or condition that follows from the prior claim.  The paragraph now advances.
      (2) Cut: delete the restatement entirely.  If the prior sentence was well-made, the paragraph does not lose argument by losing the paraphrase.
      (3) Promote one phrasing, cut the others: if multiple paraphrases exist across consecutive sentences, keep the strongest and cut the rest.  A single well-chosen metaphor beats three stacked.

    + CHECK:
      Engaged when: every sentence in a paragraph adds a claim, mechanism, consequence, or precondition not present in the preceding sentences.  Failing when: consecutive sentences map, under paraphrase, to the same underlying assertion.
```


---
## 象/制度有时
source: futon3/library/象/制度有时.flexiarg

```
@flexiarg 象/制度有时
@title 制度有时：协议与规则有生效之时，按行为发生时的制度评判它
@keywords 制度, 协议, 版本, 生效时间, 繁文缛节, 事故, 象2000
@audience 操作者, 立规则的代理, 事故清结者
@tone 分析
@style pattern
@status draft 2026-09-26 (claude-14；据麦卡锡原文 elephant.tex，象2000 语族，尚未评审)
@why [象/三层规格 象/收回亦是行]
@see-also [inbox-zero/gate-fails-loudly]
@how 据 M-象-2000 级联 v2（futon3c holes/labs/M-象-2000/cascade-象2000-v2.edn）的令牌流，本模式成立之后可做的下一步：:制度版本生效区间 → 象/措施随案。
@provenance 麦卡锡 elephant.tex l.1453–1470：许多言语行为依附于会变的社会制度；决斗的废止部分靠有意改变这些制度；程序之间的言语行为往往需要设计新的制度。l.1779–1781：订座应被视为制度，「短暂而可变」。l.761–763：人类制度常常发明言语行为。

! conclusion: 协议、闸门与常规都是制度：各带版本与生效区间；一个行为按它发生时有效的制度评判；为应对事故而立的制度挂在该事故之下，事故清结时随之失效，除非另行一次明确的采纳。

  + context: 铃与停泊的协议、CLAUDE.md 中的规则、为事故而装的闸门。

  + IF:
    一条规则已经立下，而没有人记得它为什么还在。

  + HOWEVER:
    制度一旦立下就只凭惯性延续。为一次事故拉起的警戒线若没有拆除的条件，就会变成对所有人的常设关卡——红线原是黄线。可若要求报告事故时就写出拆除条件，报告会变贵，事故会少报。

  + THEN:
    制度记录其版本与生效区间。事故报告保持廉价；清结事故须附证明（「若 P 在 T0 已生效，事故不会发生」，以回放检验）。事故期间所立的制度挂在事故之下，清结时一并失效；要保留，须另做一次采纳，作为新的承诺入史。未清结的事故只在清单变化时报告，不在每个回合重复（inbox-zero/gate-fails-loudly）。

  + BECAUSE:
    麦卡锡把订座看作制度而非定义，正因为它会变。规则既然是制度，就应当有时间；有时间，才能问它此刻是否还该存在。

    + evidence: 2026-09-24 15:48 Joe 报告 Kimi 额度耗尽；15:54 原因已查明（claude-10 自身的派工，d5e3147e）；16:03—16:34 目标闸门与申领闸门相继装上（80428193、5146606d），此后申领提醒 42 次进入各会话，直至 09-25 被称为「繁文缛节」。
    + COUNTERFACTUAL: 若挂在事故之下的规则，在清结时被显式采纳为常设规则的比例接近百分之百，则自动失效只是多了一道手续，应改为提醒复审。
```


---
## 象/言即行
source: futon3/library/象/言即行.flexiarg

```
@flexiarg 象/言即行
@title 言即行：把言语行为写成一等公民
@keywords 言语行为, 施为, 力, 力度, 象2000, 控制语言, 铃, 设计模式
@audience 操作者, 代理, 调度层的作者
@tone 分析
@style pattern
@status draft 2026-09-23 (claude-1；象2000 语族的第一块基石，尚未评审)
@see-also [象/象不忘 translation/route-the-untranslatable cascades/declared-skeleton]
@how 据 M-象-2000 级联 v2（futon3c holes/labs/M-象-2000/cascade-象2000-v2.edn）的令牌流，本模式成立之后可做的下一步：:行为类型 → 象/两种规格、象/以史为据、象/双时并记、象/名分有据、象/收回亦是行、象/翻译契约、象/行有定名、象/视图出于史、象/诺必践、象/释义非授。
@provenance 出自 2026-09-23 的操作者回合 turn-zqqgaU 与 turn-Ogz2uU：麦卡锡的 Elephant 2000 跑在设计模式之上。

! conclusion: 每一次传递都必须同时声明它的言语行为类型和它的模式内容；类型在信封上，内容在级联里，两者缺一不可。

  + context: 操作者与代理之间的每一次传递——铃、哨、派工、回执。

  + IF:
    一条消息既要说明「这是什么行为」（请求、断言、质疑、收回、答复），又要说明「行为作用于什么」（哪些设计模式，按什么次序组合）。

  + HOWEVER:
    现有的两层互不相连。futon3c 的定型铃已经带有施为类型——query、answer、assert、challenge、agree、define、retract、suggest、request——但正文是散文；级联带有模式地址，却没有类型。于是接收者必须从散文里猜出这是命令还是建议，而猜测正是错误进入的地方。

  + THEN:
    把类型放在信封上，把级联放在正文里：`(bell :type request :force 强 :cascade (...))`。答复必须带 `ref` 指回它所答的问题。接收者若无法据此行动，就按 translation/route-the-untranslatable 带类型退回，而不是猜。
    类型之外还要记力：同一种言语行为有轻重之分，而轻重决定接收者欠下多少。取三档——`轻`（试探：「大概」「或许」「我们可以」）、`平`（未加修饰的默认）、`强`（加重：「务必」「我告诉你」、重复、责备）。力必须由话中的词承担：声明 `:force` 就要一并给出承载它的 `:force-span`，即原文中的确切偏移。力不是译者对语气的印象，而是可被指认、可被反驳的一段字。

  + BECAUSE:
    言语行为的类型决定了接收者欠下什么：请求欠一个行动，断言欠一次核验，收回解除先前的承诺。类型若只存在于语气里，欠债就无法被记录，也就无法被追讨。

    + evidence: futon3c/CLAUDE.md §Typed Bell Contract 已实现九种施为类型与 `ref` 线索，但正文仍为自然语言。
    + evidence: 2026-09-23，claude-1 误以 brief 模式派出委托：接收者收到文字、未执行任何工作，而作业状态仍报 done。信封上的类型与正文中的意图不一致，记录便说了谎。
    + evidence: 2026-09-23，claude-1、zai-2、kimi-2 三方各自翻译同一回合，互不参看。十八个模式 id 中仅两个为三方共引；而三方各自独立报告了同一处缺口——九种类型之下没有轻重，于是「我们可以」与「务必」记成同一件事。三个对齐的实例，满足 cascade-construction/lift-when-three-align 的准入。
    + evidence: 同日 turn-v2qr9R，操作者以两句演示这一维度：「No, look, you're wrong.」与「See, I'm telling you, you're wrong.」——类型未变，力变了。按改前的写法，二者记录相同。
    + evidence: 「力」一词在本库中另有旧义（Alexander 的诸力，见各模式的 + HOWEVER）。操作者 2026-09-23 裁定：本项目只用言语行为一义，旧义留待论文另述。一名两义是 @why 失守的成因，故此处明记。
    + COUNTERFACTUAL: 若带类型的级联与散文派工在澄清回铃率上没有差别，则类型未起作用，本模式应予撤回。
    + COUNTERFACTUAL: 若三档力在译者之间的一致率，与不记力时无异，则力只是译者的印象，未能由词承担，本条应予撤回。
```
