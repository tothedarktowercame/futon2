#!/usr/bin/env python3
"""Read-only source census. Writes census.csv and README.md beside this script.
Manual judgments are pinned to exact source hashes: changed sources require review.
No server queries, lifecycle writes, git staging, or repository changes are made.
"""
from pathlib import Path
import argparse, collections, csv, datetime as dt, hashlib, json, re, subprocess

REPOS = "futon0 futon1 futon1a futon1b futon2 futon3 futon3a futon3b futon3c futon4 futon5 futon5a futon6 futon7 futon7a p4ng".split()
GROUPS = ["closed-witnessed", "closed-unwitnessed", "open", "undetermined"]
AS_OF = dt.date(2026, 9, 21)
REVIEWS = {'futon0/holes/M-what-is-it-who-is-it-for.md': {'group': 'open',
                                                'line': 3,
                                                'quote': '**Status:** HEAD captured 2026-08-17 · **MAP '
                                                         'complete** 2026-08-17 (§2) ·',
                                                'reason': 'MAP completion is phase-local; IDENTIFY and '
                                                          'DERIVE explicitly not started (lines 3–4).',
                                                'sha256': 'b593022e40a058445c7fc3e25a85cd5754644bbdd856065ba4f4c561f58d13c7'},
 'futon0/holes/excursions/E-aif-daisyworld.md': {'group': 'open',
                                                 'line': 4,
                                                 'quote': '**Status:** CHARTER + log. Phases 1–3a done; 3b '
                                                          '(REINFORCE) = honest negative; **3b′ (EVOLUTION) '
                                                          '= positive** —',
                                                 'reason': 'CHARTER plus partial phase results; the carried '
                                                           'full closed-loop test remains at lines 77–80.',
                                                 'sha256': '80de8b2d4acfeee845adaba9de1d854b99e3d0f7d1df99488d32e882b8b2e099'},
 'futon0/holes/missions/M-capability-star-map.md': {'group': 'closed-unwitnessed',
                                                    'line': 219,
                                                    'quote': '### Completion criteria (testable)',
                                                    'reason': 'Operator close is recorded at 1276–1286, but '
                                                              'the four completion criteria at 219–225 have '
                                                              'no complete discharge ledger; integration and '
                                                              'four caveats are transferred, not an explicit '
                                                              'retirement of the charter.',
                                                    'sha256': 'e10feaa7dbf6e07df4988e9f34c546f71e1aefd2a2b81e7f60032cc439947de0'},
 'futon0/holes/missions/M-futonzero-capability.md': {'group': 'closed-unwitnessed',
                                                     'line': 155,
                                                     'quote': '- [ ] **G5 Task Specification:** '
                                                              'capability/functioning/conversion-factor',
                                                     'reason': 'COMPLETE banner and later implemented '
                                                               'monitor are documented, but eight mission '
                                                               'gate obligations at 155–166 remain '
                                                               'unchecked; no explicit retirement of that '
                                                               'checklist.',
                                                     'sha256': '8ce2b2563a59da9de973f83b4ba742e15499d53290b7b2f5212285552c1cf2e6'},
 'futon2/holes/E-C-vector-live.md': {'group': 'closed-witnessed',
                                     'line': 98,
                                     'quote': '**Exit conditions — met (verified, auditable):**',
                                     'reason': 'Five excursion exit conditions explicitly met with live C '
                                               'derivation, freshness/re-ranking checks and '
                                               'c_vector_test.clj; later predictive-risk/durable-join '
                                               'delivery recorded, full PROOF store excluded.',
                                     'sha256': '7dffb96fbe9df99d6473418e97000d71c8ca7abbffb4e343948aa8e84858b290'},
 'futon2/holes/E-KL-refinements.md': {'group': 'closed-witnessed',
                                      'line': 3,
                                      'quote': '**Date:** 2026-07-03 · **Status:** ✅ **CLOSED** (Joe, '
                                               '2026-07-03 — "if the work is',
                                      'reason': 'All five exit items individually disposed with commits '
                                                '0f8d5c6, 22b0024 and eb06565; exit at 89–91 permits the '
                                                'recorded negative/declined outcomes.',
                                      'sha256': 'f1627ade93ecf79ccb9f10a95bf93eb5490cf70c13b83915e19e934f9f897142'},
 'futon2/holes/E-aif2-partB.md': {'group': 'open',
                                  'line': 4,
                                  'quote': '**Status:** **FIRST-PASS DONE (2026-06-24), full adversarial '
                                           'round still optional.** A first-pass triangulation',
                                  'reason': 'Only FIRST-PASS DONE; full adversarial charter is retained as '
                                            'optional hardening at 9–10, without a whole-excursion closure '
                                            'or retirement.',
                                  'sha256': '2b92739ba7712a1725f9d8c1bf108b00951c6f844185cdd8382bae4ef41a66bb'},
 'futon2/holes/E-cascade-sampler-sampler.md': {'group': 'open',
                                               'line': 9,
                                               'quote': 'Status: IN FLIGHT — v0 complete (checkpoints 0–4); '
                                                        'v1 mid-flight at handoff.',
                                               'reason': 'IN FLIGHT and v1 mid-flight; v0 complete does not '
                                                         'close v1.',
                                               'sha256': 'a84aa11cb2fab9595e02723fd94c3e03482a52322b38a7f0f057f79c271bcfb9'},
 'futon2/holes/E-evaluate-policies-spikes.md': {'group': 'closed-witnessed',
                                                'line': 21,
                                                'quote': '',
                                                'reason': 'Both chartered spikes explicitly DONE; '
                                                          'regenerated argue-exhibit.pdf and '
                                                          'spike-spectral/spectral-comparison.json supply '
                                                          'the two output pointers.',
                                                'sha256': '4f4d8223bda213e0cf7d94b705250e8245481e3d0b23490976f4045036179fec'},
 'futon2/holes/E-futon1b-foothold.md': {'group': 'closed-witnessed',
                                        'line': 7,
                                        'quote': '**Findings (full record in `futon1b/NOTES.md`):** both '
                                                 'excursion questions',
                                        'reason': 'Both explicitly bounded excursion questions answered YES; '
                                                  'futon1b/NOTES.md and seam-swap probes named as the '
                                                  'results, not a claim that the full port is complete.',
                                        'sha256': '225fc45ee58b4234c80d3ea8b92af9247922c8e5d5a1085578049fd88a624f43'},
 'futon2/holes/E-have-want-pairs.md': {'group': 'closed-witnessed',
                                       'line': 77,
                                       'quote': '### ✅ Closure ruling (claude-10, author, 2026-07-02)',
                                       'reason': 'Closure ruling discharges all three exit conditions Q-A, '
                                                 'Q-B and foliation; scorecard, diffsub-moves.edn and '
                                                 'ecf2a37 are named evidence.',
                                       'sha256': 'f6039449b63d5201bd2cd23da5bdbab7b5a4f5a0e9a610dd97e81baf74ab65a1'},
 'futon2/holes/E-policy-rollout-engine.md': {'group': 'closed-witnessed',
                                             'line': 4,
                                             'quote': '**Status:** ✅ **LANDED + REVIEWED PASS 2026-06-09.** '
                                                      'codex-2 built (futon2 `65f137d` "Add policy',
                                             'reason': 'Build/review record marks MUST-A, MUST-B, witness '
                                                       'and regression checks satisfied; commits 65f137d and '
                                                       '2122daf. Historical pre-build hold at tail is '
                                                       'superseded by this delivered header.',
                                             'sha256': '37c5bd1fbc77b876a9f922383d51259d9e482eec3b50382473eec0e1069c36c2'},
 'futon2/holes/E-precision-over-policies.md': {'group': 'closed-witnessed',
                                               'line': 35,
                                               'quote': '  enactor, read by γ next tick (async-clean). '
                                                        'fold.clj stays expected-only.',
                                               'reason': 'All five exit conditions individually marked met; '
                                                         'policy_precision.clj, regression test and '
                                                         'README-gamma.md supply pointers.',
                                               'sha256': '4cf5e35aad7105dda24687309537b9f956abf402e5d04c0a6174e9a785df8758'},
 'futon2/holes/E-r18-faithfulness-audit.md': {'group': 'closed-witnessed',
                                              'line': 3,
                                              'quote': '**Date:** 2026-07-03 · **Owner:** claude-2 '
                                                       '(dispatched by claude-11, Joe-ratified) · '
                                                       '**Status:** DELIVERED — R18 goes',
                                              'reason': 'Bounded audit delivers a literature/code badge for '
                                                        'all 16 named quantities in data/r18-badges.edn; '
                                                        'rendering and faithfulness repairs explicitly '
                                                        'follow-on scope.',
                                              'sha256': '56592f9db16db8f3ccbc57308fb0dd09bd02fbfd3919b7beda04a692d073c729'},
 'futon2/holes/M-G-over-cascades.md': {'group': 'open',
                                       'line': 4,
                                       'quote': '**Status:** HEAD + IDENTIFY + MAP done; now in '
                                                '**exploratory DERIVE**. Per Joe (2026-06-23): slice-1 '
                                                '(reviewed PASS) and slice-2a are *exploratory probes*, '
                                                '**not** INSTANTIATE — only from their findings will we '
                                                'commit to a design, ARGUE it, VERIFY, then INSTANTIATE the '
                                                'real artifact. slice-2a probe in flight (claude-1). '
                                                'Continuation of [[M-wm-policies]].',
                                       'reason': 'Explicit exploratory DERIVE; production cascade-policy '
                                                 'seam remains owed at 888–899.',
                                       'sha256': 'cb0a55e9fa4f0e1bd219f1f9ea351c0b0aa6eca347c933a8f838a09c5753fce1'},
 'futon2/holes/M-action-vocabulary.md': {'group': 'closed-witnessed',
                                         'line': 3,
                                         'quote': '**Status: STOPPED BY KILL CRITERION (2026-07-05, same day '
                                                  'as charter —',
                                         'reason': 'Explicit kill-criterion stop after two negative trials, '
                                                   'with f5fde3a3 and the reason that operator value is not '
                                                   'expressible at this grain; retirement branch, not '
                                                   'successful delivery.',
                                         'sha256': 'f6df8e75221ec6c122215de7eb8bb3f3e781d597e67627393e0310a3e32c4ef2'},
 'futon2/holes/M-aif-head.md': {'group': 'closed-unwitnessed',
                                'line': 162,
                                'quote': '### Completion criteria',
                                'reason': 'COMPLETE and all nine implementation handoffs recorded at '
                                          '1896–1910, but no explicit discharge mapping for all seven '
                                          'mission criteria, particularly autonomous default-mode '
                                          'demonstration and all-decision evidence emission.',
                                'sha256': 'b9d7da93cb8c6a57477c8aed5531aa3f8f87de91af9c9f1d960f5b409da1fc87'},
 'futon2/holes/M-aif-wiring.md': {'group': 'open',
                                  'line': 4,
                                  'quote': '**Status:** **IDENTIFY** (new mission; successor home for the '
                                           'R14–R18 gaps surfaced by the now-closed M-wm-policies). *Name '
                                           'provisional — Joe may rename.*',
                                  'reason': "IDENTIFY is this mission's status; now-closed refers to its "
                                            'predecessor M-wm-policies.',
                                  'sha256': '0c88ff352a47236e89a2638f082d8838cdac11afc92fe772b1a39d334f64f0b8'},
 'futon2/holes/M-aif2.md': {'group': 'closed-witnessed',
                            'line': 620,
                            'quote': '',
                            'reason': 'Final closure section explicitly rechecks all seven IDENTIFY criteria '
                                      'as met, with aif2-exotype.edn, slice-1/β implementation and '
                                      'five-source dispositions; earlier unchecked list is superseded by '
                                      'FINAL list.',
                            'sha256': '4f1b89f6a7c7a5a8b556b151b8995f9b8b5bc05a49915a8f5eb48da43258f660'},
 'futon2/holes/M-composition-aware-reward.md': {'group': 'open',
                                                'line': 3,
                                                'quote': '**Status: IDENTIFY complete (evidence below) → '
                                                         'DERIVE/INSTANTIATE chartered',
                                                'reason': 'IDENTIFY complete is followed by '
                                                          'DERIVE/INSTANTIATE chartered; no mission closure.',
                                                'sha256': 'ef15ec6eb47a999841e1e4276a2544ac4c7b2d073f9ea9abb4d58bc61ad4868b'},
 'futon2/holes/M-custom-harness.md': {'group': 'open',
                                      'line': 16,
                                      'quote': 'excursion are all done and live-verified. Remaining: '
                                               'DOCUMENT. Parked:',
                                      'reason': 'VERIFY complete but header explicitly says Remaining: '
                                                'DOCUMENT.',
                                      'sha256': '5e42157a8e60ef206b24e0aa3f4187d9f5ace29830ecada8b8090b45d23ffee7'},
 'futon2/holes/M-evaluate-policies.md': {'group': 'open',
                                         'line': 14,
                                         'quote': '(author ≠ reviewer throughout). **OPEN at '
                                                  'close-candidate:** C10 (Joe) · E7',
                                         'reason': 'All phases through DOCUMENT claimed complete, but C10/E7 '
                                                   'remain open at close-candidate and mission-close is '
                                                   "still Joe's call; tail retains these evidence steps.",
                                         'sha256': 'c83a9855f7f9b35f3ff167ab22f2e975a7d509b719fbcfef54d3e45ee2a17e54'},
 'futon2/holes/M-fold-ansatz.md': {'group': 'open',
                                   'line': 3,
                                   'quote': '**Date:** 2026-07-01 · **Status:** MAP COMPLETE (claude-4 — the '
                                            "scatter is mapped; see §MAP M1–M4; awaiting Joe's ARGUE steer). "
                                            'IDENTIFY complete below.',
                                   'reason': 'MAP complete only; awaiting operator steer and open follow-on '
                                             'decision at 631–635.',
                                   'sha256': '10cbe1877f96f5de50471040f51be10cc0e03bfb11325cecddbca27084e53d45'},
 'futon2/holes/M-futon1b-port.md': {'group': 'closed-witnessed',
                                    'line': 818,
                                    'quote': '### P-criteria scoreboard (final)',
                                    'reason': 'Final P1–P4 and E1–E4 tables mark every criterion MET with '
                                              'scripts, assertions, parity output and PAR IDs; explicit '
                                              'operator close at 837.',
                                    'sha256': '7a3d0226c477e8d2c254c7619c4af4e21220371dee8546a21cc33bea0854a6af'},
 'futon2/holes/M-mission-conditional-reward.md': {'group': 'closed-witnessed',
                                                  'line': 3,
                                                  'quote': '**Status: CLOSED 2026-07-12 (honest negative) — '
                                                           'see §INSTANTIATE-OUTCOME',
                                                  'reason': 'Explicit honest-negative close/stand-down: A1 '
                                                            'fails, d9f38f3/e0b7972 and battery log record '
                                                            'the reason; no production flip, any further '
                                                            'attempt requires a new mission.',
                                                  'sha256': '8b891f0e383d4cd5e4c6b976cac490a9afcf925ed96abdf3e8ccfe7885802a55'},
 'futon2/holes/M-peradam-mechanization.md': {'group': 'open',
                                             'line': 3,
                                             'quote': '**Status: P1-P3 MACHINERY LANDED DARK (2026-07-05 '
                                                      'late evening — flown',
                                             'reason': 'P1–P3 machinery landed dark, but header awaits two '
                                                       'rulings and E1 reward seam remains in the completion '
                                                       'criteria.',
                                             'sha256': '78254c32e053d93c206974efad6038810d1f4f8be4a0f32aa29b264820698605'},
 'futon2/holes/M-wm-policies.md': {'group': 'closed-witnessed',
                                   'line': 1143,
                                   'quote': '## 5. DOCUMENT — completion check, AIF-theory touch-map, close '
                                            'recommendation (claude-2, 2026-06-24)',
                                   'reason': 'DOCUMENT enumerates all four criteria MET with live '
                                             'measurements, documented-run sibling and commits '
                                             'ac93ed4/af26ef6/5708281; added Track-3 obligations recorded '
                                             'delivered.',
                                   'sha256': 'f3f13512b0394df7b81abadff39576ca692884e11c88ec23969ddb8d2974f455'},
 'futon2/holes/evidence/M-first-flights-closure-mechanical-path.md': {'group': 'undetermined',
                                                                      'line': 64,
                                                                      'quote': '**Recommended status choice: '
                                                                               '`SUPERSEDED-AS-MISSION '
                                                                               'toward M-fold-ansatz`**',
                                                                      'reason': 'Companion report labels '
                                                                                'Phase A complete but '
                                                                                'proposes alternative '
                                                                                'closure dispositions rather '
                                                                                'than establishing this '
                                                                                "document's own final "
                                                                                'lifecycle.',
                                                                      'sha256': '9228f6cae6e6bca7d101f27db84748c584906fede1a5aa120bd972feea656ebb'},
 'futon2/holes/missions/M-G-wm-wiring.md': {'group': 'open',
                                            'line': 4,
                                            'quote': '**Status:** HEAD. Campaign chartered by Joe '
                                                     '(emacs-repl, 2026-09-15): "we need to get real '
                                                     'evidence that it can be completed… a new '
                                                     'M-G-wm-wiring.md campaign that indexes into these '
                                                     'checklist items as its (complex) gap, and that '
                                                     'develops a strategy for working through them in a '
                                                     'reasonable order that will get us to completion."',
                                            'reason': 'HEAD charter says can be completed as a desired '
                                                      'future result, not a closure.',
                                            'sha256': '4569b69b8d4f9d7619b60f13bb0864819d372289f1cb55082499df2e0158f745'},
 'futon2/holes/missions/M-aif-a-matrix-faithfulness.md': {'group': 'open',
                                                          'line': 4,
                                                          'quote': '**Status:** **INSTANTIATE (Stage 1 '
                                                                   'production-wired; simulation spike '
                                                                   'complete; Stage 2 exogenous calibration '
                                                                   'pending)**',
                                                          'reason': 'Stage 2 exogenous calibration '
                                                                    'explicitly pending.',
                                                          'sha256': 'ced74827d8a7e6d0b9aad157f38c294f1553ae2f46e520c577e01bd4d6d24b7f'},
 'futon3/holes/excursions/E-Ttotal.md': {'group': 'open',
                                         'line': 4,
                                         'quote': '**Owner mission:** '
                                                  '`holes/missions/M-pattern-application-diagnostic.md`',
                                         'reason': 'v0 real data landed is an intermediate delivery '
                                                   'statement, not whole-excursion closure.',
                                         'sha256': '7690a895363a2df86fb8bf2143be31c800b069a6a951af34c1e63e201491c3b7'},
 'futon3/holes/missions/M-agency-forum.md': {'group': 'closed-witnessed',
                                             'line': 3,
                                             'quote': '**Status:** COMPLETE (SUPERSEDED) — Agency/Forum '
                                                      'interfaces replaced by futon3c evidence landscape '
                                                      '(store.clj + threads.clj) (2026-03)',
                                             'reason': 'Explicit SUPERSEDED disposition with reason: '
                                                       'Agency/Forum replaced by futon3c evidence store and '
                                                       'thread projection; retirement branch.',
                                             'sha256': '84c691a418c26e261b0d9016cf339e546a2e27bc4841a791d69acf1ca3ef204c'},
 'futon3/holes/missions/M-agency-rebuild.md': {'group': 'closed-witnessed',
                                               'line': 3,
                                               'quote': '**Status:** COMPLETE (SUPERSEDED) — Agency '
                                                        'invariants A0-A5 reimplemented in '
                                                        'futon3c/agency/registry.clj (2026-03)',
                                               'reason': 'Explicit SUPERSEDED disposition with reason: A0–A5 '
                                                         'reimplemented in futon3c/agency/registry.clj; '
                                                         'retirement branch.',
                                               'sha256': '1b5dfe4aa00bd13cfbba2d725ecf13d78302311d4b65960f47e1fda4353e61d2'},
 'futon3/holes/missions/M-agency-unified-routing.md': {'group': 'closed-unwitnessed',
                                                       'line': 70,
                                                       'quote': '## Success Criteria',
                                                       'reason': 'DONE, but seven success criteria remain '
                                                                 'unchecked; task completion and test counts '
                                                                 'do not discharge the whole criteria list.',
                                                       'sha256': 'fda35011773261f761525f63f085c836c0322baa8af49a09b15034ab7b4efa1f'},
 'futon3/holes/missions/M-arxana-graph-persistence.md': {'group': 'closed-witnessed',
                                                         'line': 185,
                                                         'quote': '## Success Criteria',
                                                         'reason': 'All five success criteria checked with '
                                                                   'musn/service.clj implementation and lab '
                                                                   'notebook contract pointers; future '
                                                                   'enhancements explicitly outside '
                                                                   'delivered phases.',
                                                         'sha256': '0d29c06d67362ffd1688965c6ebc418bae850866c81b22845f09cd111ebe6e51'},
 'futon3/holes/missions/M-codex-parity.md': {'group': 'closed-witnessed',
                                             'line': 157,
                                             'quote': '## Success Criteria',
                                             'reason': 'All five success criteria checked with session '
                                                       'codex-parity-20260202T211454Z, lab anchor/link files '
                                                       'and PAR IDs par-9e255cc4/par-636b4a99.',
                                             'sha256': '802209f702d90c596e362ecb932427066e80406efd6ec1a5538a9917465c5a27'},
 'futon3/holes/missions/M-coordination-rewrite.md': {'group': 'closed-unwitnessed',
                                                     'line': 2,
                                                     'quote': 'Status: archived',
                                                     'reason': 'Archived status records no retirement '
                                                               'reason; inspected scope/criteria have no '
                                                               'explicit complete discharge with a witness. '
                                                               'Archive stamp alone is insufficient; any '
                                                               'older status is retained in CSV.',
                                                     'sha256': '58d26c48f15a9ad11b048483132a54d9ad577feed500ddcad7935b58a77a05dd'},
 'futon3/holes/missions/M-drawbridge-multi-agent.md': {'group': 'closed-witnessed',
                                                       'line': 3,
                                                       'quote': '**Status:** COMPLETE (SUPERSEDED) — N:1 '
                                                                'shared agent registry in futon3c replaces '
                                                                'per-agent JVM model (2026-03)',
                                                       'reason': 'Explicit SUPERSEDED disposition: shared '
                                                                 'N:1 registry replaces the per-agent JVM '
                                                                 'design; retirement branch, not a claim '
                                                                 'that the old checklist shipped.',
                                                       'sha256': '31adefe641318ac0acb95334d5268f9759d3f52d42f88384c414b398a02c0d94'},
 'futon3/holes/missions/M-f6-agents.md': {'group': 'closed-unwitnessed',
                                          'line': 2,
                                          'quote': 'Status: archived',
                                          'reason': 'Archived status records no retirement reason; inspected '
                                                    'scope/criteria have no explicit complete discharge with '
                                                    'a witness. Archive stamp alone is insufficient; any '
                                                    'older status is retained in CSV.',
                                          'sha256': 'b34f034471ff94633866b8fea35adc447e3ebe5b34c9d4693c824ea550e53827'},
 'futon3/holes/missions/M-f6-arxiv.md': {'group': 'closed-unwitnessed',
                                         'line': 2,
                                         'quote': 'Status: archived',
                                         'reason': 'Archived status records no retirement reason; inspected '
                                                   'scope/criteria have no explicit complete discharge with '
                                                   'a witness. Archive stamp alone is insufficient; any '
                                                   'older status is retained in CSV.',
                                         'sha256': '096f19176165eed5059c351ca0b783234e024b56010740666ea14a644b8d0021'},
 'futon3/holes/missions/M-f6-eval.md': {'group': 'closed-unwitnessed',
                                        'line': 2,
                                        'quote': 'Status: archived',
                                        'reason': 'Archived status records no retirement reason; inspected '
                                                  'scope/criteria have no explicit complete discharge with a '
                                                  'witness. Archive stamp alone is insufficient; any older '
                                                  'status is retained in CSV.',
                                        'sha256': '739477f82543ebd50da5463cf906ddbd16f0ea8872597ff26c1f5b9e187b43f7'},
 'futon3/holes/missions/M-f6-ingest.md': {'group': 'closed-unwitnessed',
                                          'line': 2,
                                          'quote': 'Status: archived',
                                          'reason': 'Archived status records no retirement reason; inspected '
                                                    'scope/criteria have no explicit complete discharge with '
                                                    'a witness. Archive stamp alone is insufficient; any '
                                                    'older status is retained in CSV.',
                                          'sha256': 'f9fa23ca55c739b26e31b1fe9729f78738f45292afa61691b71e29e222f9ef37'},
 'futon3/holes/missions/M-f6-recursive.md': {'group': 'closed-unwitnessed',
                                             'line': 2,
                                             'quote': 'Status: archived',
                                             'reason': 'Archived status records no retirement reason; '
                                                       'inspected scope/criteria have no explicit complete '
                                                       'discharge with a witness. Archive stamp alone is '
                                                       'insufficient; any older status is retained in CSV.',
                                             'sha256': '541b8bc3bcf0aa4e6e0c9fe4c1df974fe31e5929ac556447f22ecc18ca00c79f'},
 'futon3/holes/missions/M-forum-organization.md': {'group': 'undetermined',
                                                   'line': 2,
                                                   'quote': 'Status: archived',
                                                   'reason': 'Undated archive stamp conflicts with explicit '
                                                             'active/greenfield/pending status; no '
                                                             'retirement reason or dated supersession '
                                                             'resolves precedence.',
                                                   'sha256': '7879fbc489fedde62b3dcbc8b2294976473670c2ceefb92a59bd686214e6fa4d'},
 'futon3/holes/missions/M-fucodex-parity.md': {'group': 'closed-witnessed',
                                               'line': 143,
                                               'quote': '## Success Criteria',
                                               'reason': 'All six success criteria checked with '
                                                         'fucodex-parity-test session, two link IDs and PAR '
                                                         'IDs in lab and RAP stores.',
                                               'sha256': 'a05cb14ca742b7326195b3c7c319e7e0fe8670f332f14972bc11445c2c16a328'},
 'futon3/holes/missions/M-futon1a-evidence.md': {'group': 'closed-unwitnessed',
                                                 'line': 2,
                                                 'quote': 'Status: archived',
                                                 'reason': 'Archived status records no retirement reason; '
                                                           'inspected scope/criteria have no explicit '
                                                           'complete discharge with a witness. Archive stamp '
                                                           'alone is insufficient; any older status is '
                                                           'retained in CSV.',
                                                 'sha256': '649dd0b705c3770633c023d0774663bfe71fe679435f1620df4ebe921a2b6231'},
 'futon3/holes/missions/M-futon1a-rebuild-scoping-review.md': {'group': 'closed-unwitnessed',
                                                               'line': 2,
                                                               'quote': 'Status: archived',
                                                               'reason': 'Archived status records no '
                                                                         'retirement reason; inspected '
                                                                         'scope/criteria have no explicit '
                                                                         'complete discharge with a witness. '
                                                                         'Archive stamp alone is '
                                                                         'insufficient; any older status is '
                                                                         'retained in CSV.',
                                                               'sha256': 'fcf3c150180326471614b8fd4d285a2f86a2074e71ba758b5506dee69a323fd9'},
 'futon3/holes/missions/M-futon1a-rebuild.md': {'group': 'closed-unwitnessed',
                                                'line': 2,
                                                'quote': 'Status: archived',
                                                'reason': 'Archived status records no retirement reason; '
                                                          'inspected scope/criteria have no explicit '
                                                          'complete discharge with a witness. Archive stamp '
                                                          'alone is insufficient; any older status is '
                                                          'retained in CSV.',
                                                'sha256': 'ac18a4250f16d1fa4adbfef63a4e705ce163449b5b39a287e549713553cf15f7'},
 'futon3/holes/missions/M-futon1a-workplan.md': {'group': 'closed-unwitnessed',
                                                 'line': 2,
                                                 'quote': 'Status: archived',
                                                 'reason': 'Archived status records no retirement reason; '
                                                           'inspected scope/criteria have no explicit '
                                                           'complete discharge with a witness. Archive stamp '
                                                           'alone is insufficient; any older status is '
                                                           'retained in CSV.',
                                                 'sha256': 'a17be66437852e1b7992529b0f711d5df0051e16aaafc80bb1200c19379b0b9b'},
 'futon3/holes/missions/M-futon3x-e2e.md': {'group': 'closed-witnessed',
                                            'line': 668,
                                            'quote': '### Completion Criteria Status',
                                            'reason': 'All seven criteria explicitly PASS with '
                                                      'proof-path/path-22293f1b-b.edn, endpoint and docbook '
                                                      'evidence; remaining items explicitly resolved.',
                                            'sha256': '9a9bc7cfc64086da58937c74430a6a925dc22daa6cf478cfe7e1386adc1aaa35'},
 'futon3/holes/missions/M-graph-unification.md': {'group': 'closed-unwitnessed',
                                                  'line': 2,
                                                  'quote': 'Status: archived',
                                                  'reason': 'Archived status records no retirement reason; '
                                                            'inspected scope/criteria have no explicit '
                                                            'complete discharge with a witness. Archive '
                                                            'stamp alone is insufficient; any older status '
                                                            'is retained in CSV.',
                                                  'sha256': '4a688b1b195a1bb3698b9d7232ef166683724d9e59b4ffcc61baba1ffc3db2dd'},
 'futon3/holes/missions/M-labs-integration.md': {'group': 'closed-witnessed',
                                                 'line': 3,
                                                 'quote': '**Status:** COMPLETE (SUPERSEDED) — Lab capture '
                                                          'reframed as evidence landscape in futon3c; '
                                                          'evidence store + thread projection replace Arxana '
                                                          'overlays (2026-03)',
                                                 'reason': 'Explicit SUPERSEDED disposition: evidence '
                                                           'landscape/thread projection replaces lab Arxana '
                                                           'overlays; retirement branch.',
                                                 'sha256': 'c15a5471b52878ed3424734852575e951ce083a84950e83e11b6422fcc4aebb2'},
 'futon3/holes/missions/M-live-geometric-stack.md': {'group': 'closed-witnessed',
                                                     'line': 3,
                                                     'quote': '**Status:** COMPLETE (2026-04-28). All seven '
                                                              'phases delivered;',
                                                     'reason': 'All seven phases delivered; two excluded '
                                                               'signatures explicitly closed as not '
                                                               'pain-driven; '
                                                               'CLEANUP-CHECKPOINT-2026-04-27.md records '
                                                               'disposition and successor scope.',
                                                     'sha256': 'f8b0b75b873925c0968b45cb7f6cd4039b231ed1b3c802e3571b739292b7cac4'},
 'futon3/holes/missions/M-make-agency-work-properly.md': {'group': 'closed-witnessed',
                                                          'line': 3,
                                                          'quote': '**Status:** COMPLETE (SUPERSEDED) — '
                                                                   'Identifier separation and runner '
                                                                   'defaults implemented in futon3c agency '
                                                                   'layer (2026-03)',
                                                          'reason': 'Explicit SUPERSEDED disposition with '
                                                                    'reason: identifier separation and '
                                                                    'runner defaults moved to futon3c '
                                                                    'agency; retirement branch.',
                                                          'sha256': '11a0a2db9cac277a35d2c56bff7a13719e9c7863df5d9e2499055bfde4ad35d3'},
 'futon3/holes/missions/M-mission-coherence-patterns.md': {'group': 'open',
                                                           'line': 4,
                                                           'quote': '**Status:** HEAD complete; IDENTIFY '
                                                                    'drafted; MAP pilot run on 3-mission '
                                                                    'corpus; DERIVE and beyond pending',
                                                           'reason': 'HEAD complete but DERIVE and beyond '
                                                                     'pending.',
                                                           'sha256': '968f9d60923894d171e508ae32d874bab2b2957675e272d5e564c55655ffdfb9'},
 'futon3/holes/missions/M-mission-control-scoping.md': {'group': 'undetermined',
                                                        'line': 2,
                                                        'quote': 'Status: archived',
                                                        'reason': 'Undated archive stamp conflicts with '
                                                                  'explicit active/greenfield/pending '
                                                                  'status; no retirement reason or dated '
                                                                  'supersession resolves precedence.',
                                                        'sha256': '27857eb2173d6de06c738b1a1d49c5aaba21c20245eb901df8fd127657f6a55b'},
 'futon3/holes/missions/M-native-plan-coherence.md': {'group': 'undetermined',
                                                      'line': 2,
                                                      'quote': 'Status: archived',
                                                      'reason': 'Undated archive stamp conflicts with '
                                                                'explicit active/greenfield/pending status; '
                                                                'no retirement reason or dated supersession '
                                                                'resolves precedence.',
                                                      'sha256': '785d35a64cf347fe6f0240786f031dbcdcf6caa775a44426c9d2a173e92ed0c4'},
 'futon3/holes/missions/M-par-session-punctuation.md': {'group': 'closed-witnessed',
                                                        'line': 3,
                                                        'quote': '**Status:** COMPLETE (SUPERSEDED) — PAR '
                                                                 'emission via futon3c '
                                                                 'peripheral/reflect.clj; detach/reattach '
                                                                 'via peripheral adapter model (2026-03)',
                                                        'reason': 'Explicit SUPERSEDED disposition: reflect '
                                                                  'peripheral and adapter model replace '
                                                                  'prior PAR/session mechanism; retirement '
                                                                  'branch.',
                                                        'sha256': 'ce65c80c21e6a6711dfb24778dac73d68bdaccec2f3d172b9e413d9291fc183f'},
 'futon3/holes/missions/M-pattern-inference-engine-scoping-review.md': {'group': 'closed-unwitnessed',
                                                                        'line': 2,
                                                                        'quote': 'Status: archived',
                                                                        'reason': 'Archived status records '
                                                                                  'no retirement reason; '
                                                                                  'inspected scope/criteria '
                                                                                  'have no explicit complete '
                                                                                  'discharge with a witness. '
                                                                                  'Archive stamp alone is '
                                                                                  'insufficient; any older '
                                                                                  'status is retained in '
                                                                                  'CSV.',
                                                                        'sha256': 'f1e0dc44dbba3a32f540aec145a08cbdd255766ba00f470dc5f3d2442a3420a8'},
 'futon3/holes/missions/M-pattern-inference-engine.md': {'group': 'closed-unwitnessed',
                                                         'line': 2,
                                                         'quote': 'Status: archived',
                                                         'reason': 'Archived status records no retirement '
                                                                   'reason; inspected scope/criteria have no '
                                                                   'explicit complete discharge with a '
                                                                   'witness. Archive stamp alone is '
                                                                   'insufficient; any older status is '
                                                                   'retained in CSV.',
                                                         'sha256': '93ad3bc0e97d3d7b7427beb385e4637f4d5d15096543cc37530418c767be404b'},
 'futon3/holes/missions/M-plan-mermaid-viewer.md': {'group': 'closed-unwitnessed',
                                                    'line': 2,
                                                    'quote': 'Status: archived',
                                                    'reason': 'Archived status records no retirement reason; '
                                                              'inspected scope/criteria have no explicit '
                                                              'complete discharge with a witness. Archive '
                                                              'stamp alone is insufficient; any older status '
                                                              'is retained in CSV.',
                                                    'sha256': 'df922377b3cf8ec574ecc015f2e0087479ed67f0412ddd7c8dfa00a5dcbbbc8c'},
 'futon3/holes/missions/M-understand-fucodex.md': {'group': 'closed-witnessed',
                                                   'line': 3,
                                                   'quote': '**Status:** COMPLETE (SUPERSEDED) — Bridge '
                                                            'architecture resolved by futon3c unified '
                                                            'dispatch + peripheral model (2026-03)',
                                                   'reason': 'Explicit SUPERSEDED disposition: unified '
                                                             'dispatch/peripheral model resolves the old '
                                                             'bridge architecture question; retirement '
                                                             'branch.',
                                                   'sha256': 'df2f14e80aeb348a1e555f14e81cfb347e896681b3b383e1edf7ac8f1b127b7f'},
 'futon3/holes/missions/M-ws-emacs-log-stream.md': {'group': 'closed-unwitnessed',
                                                    'line': 2,
                                                    'quote': 'Status: archived',
                                                    'reason': 'Archived status records no retirement reason; '
                                                              'inspected scope/criteria have no explicit '
                                                              'complete discharge with a witness. Archive '
                                                              'stamp alone is insufficient; any older status '
                                                              'is retained in CSV.',
                                                    'sha256': '44dfc3a96f20392dc0502de5b40e3676b6e4782e18d67f26fe6520c6a01b21d0'},
 'futon3a/holes/missions/E-patterns-and-missions-live.md': {'group': 'closed-witnessed',
                                                            'line': 129,
                                                            'quote': '1. **Diff** (`git show 8f8a099`): '
                                                                     'exactly 2 files — '
                                                                     '`minilm_pattern_embeddings.json` + '
                                                                     '`scripts/index_patterns.sh`. Nothing '
                                                                     'else touched.',
                                                            'reason': 'Independent review records all four '
                                                                      'excursion criteria met; repair commit '
                                                                      '8f8a099 and live API checks recorded.',
                                                            'sha256': 'aa4db26fa8d664a2b0af6a6d1bcd6bfafbe2ebc714efcd5b05ce2ef535645529'},
 'futon3a/holes/missions/E-wm-policy-arrow-seam.md': {'group': 'closed-witnessed',
                                                      'line': 5,
                                                      'quote': '**Status:** ✅ **LANDED 2026-06-09** — '
                                                               'codex-2 built (`9e9d446` "Wire meme arrows '
                                                               'to capability',
                                                      'reason': 'Commit 9e9d446 implements arrow seam; '
                                                                'independent worked example '
                                                                'e-advances-cap-ascent.clj passes all five '
                                                                'required cases; consumer wiring explicitly '
                                                                'outside this excursion.',
                                                      'sha256': '84aae62cd7aa7920c136f84f4ebe458b12712c273508cf8f79a6cd7f12076c05'},
 'futon3a/holes/missions/M-memes-arrows-patterns-diagrams.BHK-research.md': {'group': 'closed-unwitnessed',
                                                                             'line': 3,
                                                                             'quote': 'Status: archived',
                                                                             'reason': 'Archived status '
                                                                                       'records no '
                                                                                       'retirement reason; '
                                                                                       'inspected '
                                                                                       'scope/criteria have '
                                                                                       'no explicit complete '
                                                                                       'discharge with a '
                                                                                       'witness. Archive '
                                                                                       'stamp alone is '
                                                                                       'insufficient; any '
                                                                                       'older status is '
                                                                                       'retained in CSV.',
                                                                             'sha256': '0f5f5a72d08dcade24fb40f6c5871cff0ccc32c62a2432ab0bb53c48f44530e3'},
 'futon3a/holes/missions/M-memes-arrows-patterns-diagrams.md': {'group': 'closed-witnessed',
                                                                'line': 1510,
                                                                'quote': '### 12.9 T4 exit condition '
                                                                         'SATISFIED — live substrate-2 write '
                                                                         '(operator-greenlit 2026-06-09)',
                                                                'reason': 'Both mission exit conditions T4 '
                                                                          'and T3 explicitly SATISFIED with '
                                                                          'h5-live-substrate2-promotion.clj '
                                                                          'and t3-count-watch-armed.clj; '
                                                                          'DOCUMENT closed by '
                                                                          'README-memes-and-arrows.md.',
                                                                'sha256': '928e40e7c6357d0b3622a7b54676fe6a4610479c94d18dec28d4425500c77760'},
 'futon3b/holes/missions/M-coordination-rewrite.md': {'group': 'closed-unwitnessed',
                                                      'line': 498,
                                                      'quote': '- [ ] Composition map maps all six gates to '
                                                               'existing code (§1.1)',
                                                      'reason': 'COMPLETE, but five Part I success criteria '
                                                                'remain unchecked; Parts II/III and test '
                                                                'counts do not explicitly resolve that list.',
                                                      'sha256': 'd5d8d0e5208b89235688df4239cb398a7103e90fce8f535b95f1fbf6987a2723'},
 'futon3c/holes/E-memory-latency.md': {'group': 'open',
                                       'line': 3,
                                       'quote': '**Status: BUILD-TEST COMPLETE; MONITOR (2026-07-23). Owner: '
                                                'Joe + Zaif/WM',
                                       'reason': 'BUILD-TEST COMPLETE followed by MONITOR; the monitoring '
                                                 'phase remains active.',
                                       'sha256': '9a13078c1225b3213cebda76918cf29dd2dcb62b09aa0e73cfa8684f78b22fc1'},
 'futon3c/holes/campaigns/C-substrate-completion.E3-escrow.draft.md': {'group': 'closed-witnessed',
                                                                       'line': 3,
                                                                       'quote': '> **Superseded by the '
                                                                                'canonical ledger.** This '
                                                                                'draft is retained as the '
                                                                                'authoring record; the live',
                                                                       'reason': 'Explicitly superseded by '
                                                                                 'C-substrate-completion.md '
                                                                                 'section 3; retained only '
                                                                                 'as authoring record after '
                                                                                 'insertion into canonical '
                                                                                 'ledger (recorded '
                                                                                 'retirement reason).',
                                                                       'sha256': '24c9f68932078bbbfe7b8dcb423dc3bcd14f5e61eba97b967a9fe5c8d98a56f9'},
 'futon3c/holes/campaigns/C-substrate-completion.md': {'group': 'open',
                                                       'line': 4,
                                                       'quote': '**Status:** CHARTER + CONSTITUTION + ESCROW '
                                                                '(2026-05-31) + STANDARD-ARGUE (§4) + '
                                                                '**STANDARD-VERIFY RATIFIED (Joe, '
                                                                '2026-06-01, PASS-on-design)**. Keystone '
                                                                '`M-substrate-metric` is **DELIVERED (v1)** '
                                                                'with named residue; O1 closed; E1 curvature '
                                                                'query delivered; E2 continuity cut passed '
                                                                'O4(b)/O4(c). **Escrow E1 + E2 '
                                                                '`:contract-released`** — consumers build to '
                                                                'the verified spec. **→ DISSOLVED (Joe, '
                                                                '2026-06-03)** — both dissolution criteria '
                                                                'met (STANDARD-VERIFY passed + E1 consumed '
                                                                "live by M-aif2's tension-proposer); closure "
                                                                'record + inspired-inquiry residue map + the '
                                                                'named high-priority follow-up '
                                                                '`M-intent-curvature` in **§9**. Working '
                                                                'mode: **swarm**.',
                                                       'reason': 'Campaign charter/escrow and keystone '
                                                                 'delivery do not close the whole campaign.',
                                                       'sha256': 'c11d176af722065be9ad400cbe704e714062f2b6cf2393e8920aba15c348c970'},
 'futon3c/holes/excursions/E-R5-red-ring-fill.md': {'group': 'undetermined',
                                                    'line': 13,
                                                    'quote': '## Status',
                                                    'reason': 'Status section is a table of claim '
                                                              'confirmation/bearer/theory, not a mission '
                                                              'lifecycle state; no unambiguous open/closed '
                                                              'status.',
                                                    'sha256': '3660bdfb1bd2ab2096a9ce63dd326243d6e2eabf434de8ba92141fadbf950abc'},
 'futon3c/holes/excursions/E-first-flights-policy-grade-G-closure.md': {'group': 'closed-unwitnessed',
                                                                        'line': 63,
                                                                        'quote': "- **Exit 9** (Joe's second "
                                                                                 'side-by-side verdict): '
                                                                                 'this is the operator gate, '
                                                                                 'not a machine-checkable '
                                                                                 'condition. The '
                                                                                 '`*live-wire?*` flag '
                                                                                 '(enactment wiring) remains '
                                                                                 "off pending Joe's consent "
                                                                                 "— but the mission's own "
                                                                                 'arming revision '
                                                                                 '(checkpoint 22) changed '
                                                                                 'from "rollout engine '
                                                                                 'lands" to "terms '
                                                                                 'constructed + metric '
                                                                                 'understood", both of which '
                                                                                 'are now true. The pure '
                                                                                 'path is ready; the live '
                                                                                 "wiring is Joe's call.",
                                                                        'reason': 'Verdict CLOSED, but exit '
                                                                                  '9 is still an operator '
                                                                                  'gate; pure-path readiness '
                                                                                  'and revised arming '
                                                                                  'preconditions do not '
                                                                                  "record Joe's second "
                                                                                  'side-by-side verdict.',
                                                                        'sha256': '399128ed5bd57dd6a561d4d1cf6b28d953da7527d63def99f4dd3176b223e66e'},
 'futon3c/holes/excursions/E-first-flights-transferred-work.md': {'group': 'closed-witnessed',
                                                                  'line': 42,
                                                                  'quote': '2026-07-06 by `codex-1`.',
                                                                  'reason': 'W1 independent closure receipt '
                                                                            'b88a81f plus W2 later '
                                                                            'interactive closure with '
                                                                            'flight.spec.edn and '
                                                                            'flight-typed-ground-witness.edn; '
                                                                            'both bounded transferred '
                                                                            'obligations disposed.',
                                                                  'sha256': '5c50fc5c34c038f5bba9b8d666bcfc5d87350e5ce81e049cfea45b2c5679c8c4'},
 'futon3c/holes/excursions/E-first-flights-typed-grounds-closure.md': {'group': 'closed-witnessed',
                                                                       'line': 37,
                                                                       'quote': '## Witness',
                                                                       'reason': 'Producer/spec/witness '
                                                                                 'scope discharged by '
                                                                                 'flight-typed-ground-witness.edn '
                                                                                 'generated through real '
                                                                                 'composer and CONFORMS '
                                                                                 'verification; historical '
                                                                                 'migration explicitly '
                                                                                 'excluded.',
                                                                       'sha256': '9f259547f6ccf2f8d90b6bc56def51e62d60aed1e43e6ae4854b5baeb2421e39'},
 'futon3c/holes/excursions/E-first-flights-typed-grounds-tail-closure.md': {'group': 'open',
                                                                            'line': 5,
                                                                            'quote': '**Verdict:** **HOLD**',
                                                                            'reason': 'Verdict HOLD; '
                                                                                      'typed-grounds '
                                                                                      'migration blocker '
                                                                                      'explicitly remains '
                                                                                      'open in this dated '
                                                                                      'census (not silently '
                                                                                      'upgraded by another '
                                                                                      'document).',
                                                                            'sha256': 'a92efb31e8bb80888cc87f6874c51874eedc1e7040d5d736272f96b7ff12c66c'},
 'futon3c/holes/excursions/E-futon1a-archivist.md': {'group': 'closed-witnessed',
                                                     'line': 75,
                                                     'quote': 'All four acceptance criteria met:',
                                                     'reason': 'Later completion record explicitly '
                                                               'discharges all four acceptance criteria, '
                                                               'including live write rejection and 246-node '
                                                               'migration at 431aca7; early IDENTIFY header '
                                                               'is stale.',
                                                     'sha256': '94470df1a016d3974bbcbd2f9bd6051c4a908af808def10384907ba60a4abc03'},
 'futon3c/holes/excursions/E-monster-to-joey.md': {'group': 'closed-unwitnessed',
                                                   'line': 149,
                                                   'quote': '## VERIFY (acceptance — to define at DERIVE)',
                                                   'reason': 'CLOSED with implementation commits, but '
                                                             'acceptance includes lower cost and less loop '
                                                             'lag plus idempotent recovery; no explicit '
                                                             'all-criteria discharge, lag has a separate '
                                                             'unresolved cause.',
                                                   'sha256': '166d56e5526f86c00ed93c3663dfed2deed3cc7e2204b0d000de51e2d352ce3b'},
 'futon3c/holes/excursions/E-scope-organism-copar.md': {'group': 'closed-witnessed',
                                                        'line': 4,
                                                        'quote': 'Status: CLOSED (2026-07-09). '
                                                                 '`futon6/scripts/clean_to_lean.py` now '
                                                                 'emits a',
                                                        'reason': 'The sole renderer fidelity gap is '
                                                                  'recorded implemented with '
                                                                  'clean_to_lean.py and domain-copar output; '
                                                                  'regression records 26 proofs, zero sorry, '
                                                                  'byte-identical default path.',
                                                        'sha256': '3e072c83cbbb174711ec566b543d56cb46bebcab53549974aff9bccf18de5739'},
 'futon3c/holes/excursions/E-shutdown-agents-killed-the-pools.md': {'group': 'closed-witnessed',
                                                                    'line': 52,
                                                                    'quote': '',
                                                                    'reason': 'Both closing changes and the '
                                                                              'actual destructive-state '
                                                                              'throwaway-JVM verification '
                                                                              'are recorded, with '
                                                                              'src/repl/http.clj as the '
                                                                              'implementation pointer.',
                                                                    'sha256': '4dcfb0e804d34a002cc335c3641aedc588e6bb77c2f587af5e2636c571f6dcad'},
 'futon3c/holes/missions/E-g-incorporates-deltaT.md': {'group': 'open',
                                                       'line': 4,
                                                       'quote': '**Status:** CYCLE 2 DRAFTED — '
                                                                'implementation landed locally + live WM '
                                                                'cache recomputed; awaiting claude-1 review '
                                                                '/ operator ratification before Cycle 3.',
                                                       'reason': 'CYCLE 2 drafted; reviewer/operator '
                                                                 'ratification outstanding before Cycle 3.',
                                                       'sha256': '82407e364650d193389becd87acf5c6337743c923e04de986343a504860a8ce6'},
 'futon3c/holes/missions/E-night-shift.md': {'group': 'open',
                                             'line': 4,
                                             'quote': '**Status:** PARTIALLY EXECUTED by Codex on '
                                                      '2026-05-25. The envelope, registry/spec wiring, '
                                                      'invariant tests, and spike-check are landed; '
                                                      'pilot-side hop integration and a live '
                                                      'operator-reviewed PR demonstration remain open.',
                                             'reason': 'Partially executed; pilot integration and '
                                                       'operator-reviewed demonstration remain open.',
                                             'sha256': 'ec73df8719593b2c24c1ae3474ae793f2adfb61f2dcd30ce88beeaa08231ff4b'},
 'futon3c/holes/missions/E-pilot-vsatarcs-feed.md': {'group': 'open',
                                                     'line': 4,
                                                     'quote': '**Status:** EXECUTED by Codex on 2026-05-25. '
                                                              'Mechanism landed; 9-item backfill ingested '
                                                              'into canonical VSATARCS; next live substrate '
                                                              'event still needed to demonstrate '
                                                              'post-landing auto-flow.',
                                                     'reason': 'Mechanism executed but next live substrate '
                                                               'event still required to demonstrate '
                                                               'auto-flow.',
                                                     'sha256': '92f9d4dce95c5a524fc1d67d72b274a6eb45d6fa4715c92c0845c1ba1853b8b6'},
 'futon3c/holes/missions/E-street-sweeper.md': {'group': 'open',
                                                'line': 4,
                                                'quote': 'the in-flight mission needs addressed but '
                                                         "shouldn't derail to build",
                                                'reason': 'ACTIVE; deferred packet queue awaits operator '
                                                          'review.',
                                                'sha256': 'efd20c49af5816219306aa8b6a5a2aca9a04f50b37fee34583a427ea35248240'},
 'futon3c/holes/missions/M-IRC-stability.md': {'group': 'closed-witnessed',
                                               'line': 196,
                                               'quote': '- [x] Server sends PING to idle clients after '
                                                        ':ping-interval-ms',
                                               'reason': 'All recorded checklists marked met, six failure '
                                                         'modes fixed, 16 stability tests; implementation '
                                                         'and test file pointers in the document.',
                                               'sha256': '00270da50f0b66ffa7ee5ae6ea54b3c618a3f524885852ad5b876d6bdcb8d063'},
 'futon3c/holes/missions/M-action-cost-modelling.md': {'group': 'open',
                                                       'line': 4,
                                                       'quote': '**Status:** HEAD / IDENTIFY / MAP / DERIVE '
                                                                '/ ARGUE / VERIFY all drafted and '
                                                                'operator-ratified through 2026-05-27. '
                                                                "VERIFY's 10 carried-forward tensions: 4 "
                                                                'done (T1, T5, T8, T10), 4 held for '
                                                                'downstream INSTANTIATE (T2, T4, T6, T9 — '
                                                                'all blocked on '
                                                                "`E-substrate-2-sorry-typing.md`'s own "
                                                                'INSTANTIATE which is a future cycle), 2 '
                                                                'future-Joe-triggered (T3, T7). Mission '
                                                                'ready for INSTANTIATE when Joe ratifies.',
                                                       'reason': 'Lifecycle phases drafted/ratified but '
                                                                 'carried-forward INSTANTIATE tensions '
                                                                 'remain.',
                                                       'sha256': 'b545fc982855744a341d60a490be6cbbd6a8a1e3c2dcd897bc7959f470c504e9'},
 'futon3c/holes/missions/M-agency-hardening-datapoints.md': {'group': 'closed-unwitnessed',
                                                             'line': 3,
                                                             'quote': 'Status: archived',
                                                             'reason': 'Archived status records no '
                                                                       'retirement reason; inspected '
                                                                       'scope/criteria have no explicit '
                                                                       'complete discharge with a witness. '
                                                                       'Archive stamp alone is insufficient; '
                                                                       'any older status is retained in CSV.',
                                                             'sha256': '897be4642944b69fc3227a708216c0503f1ca41ccf6a43785e761b7acf8ba646'},
 'futon3c/holes/missions/M-agency-hardening.md': {'group': 'closed-unwitnessed',
                                                  'line': 324,
                                                  'quote': '- [ ] Two humans and at least three agent roles '
                                                           'can share the IRC channel with',
                                                  'reason': 'CLOSED but eleven acceptance boxes remain '
                                                            'unchecked; August regression explicitly leaves '
                                                            'formal reopening to Joe, not a recorded '
                                                            'retirement.',
                                                  'sha256': '48cc537f859f05c37708dfdd13e46c639418b8a938d91aa3d474198ab336d45d'},
 'futon3c/holes/missions/M-agency-refactor.md': {'group': 'closed-unwitnessed',
                                                 'line': 2,
                                                 'quote': 'Status: archived',
                                                 'reason': 'Archived status records no retirement reason; '
                                                           'inspected scope/criteria have no explicit '
                                                           'complete discharge with a witness. Archive stamp '
                                                           'alone is insufficient; any older status is '
                                                           'retained in CSV.',
                                                 'sha256': 'f8732076670115bb232786027e1a9717ca5ccb24264a8fcfd70c35e3a44715b7'},
 'futon3c/holes/missions/M-alfworld-pattern-discovery.md': {'group': 'closed-witnessed',
                                                            'line': 15,
                                                            'quote': '**Completion criterion:** 10 flexiarg '
                                                                     'patterns written to '
                                                                     '`library/alfworld/`.',
                                                            'reason': 'The sole completion criterion is ten '
                                                                      'flexiarg patterns; header records '
                                                                      '10/10 written and commit 2713661.',
                                                            'sha256': '3873e18583ded437f93ea6924dcd1fa651e3161c3aa64037b26e3dd43d20df6b'},
 'futon3c/holes/missions/M-apm-demonstration.md': {'group': 'open',
                                                   'line': 4,
                                                   'quote': '**Gate:** operator-acceptance — HEAD must be '
                                                            'recognised as faithful to the',
                                                   'reason': 'HEAD complete, IDENTIFY draft pending operator '
                                                             'acceptance.',
                                                   'sha256': '0abc8484a418f6ea4cd42573dd2942ce0b28571fb4216043664c4e6c89e7788e'},
 'futon3c/holes/missions/M-archaeology-control.md': {'group': 'open',
                                                     'line': 1,
                                                     'quote': '**Status:** INSTANTIATE complete (2026-04-29) '
                                                              'for three sibling artifact-classes; deferred '
                                                              'siblings recorded in scope-out.',
                                                     'reason': 'Only INSTANTIATE for three artifact classes '
                                                               'is complete; no whole-mission closure '
                                                               'asserted and remaining siblings are scoped '
                                                               'out.',
                                                     'sha256': '3c2db5317d5eb962dd7d5a0dc93bb746d7431c80dc58ea077689102e26cf7b73'},
 'futon3c/holes/missions/M-bounded-disposition.md': {'group': 'open',
                                                     'line': 1,
                                                     'quote': '**Status:** INSTANTIATE complete (2026-04-29) '
                                                              'for the stash slice; siblings handed to Codex '
                                                              'via GitHub issue.',
                                                     'reason': 'Only stash-slice INSTANTIATE is complete; '
                                                               'sibling work handed off, not whole-mission '
                                                               'completion.',
                                                     'sha256': '86db60dbe4dfb159757a2bd2a4f9f07cb2304d3b25cfdea5100de5ecd22e368b'},
 'futon3c/holes/missions/M-codex-agent-behaviour.md': {'group': 'closed-witnessed',
                                                       'line': 482,
                                                       'quote': '### Completion criteria check',
                                                       'reason': 'All six completion criteria mapped to '
                                                                 'enforcement code, tests and two live '
                                                                 'invokes; scripts/test_enforcement_live.clj '
                                                                 'is the demonstration pointer.',
                                                       'sha256': '32acf8c52d9cc9382af88f3939b4717b209c5b95f9bd76c409b6874bb44b4992'},
 'futon3c/holes/missions/M-codex-irc-execution.md': {'group': 'closed-witnessed',
                                                     'line': 349,
                                                     'quote': '### 5.3 Completion criteria check (IDENTIFY '
                                                              '-> VERIFY)',
                                                     'reason': 'All five completion criteria explicitly met '
                                                               'with dev_irc_summary_test, http_test, '
                                                               'delivery recorder and durable-ledger '
                                                               'recovery tests.',
                                                     'sha256': '01c9b1cc199713d5690d8093ea1a49be92edf8a2de5da5d5eaf7b2a1f3fb7aa9'},
 'futon3c/holes/missions/M-dionysus-winddown.md': {'group': 'open',
                                                   'line': 4,
                                                   'quote': '**Gate:** operator-decision — placement and '
                                                            'sensitivity of raw session history '
                                                            '(`~/.claude`, `~/.codex`): live mirror on a '
                                                            'Linode, encrypted archive, or offline copy?',
                                                   'reason': 'HEAD/IDENTIFY and most MAP completed; no '
                                                             'terminal lifecycle claim.',
                                                   'sha256': 'c57be12323645df4eeac1795d5007de068cd357ef23da8d7f436fc06519706ea'},
 'futon3c/holes/missions/M-dispatch-peripheral-bridge.md': {'group': 'closed-witnessed',
                                                            'line': 377,
                                                            'quote': '- [x] Full pipeline → peripheral → '
                                                                     'receipt works end-to-end',
                                                            'reason': 'All 25 implementation/integration '
                                                                      'checks marked met including each exit '
                                                                      'function, proof-tree and regression '
                                                                      'checks; session/dispatch source '
                                                                      'pointers and 342-test result '
                                                                      'recorded.',
                                                            'sha256': '879a6ad6123a5015131bffafeefe22425ef69946e9465a615570b14025fd8bf1'},
 'futon3c/holes/missions/M-first-flights.md': {'group': 'closed-witnessed',
                                               'line': 4,
                                               'quote': 'Status: SUPERSEDED-AS-MISSION toward '
                                                        'E-first-flights-transferred-work — Phase A COMPLETE '
                                                        '(2026-06-12, operator side-by-side verdict PASS — '
                                                        'checkpoint 20). Full lifecycle IDENTIFY → MAP → '
                                                        'DERIVE → ARGUE → VERIFY → INSTANTIATE ran '
                                                        '2026-06-11/12. Phase B (policy-grade G(s, π)) and '
                                                        'the typed-grounds tail moved to Excursion ownership '
                                                        'on 2026-07-06; related fold-frontier work may '
                                                        'inform M-fold-ansatz, but M-first-flights no longer '
                                                        'carries that work as an open WM target.',
                                               'reason': 'Explicit SUPERSEDED-AS-MISSION: residual '
                                                         'policy-G/typed-grounds obligations transferred to '
                                                         'named E-first-flights-transferred-work, so mission '
                                                         'is retired as live WM target.',
                                               'sha256': '08db73e22e03891fe8fe7d6c7e537a340a95e517f6c951b5105807d28c9a2c12'},
 'futon3c/holes/missions/M-forum-refactor.md': {'group': 'closed-unwitnessed',
                                                'line': 246,
                                                'quote': '- [ ] Shape-validated: all inputs/outputs checked '
                                                         'against evidence shapes',
                                                'reason': 'DONE and component source pointers, but Parts '
                                                          'II-IV acceptance criteria remain unchecked; no '
                                                          'all-criteria closure mapping.',
                                                'sha256': '1fb5c002b17b5dafd1c17c3182f354789841b3d73c62a624209c36fece7cd6ae'},
 'futon3c/holes/missions/M-futon3-last-mile.md': {'group': 'undetermined',
                                                  'line': 2,
                                                  'quote': 'Status: archived',
                                                  'reason': 'Undated archive stamp conflicts with explicit '
                                                            'IDENTIFY/active/in-progress status; no '
                                                            'retirement reason or dated supersession '
                                                            'resolves precedence.',
                                                  'sha256': 'f52ee1b302910b5ccca69864edb8c78cca458f3d0c440fb994a0da43e18af822'},
 'futon3c/holes/missions/M-futon3c-codex.md': {'group': 'undetermined',
                                               'line': 2,
                                               'quote': 'Status: archived',
                                               'reason': 'Undated archive stamp conflicts with explicit '
                                                         'IDENTIFY/active/in-progress status; no retirement '
                                                         'reason or dated supersession resolves precedence.',
                                               'sha256': '4db10fb94834a8e81a70c60f069f8cf3597b44646932accafd283660652409c7'},
 'futon3c/holes/missions/M-improve-irc.md': {'group': 'closed-witnessed',
                                             'line': 29,
                                             'quote': '### P1: irc-read! endpoint — DONE',
                                             'reason': 'All four scoped improvements P1-P4 explicitly DONE '
                                                       'with http.clj, ngircd_bridge.py and commit 62856c1 '
                                                       'pointers.',
                                             'sha256': '4585f376a9ee748bb495c2fa5ec6c56d17724cba270cd5a05c157126c1e71592'},
 'futon3c/holes/missions/M-kangaroo.md': {'group': 'open',
                                          'line': 4,
                                          'quote': 'Status: INSTANTIATE v1 LANDED on master + LIVE-VALIDATED '
                                                   '2026-06-10 (agent_pouch.clj, gate in '
                                                   'make-claude-invoke-fn, flag FUTON3C_KANGAROO default '
                                                   'OFF). VERIFY ✅ — live real-claude `feed-turn!` ×2: turn1 '
                                                   'spawn 7.6s → "ONE", turn2 warm 3.4s → "TWO" (≈2.2× '
                                                   'faster even fresh; gap scales with session size = the '
                                                   '5.8MB win). ACTIVATION: next restart (closure-capture, '
                                                   'like Car-3) + cr-new agents + flag ON. Reviewed: OFF '
                                                   'byte-for-byte cold; ON→cold-fallback on any Throwable; '
                                                   'feed-turn! evicts pouch on failure (no desync).',
                                          'reason': 'INSTANTIATE v1 landed but activation awaits '
                                                    'restart/flag ON; mission acceptance checklist remains '
                                                    'open.',
                                          'sha256': '38dd5d1c95c845c38bcdf3b674618d11ed3c8d62e32f06d6403bede66fe4b487'},
 'futon3c/holes/missions/M-mission-control.md': {'group': 'closed-witnessed',
                                                 'line': 1145,
                                                 'quote': '## Closure — 2026-02-26',
                                                 'reason': 'Closure enumerates seven delivered capabilities '
                                                           'with code/tool pointers and explicitly disposes '
                                                           'all six weaknesses; inference is assigned to '
                                                           'successor M-portfolio-inference.',
                                                 'sha256': '9b130b4fc72651990c70cdc863151b23760f867b5e3275093d041b390ce79789'},
 'futon3c/holes/missions/M-mission-peripheral.md': {'group': 'closed-unwitnessed',
                                                    'line': 2,
                                                    'quote': 'Status: archived',
                                                    'reason': 'Archived status records no retirement reason; '
                                                              'inspected scope/criteria have no explicit '
                                                              'complete discharge with a witness. Archive '
                                                              'stamp alone is insufficient; any older status '
                                                              'is retained in CSV.',
                                                    'sha256': 'e2bb9133decd523620385cccfcfdc48d8cc3eeedee403a0eb2e8acfe701f4492'},
 'futon3c/holes/missions/M-operational-readiness-traceability.md': {'group': 'closed-unwitnessed',
                                                                    'line': 2,
                                                                    'quote': 'Status: archived',
                                                                    'reason': 'Archived status records no '
                                                                              'retirement reason; inspected '
                                                                              'scope/criteria have no '
                                                                              'explicit complete discharge '
                                                                              'with a witness. Archive stamp '
                                                                              'alone is insufficient; any '
                                                                              'older status is retained in '
                                                                              'CSV.',
                                                                    'sha256': '7ad4383aecb8dcc28288c431d2d018fe83d179c2833c8dcb5e01e6068079ac64'},
 'futon3c/holes/missions/M-operational-readiness.md': {'group': 'closed-witnessed',
                                                       'line': 35,
                                                       'quote': '### Issues (R11 Handoffs)',
                                                       'reason': 'Eight issue rows all done with '
                                                                 'commit/receipt pointers; four gates marked '
                                                                 'met and checkpoint evidence records API, '
                                                                 'tests, evidence and unattended loop.',
                                                       'sha256': 'ea4395fc4db3eff63a14f00601b2dbe84bc00048cd9daafe37c68abe5d6cf888'},
 'futon3c/holes/missions/M-peripheral-behavior.md': {'group': 'closed-unwitnessed',
                                                     'line': 2,
                                                     'quote': 'Status: archived',
                                                     'reason': 'Archived status records no retirement '
                                                               'reason; inspected scope/criteria have no '
                                                               'explicit complete discharge with a witness. '
                                                               'Archive stamp alone is insufficient; any '
                                                               'older status is retained in CSV.',
                                                     'sha256': 'b5cd5f9e516777e01f8bf620e6538cfefcf65be0b37dc431a90b40e874629bcf'},
 'futon3c/holes/missions/M-peripheral-gauntlet.md': {'group': 'closed-unwitnessed',
                                                     'line': 2,
                                                     'quote': 'Status: archived',
                                                     'reason': 'Archived status records no retirement '
                                                               'reason; inspected scope/criteria have no '
                                                               'explicit complete discharge with a witness. '
                                                               'Archive stamp alone is insufficient; any '
                                                               'older status is retained in CSV.',
                                                     'sha256': '6669f1a6a2e0d54a51f6eba91650585ce2b47756a4fa91b52fb7baee4d7a865d'},
 'futon3c/holes/missions/M-peripheral-model.md': {'group': 'closed-witnessed',
                                                  'line': 112,
                                                  'quote': '- [x] R10 (mode-gate): coordination and action '
                                                           'are distinguishable',
                                                  'reason': 'All 18 staged criteria checked: mode shapes, '
                                                            'five peripherals, hop preservation and '
                                                            'integration; resources/peripherals.edn and '
                                                            'social/peripheral.clj are concrete artifacts.',
                                                  'sha256': '40cbb32ab65c62761f97e6c7749406d4000e7a93bf2d6d968d896050bac68ed0'},
 'futon3c/holes/missions/M-peripheral-phenomenology.md': {'group': 'closed-unwitnessed',
                                                          'line': 2,
                                                          'quote': 'Status: archived',
                                                          'reason': 'Archived status records no retirement '
                                                                    'reason; inspected scope/criteria have '
                                                                    'no explicit complete discharge with a '
                                                                    'witness. Archive stamp alone is '
                                                                    'insufficient; any older status is '
                                                                    'retained in CSV.',
                                                          'sha256': '6b1ece0f3169ee0c11aeaa3e4f043caf2457c3e1e2fe14ea156110269956374c'},
 'futon3c/holes/missions/M-pilot-appearance.md': {'group': 'closed-unwitnessed',
                                                  'line': 60,
                                                  'quote': 'Timebox: 3 weeks soft. Scope: the *scaffolding '
                                                           'for pilot-appearance instrumentation*, not its '
                                                           'completion. The exit criterion is *a working '
                                                           'storyboard with at least four scenes (one per '
                                                           'world), each rendered in at least two of the '
                                                           'four modes, with the appearance-at-each-depth '
                                                           'visible across the scenes as a coherent '
                                                           'characterization of the pilot*.',
                                                  'reason': 'Operator-ratified close has four-depth '
                                                            'operational evidence, but original exit '
                                                            'requires four storyboard scenes in two modes; '
                                                            'closing checkpoint says that prototype stayed a '
                                                            'skeleton, without explicit retirement of that '
                                                            'exit criterion.',
                                                  'sha256': '042a4ed4d9467d3a6e7c33e35eb5707bd2496811bbc5a6c8efe374c6178ef9a3'},
 'futon3c/holes/missions/M-populate-substrate-2.D3-sweep-runbook.md': {'group': 'closed-witnessed',
                                                                       'line': 218,
                                                                       'quote': '**Per-repo results** '
                                                                                '(commits / files / '
                                                                                'skipped-nonclj / retracted '
                                                                                '/ cursor==HEAD / '
                                                                                'time-travel vars '
                                                                                'early→now):',
                                                                       'reason': 'Bounded runbook sweep '
                                                                                 'reports all 14 cursor '
                                                                                 'checks met with per-repo '
                                                                                 'table, persisted cursor '
                                                                                 'ad7c824 and replay.clj '
                                                                                 'checks; historical Python '
                                                                                 'limitation explicitly '
                                                                                 'scoped.',
                                                                       'sha256': '47a7926d509f03df432855e73973dd8894e1c0c3df83e4be32d11e90a3963bf5'},
 'futon3c/holes/missions/M-portfolio-inference.md': {'group': 'closed-unwitnessed',
                                                     'line': 1054,
                                                     'quote': '- [ ] First live weekly heartbeat cycle (bid '
                                                              'Monday → clear Sunday → prediction error)',
                                                     'reason': 'DONE (TESTING), but first live weekly '
                                                               'heartbeat is an explicitly unchecked exit '
                                                               'condition, awaiting a real cycle.',
                                                     'sha256': 'd857265628652955943aafaac34db1805522e6ec2504b808a4baf763164327b4'},
 'futon3c/holes/missions/M-proof-peripheral.md': {'group': 'undetermined',
                                                  'line': 2,
                                                  'quote': 'Status: archived',
                                                  'reason': 'Undated archive stamp conflicts with explicit '
                                                            'IDENTIFY/active/in-progress status; no '
                                                            'retirement reason or dated supersession '
                                                            'resolves precedence.',
                                                  'sha256': 'd7830977d220daa2320c9b2ba5d2c70051875bd55098ccffc5ebd346b0169691'},
 'futon3c/holes/missions/M-psr-pur-mesh-peripheral.md': {'group': 'closed-unwitnessed',
                                                         'line': 2,
                                                         'quote': 'Status: archived',
                                                         'reason': 'Archived status records no retirement '
                                                                   'reason; inspected scope/criteria have no '
                                                                   'explicit complete discharge with a '
                                                                   'witness. Archive stamp alone is '
                                                                   'insufficient; any older status is '
                                                                   'retained in CSV.',
                                                         'sha256': '0703e27ef4823069a173434a2f28bed2fe64bc3780622c3e61bbbcced080619e'},
 'futon3c/holes/missions/M-reachable-from-boot.md': {'group': 'open',
                                                     'line': 145,
                                                     'quote': 'When Codex closes #65, the line is *cleared*: '
                                                              'the foundation invariant',
                                                     'reason': 'Only evidence-store sibling is complete; '
                                                               'three remaining siblings are explicitly work '
                                                               'in progress under issue 65.',
                                                     'sha256': '45d77302e540acee80c3879e222ea4ecf6c7c9905fdd882590442767c9c8c5fb'},
 'futon3c/holes/missions/M-single-locus.md': {'group': 'open',
                                              'line': 1,
                                              'quote': '**Status:** INSTANTIATE complete (2026-04-29) for '
                                                       'the mission-home slice; siblings handed to Codex via '
                                                       'GitHub issue #64.',
                                              'reason': 'Only mission-home slice INSTANTIATE is complete; '
                                                        'remaining sibling implementations handed to issue '
                                                        '64.',
                                              'sha256': '1b21422758ad6146451a0d73c51438acac80b2581c856929c0034670f7f5e947'},
 'futon3c/holes/missions/M-social-exotype.md': {'group': 'closed-unwitnessed',
                                                'line': 2,
                                                'quote': 'Status: archived',
                                                'reason': 'Archived status records no retirement reason; '
                                                          'inspected scope/criteria have no explicit '
                                                          'complete discharge with a witness. Archive stamp '
                                                          'alone is insufficient; any older status is '
                                                          'retained in CSV.',
                                                'sha256': '4bd465e66b1d0abb0e59dc026c697b4b4fe89c2d1fb8fb4ad47e205dbcfb7ffa'},
 'futon3c/holes/missions/M-stepper-calibration.md': {'group': 'undetermined',
                                                     'line': 2,
                                                     'quote': 'Status: archived',
                                                     'reason': 'Undated archive stamp conflicts with '
                                                               'explicit IDENTIFY/active/in-progress status; '
                                                               'no retirement reason or dated supersession '
                                                               'resolves precedence.',
                                                     'sha256': '41b291c29a7ba72369211ccc020c6af72b98257addc892b68499c3e0caebdcad'},
 'futon3c/holes/missions/M-substrate-metric.OR-sample.md': {'group': 'closed-witnessed',
                                                            'line': 11,
                                                            'quote': '```bash',
                                                            'reason': 'Bounded sample report supplies its '
                                                                      'exact script command, eight computed '
                                                                      'edge results and verdict; next '
                                                                      "scaling step is outside this sample's "
                                                                      'stated scope.',
                                                            'sha256': '2885f380b33ce1203e359f9520f33dbf78661cb3a19256d9e3f64cead5d0f6a2'},
 'futon3c/holes/missions/M-substrate-metric.R1-report.md': {'group': 'closed-witnessed',
                                                            'line': 89,
                                                            'quote': '## R1 Verdict',
                                                            'reason': 'Bounded R1 extraction report supplies '
                                                                      'graph/count/component/bridge results '
                                                                      'requested for R1, with command and '
                                                                      'output file pointers; curvature '
                                                                      'explicitly excluded.',
                                                            'sha256': '3af4e4efc1f38f7d6ed6368ccbcbb070a999d900b06809e01bd7b3bbfb85a395'},
 'futon3c/holes/missions/M-substrate-metric.R2-curvature-full-report.md': {'group': 'closed-witnessed',
                                                                           'line': 11,
                                                                           'quote': 'python3 '
                                                                                    'scripts/substrate_metric_e1_curvature.py '
                                                                                    '--edge-cap 2000 --top '
                                                                                    '25 > '
                                                                                    'holes/missions/M-substrate-metric.R2-curvature-full.json',
                                                                           'reason': 'Full-current E1 report '
                                                                                     'supplies output JSON '
                                                                                     'command, all 577 '
                                                                                     'bridge computations '
                                                                                     'and final verdict, '
                                                                                     'satisfying the bounded '
                                                                                     'report scope.',
                                                                           'sha256': 'c3e06db316467e58e62811f9a42d81a2bdefb8e2876342a549cb236ae63d8852'},
 'futon3c/holes/missions/M-substrate-metric.R2-curvature-report.md': {'group': 'closed-witnessed',
                                                                      'line': 11,
                                                                      'quote': '```bash',
                                                                      'reason': '200-edge sample report '
                                                                                'records script/output JSON, '
                                                                                'timing and final R2 verdict '
                                                                                'for the stated bounded '
                                                                                'sample.',
                                                                      'sha256': 'b080527b8ba5b539b79539a45df0812aedb7daeb78fb012e97d22e8338006dc2'},
 'futon3c/holes/missions/M-substrate-metric.md': {'group': 'closed-witnessed',
                                                  'line': 657,
                                                  'quote': '### 12.1 Delivered obligations',
                                                  'reason': 'All O1-O4 obligations explicitly delivered for '
                                                            'ratified v1 contract, with code-emb.npy and '
                                                            'metric-matrix-v0.json evidence; campaign '
                                                            'dissolution is a different scope.',
                                                  'sha256': '81b5adaf198c067ce9cd3b6e8a91efcc9cdf378fda72085cb2729e78fe65a095'},
 'futon3c/holes/missions/M-tickle-overnight.md': {'group': 'closed-unwitnessed',
                                                  'line': 93,
                                                  'quote': '- [ ] Run first overnight batch (10 entries) to '
                                                           'validate end-to-end',
                                                  'reason': 'COMPLETE for orchestration infrastructure, but '
                                                            'end-to-end overnight validation and comparison '
                                                            'remain unchecked; no complete mission discharge '
                                                            'or retirement reason.',
                                                  'sha256': '7cce4ac997fb3a01ad0d47998d2e4a3f834a3d7beb5031cb74edb38e2a67ef32'},
 'futon3c/holes/missions/M-transport-adapters.md': {'group': 'closed-witnessed',
                                                    'line': 435,
                                                    'quote': '- [x] All 10 scenarios pass',
                                                    'reason': 'All 34 criteria marked met including all ten '
                                                              'end-to-end scenarios and clean full test '
                                                              'result; transport/http.clj and websocket.clj '
                                                              'implementations cited.',
                                                    'sha256': 'b1a8e4053e759389da7afcb6dc03d84077ac673e82c613f7728c36efb68cd91d'},
 'futon3c/holes/missions/M-typed-bells.md': {'group': 'closed-unwitnessed',
                                             'line': 543,
                                             'quote': '| **C-adopt** | **A fresh agent, from the docs alone, '
                                                      'sends a correct typed bell and sees it in ArSE** | '
                                                      '**S-expose** | **⚠ S-expose — the riskiest '
                                                      'criterion** |',
                                             'reason': 'COMPLETE through DOCUMENT but co-equal behavioural '
                                                       'C-adopt criterion remains unperformed until flag ON; '
                                                       'editing exposure docs explicitly does not satisfy '
                                                       'it.',
                                             'sha256': 'bd6daa8f2fa5bcbb98810fa5f7b5d9878a4e6adeeb2a19bed4b504da221b92cb'},
 'futon3c/holes/missions/M-typed-holes-ARGUE.md': {'group': 'closed-witnessed',
                                                   'line': 88,
                                                   'quote': '## Exit',
                                                   'reason': 'Companion ARGUE artifact explicitly satisfies '
                                                             'its own synthesis/alternatives exit; artifact '
                                                             'sections and named pattern files provide the '
                                                             'evidence, not parent mission completion.',
                                                   'sha256': '8a464df04ff97141291733ad345cbe9e21016fb46b94ea5a04ac3d97dbd50b3c'},
 'futon3c/holes/missions/M-typed-holes-DERIVE.md': {'group': 'closed-witnessed',
                                                    'line': 133,
                                                    'quote': '## Exit',
                                                    'reason': 'Companion DERIVE artifact explicitly delivers '
                                                              'entity types, six invariants, data flow and '
                                                              'six decisions; its own design exit is '
                                                              'discharged in this file.',
                                                    'sha256': 'd697e78724a50a42fdb84e13275d07626cb4ff898a43b5614717285d1e2cb3ba'},
 'futon3c/holes/missions/M-typed-holes-MAP.md': {'group': 'closed-witnessed',
                                                 'line': 124,
                                                 'quote': '## Exit',
                                                 'reason': 'Companion MAP artifact explicitly answers every '
                                                           'survey question and completes ready/missing '
                                                           'table; audit and lean-manifest file pointers '
                                                           'support its bounded survey exit.',
                                                 'sha256': 'f76b4b7b8e7f4a50b7b9e916289fc3dd4af0182fd24c08ea966abc6f78111f09'},
 'futon3c/holes/missions/M-typed-holes-VERIFY.md': {'group': 'closed-witnessed',
                                                    'line': 127,
                                                    'quote': 'is certified for-all where it can be; the '
                                                             'wiring is certified where deployed and',
                                                    'reason': 'Companion VERIFY artifact provides design '
                                                              'certificate DarkTower/Coverage.lean and '
                                                              'runtime scope_query_dogfood.py with gaps '
                                                              'named as required; does not claim '
                                                              'implementation completion.',
                                                    'sha256': '20b9156df7c267eed0541a6715eae64a3c9179ecad1da8db0a624f843b20ee7c'},
 'futon3c/holes/missions/M-typed-holes-example-first-flights.md': {'group': 'closed-witnessed',
                                                                   'line': 125,
                                                                   'quote': 'The fold is now **one applied '
                                                                            'operation** `fold q db h` (not '
                                                                            'two snapshots),',
                                                                   'reason': 'Bounded worked example '
                                                                             'realised by fold_selected and '
                                                                             'negative witness '
                                                                             'fold_empty_store_leaves_hungry '
                                                                             'in FirstFlightsExample.lean; '
                                                                             'idealisation explicitly '
                                                                             'stated.',
                                                                   'sha256': 'a184f6a48952efdd7041703febe08740096264fe9d3ae8fd2e444f61aa5624d8'},
 'futon3c/holes/missions/M-typed-holes-example-mission-head.md': {'group': 'closed-witnessed',
                                                                  'line': 134,
                                                                  'quote': 'So the EDN sketch above is no '
                                                                           'longer just notation: its '
                                                                           'connectives are the',
                                                                  'reason': 'Bounded semi-formalisation '
                                                                            'mapped to checked BV/TypedHole '
                                                                            'constructors in '
                                                                            'DarkTower/Examples.lean; '
                                                                            'stronger interaction functor '
                                                                            'explicitly future work.',
                                                                  'sha256': '45ace9ed799fd83d27bc0938e721656c39cb0c93dd91aa1cedb962d75d09b5fa'},
 'futon3c/holes/missions/M-typed-holes-example-scope-query.md': {'group': 'closed-witnessed',
                                                                 'line': 13,
                                                                 'quote': '`scripts/scope_query_dogfood.py` '
                                                                          'runs the `ScopeQuery` semantics — '
                                                                          'the Lean spec',
                                                                 'reason': 'Worked-example scope is '
                                                                           'exercised with '
                                                                           'scope_query_dogfood.py, named '
                                                                           'golden graph and four explicit '
                                                                           'query/answer outputs; '
                                                                           'full-corpus extension is '
                                                                           'distinguished from this dogfood.',
                                                                 'sha256': '6cf6ee5e94776db20828681bf3bd92c8d59e5b3ab653ee39aec43b274bfae638'},
 'futon3c/holes/missions/M-typed-holes-lean-wave2-design.md': {'group': 'closed-unwitnessed',
                                                               'line': 54,
                                                               'quote': '',
                                                               'reason': 'Design note says complete but its '
                                                                         'explicit T5/T6 acceptance clauses '
                                                                         'remain prospective '
                                                                         'compile/example/lemma '
                                                                         'requirements; no discharge record '
                                                                         'for those clauses in this doc.',
                                                               'sha256': '51c81463b4c37e5b4278d81c54b7918985f3a431f55d472f2e9d6ab0cef701a4'},
 'futon3c/holes/missions/M-typed-holes.md': {'group': 'closed-witnessed',
                                             'line': 315,
                                             'quote': '## CLOSED (2026-06-15, Joe)',
                                             'reason': 'Close-out records single fill plus all six adapters, '
                                                       '19 witnessed fills, design/wiring certificates, and '
                                                       'DOCUMENT artifact; commits f7ca75b, 12fa355, '
                                                       'a573b54.',
                                             'sha256': '92907b9303b1eb2253c8819ad203f4e972e75c691803a86b6c7ef9b32af76bc8'},
 'futon3c/holes/missions/M-typed-memories.md': {'group': 'open',
                                                'line': 4,
                                                'quote': 'Status: **DERIVE** (chartered at IDENTIFY and MAP '
                                                         'completed 2026-07-22 —',
                                                'reason': 'DERIVE current; completed IDENTIFY and MAP are '
                                                          'earlier phase work.',
                                                'sha256': '088fe46b4ecac0172c1a8dc5059de5223c1af712e0b0168336cf2a9836d5f5a2'},
 'futon3c/holes/missions/M-walkie-talkie.md': {'group': 'closed-unwitnessed',
                                               'line': 3,
                                               'quote': '## Status: DONE (2026-03-08) — Gates A+D pass, B+C '
                                                        'deferred',
                                               'reason': 'DONE with Gates A+D pass and B+C deferred; no '
                                                         'witness that all four own acceptance gates are '
                                                         'discharged.',
                                               'sha256': 'fdf2c10ab2a5a3e78f522d8947818effcd99e07591b8460fa2b06cc8edcaf714'},
 'futon3c/holes/missions/M-war-machine-first-outing-expectations.md': {'group': 'closed-unwitnessed',
                                                                       'line': 3,
                                                                       'quote': 'Status: complete',
                                                                       'reason': 'Completed retrospective '
                                                                                 'narrative, but no explicit '
                                                                                 'closure criteria/discharge '
                                                                                 'ledger or source '
                                                                                 'commit/receipt binding the '
                                                                                 'scored run; not counted '
                                                                                 'closed merely because a '
                                                                                 'report exists.',
                                                                       'sha256': '47f7747d1f648cbfc216c9350fd77fd2f5eee1addfc46f353335671069389956'},
 'futon3c/holes/missions/M-war-machine-first-outing.md': {'group': 'closed-witnessed',
                                                          'line': 1019,
                                                          'quote': '**Milestones M0–M4:**',
                                                          'reason': 'Closing checkpoint explicitly marks '
                                                                    'M0-M4 met, with ledger/cycle and '
                                                                    'M-war-machine-first-outing-expectations.md '
                                                                    'evidence; bootstrap follow-on '
                                                                    'explicitly separated from demonstrated '
                                                                    'outing.',
                                                          'sha256': 'b43caf757145d76361bb40818cc31b06889487cd4be809938ff0fded49e05361'},
 'futon3c/holes/missions/M-war-machine-pilot.md': {'group': 'open',
                                                   'line': 1526,
                                                   'quote': '### Operator acceptance bell (the v0 close)',
                                                   'reason': 'Operator acceptance bell is explicitly still '
                                                             'required; operational v0 completion is not '
                                                             'formal mission closure.',
                                                   'sha256': '10bf3ae3fd8f9263ee048125ba38535fd3a20e00e81b25647ac61ca504ef4509'},
 'futon4/holes/excursions/E-arxana-workflow-management.md': {'group': 'open',
                                                             'line': 57,
                                                             'quote': '**Queued, unstarted.** Raised so the '
                                                                      'pattern-library reorg',
                                                             'reason': 'Status section explicitly says '
                                                                       'queued, unstarted; no mission '
                                                                       'closure.',
                                                             'sha256': 'ffb79aa6a17ff35e25864161f0fef03e4bf40ffcdcb28a2fdada3736371259bc'},
 'futon4/holes/missions/M-essay-corpus-substrate.md': {'group': 'open',
                                                       'line': 4,
                                                       'quote': '**Status:** Stage 1 partial-complete. D1 '
                                                                '(home convention) + D3 (first-essay '
                                                                'migration) landed; D2 (the essay projector '
                                                                'in futon3c) is the outstanding work and is '
                                                                'the scope of the Codex handoff below.',
                                                       'reason': 'Stage 1 partial-complete; D2 projector '
                                                                 'explicitly outstanding.',
                                                       'sha256': '4f043a75c037c848b48d2368d9a2bfdcc7fbb2a790f5c2519e1ccaf3ac376fa5'},
 'futon4/holes/missions/M-evidence-viewer-refinements.md': {'group': 'closed-unwitnessed',
                                                            'line': 39,
                                                            'quote': '## Success Criteria',
                                                            'reason': 'Complete header says EV-0 through '
                                                                      'EV-6 done, but success criteria and '
                                                                      'required before/after PUR plus '
                                                                      'operator confirmation lack an '
                                                                      'explicit discharge record or delivery '
                                                                      'pointer.',
                                                            'sha256': 'a600bd6779f1a83ca71453b08ad9eb54890335f599e6d33263ec9af90b659a5c'},
 'futon4/holes/missions/M-interest-network-coupling.aif-wiring.md': {'group': 'closed-witnessed',
                                                                     'line': 169,
                                                                     'quote': 'Mechanism (fully traceable): '
                                                                              'ΔG-total = −0.3870 = −0.135 '
                                                                              '(`:intrinsic-value` → G-risk)',
                                                                     'reason': 'Bounded companion wiring '
                                                                               'artifact contains measured '
                                                                               'attributable Delta G and '
                                                                               'ranking flip from live '
                                                                               'coupling; parent DOCUMENT '
                                                                               'and this artifact provide '
                                                                               'design-to-live evidence.',
                                                                     'sha256': '2f1b5945777c80927691043117d9a979f9fe7b8df54bb70c245c405ed0891f64'},
 'futon4/holes/missions/M-interest-network-coupling.md': {'group': 'closed-witnessed',
                                                          'line': 740,
                                                          'quote': '### 6.1 Completion criteria — status',
                                                          'reason': 'All four criteria explicitly MET live '
                                                                    'with CP-close delta batch, projection '
                                                                    'and WebArxana pointers; VC1-7 table '
                                                                    'also passes.',
                                                          'sha256': '4f113fec446c3d595c96ab903733c1d1be1aa18e9d9018a1eeeafc12638547df'},
 'futon4/holes/missions/M-or-training-as-learning-system.md': {'group': 'open',
                                                               'line': 1,
                                                               'quote': '**Status:** IDENTIFY (2026-05-06, '
                                                                        'draft 2; draft 1 archived as '
                                                                        '`M-or-training-as-learning-system.v1.md`)',
                                                               'reason': 'IDENTIFY draft 2 is active; '
                                                                         'archived refers to draft 1, not '
                                                                         'this document (false positive in '
                                                                         'original regex).',
                                                               'sha256': '6d89c9c4c8f48500edee4513e22281b4a57dc3874e64dff8d418d89cd61f17b5'},
 'futon4/holes/missions/M-peeragogy-rewrite.md': {'group': 'open',
                                                  'line': 4,
                                                  'quote': '',
                                                  'reason': 'IDENTIFY-redux; prior implementation suspended '
                                                            'pending re-identification.',
                                                  'sha256': '0c2ca031ffea20da1b59bc8685c1885eb4e5a26e8e0831e29c2cc3fe994d1295'},
 'futon4/holes/missions/M-self-representing-stack.md': {'group': 'closed-witnessed',
                                                        'line': 1211,
                                                        'quote': '## Closure: Gaps Resolved by '
                                                                 'M-three-column-stack (2026-03-04)',
                                                        'reason': 'All seven criteria marked met, then all '
                                                                  'three reopened gaps explicitly resolved '
                                                                  'by M-three-column-stack; '
                                                                  'ingest-three-columns.py and Arxana files '
                                                                  'are cited.',
                                                        'sha256': '7c992576165826be98457560478d4429288d60b42b68fc22b845cbc67209aa04'},
 'futon4/holes/missions/M-three-column-stack.md': {'group': 'closed-unwitnessed',
                                                   'line': 1283,
                                                   'quote': '## Completion Criteria',
                                                   'reason': 'COMPLETE with extensive phase checklists and '
                                                             'artifacts, but eight numbered final completion '
                                                             'criteria have no explicit all-met check; phase '
                                                             'checklist alone is not sufficient.',
                                                   'sha256': 'a98496e185a8b12f992b423e771070c24cdccc8efce037786f745bbcd7cedd20'},
 'futon4/holes/missions/M-vsatarcs-invariants-integration.md': {'group': 'open',
                                                                'line': 4,
                                                                'quote': '**Status:** INSTANTIATE (read-only '
                                                                         'INSTANTIATE-0 active; IDENTIFY '
                                                                         'opened 2026-06-01, MAP completed '
                                                                         '2026-06-01, DERIVE accepted '
                                                                         '2026-06-01, ARGUE accepted '
                                                                         '2026-06-01, VERIFY accepted '
                                                                         '2026-06-01 after count precision '
                                                                         'pass)',
                                                                'reason': 'Read-only INSTANTIATE-0 active; '
                                                                          'phase ratifications are not '
                                                                          'closure.',
                                                                'sha256': '4b5d609e35303cd779e117429223e85389c4741f892595efea9630303c5a4aa1'},
 'futon4/holes/missions/M-web-arxana-missions.md': {'group': 'open',
                                                    'line': 4,
                                                    'quote': '**Status:** IDENTIFY + MAP complete '
                                                             '2026-06-07. **Layer 1 LIVE** — `/ego`',
                                                    'reason': 'IDENTIFY and MAP complete; live layer 1 does '
                                                              'not state whole-mission completion.',
                                                    'sha256': '0676fbefedebb5a2500bcc1590ade1945ca58d2d15aefa5ce3962d0ba8371d31'},
 'futon4/holes/missions/M-web-arxana-ui-improvements.md': {'group': 'open',
                                                           'line': 4,
                                                           'quote': '**Status:** HEAD complete; IDENTIFY '
                                                                    'complete 2026-05-30; MAP pass complete',
                                                           'reason': 'HEAD/IDENTIFY/MAP complete only, not '
                                                                     'terminal lifecycle closure.',
                                                           'sha256': 'aa6af71d55500c9f69a594a2446b8a530e2ea3588049b8b62dab6716a7065709'},
 'futon4/holes/missions/M-webarxana.md': {'group': 'open',
                                          'line': 4,
                                          'quote': '**Status:** INSTANTIATE (2026-04-12). Interactive '
                                                   'testing pass done.',
                                          'reason': 'INSTANTIATE with an interactive testing pass done; no '
                                                    'whole-mission closure.',
                                          'sha256': 'ab61c94ebcbbbdce4e2cf58c45849fbf3c4a59b95991ae4df4484501bf636c35'},
 'futon4/holes/missions/M-writing-ethics.md': {'group': 'closed-witnessed',
                                               'line': 1034,
                                               'quote': '### Mission exit',
                                               'reason': 'Explicit mission exit says all IDENTIFY criteria '
                                                         'concretely demonstrated; criterion-8 runbook '
                                                         'completed in DOCUMENT, with draft v7 and '
                                                         'annotations.el artifact pointers.',
                                               'sha256': '50b8ffb2e96a22489b0d5ef5a4c6c78e49b8722425b165c3290419b54e593917'},
 'futon5/holes/missions/M-diagram-composition.md': {'group': 'closed-witnessed',
                                                    'line': 10,
                                                    'quote': 'constraint inputs. The futon3 three-timescale '
                                                             'stack (social/task/glacial)',
                                                    'reason': 'Declared multi-diagram validation delivered; '
                                                              'all eight checks pass standalone and '
                                                              'composed, with src/futon5/ct/mission.clj and '
                                                              'data/missions inputs named.',
                                                    'sha256': 'bdbc993d67459b3f33642826e6e89c9b9803e46aa331df9ef2b99e520ba5d519'},
 'futon5/holes/missions/M-pattern-exotype-bridge.md': {'group': 'closed-unwitnessed',
                                                       'line': 9,
                                                       'quote': 'Embed 791 patterns from the futon3 library '
                                                                'into 8-bit exotype space using',
                                                       'reason': 'Complete banner plus measured prototype '
                                                                 'and file list; broad obligation that all '
                                                                 '791 patterns be executable in any domain '
                                                                 'is not marked discharged.',
                                                       'sha256': 'd4a0baa343969e34d95427b1c2b6fff0b7d6fb3c84ee8113e2a5c36c74bb4c3b'},
 'futon5/holes/missions/M-propagators.md': {'group': 'open',
                                            'line': 4,
                                            'quote': 'reconstruction. Owner: claude-3 '
                                                     '(orchestration/review). Three lanes dispatched.',
                                            'reason': 'IDENTIFY complete, ARGUE explicitly open.',
                                            'sha256': '05d9b6a711a82f4dbcb67a4833ddac5a29b419ec20f4046fb4f788fef49f609c'},
 'futon5/holes/missions/M-sci-detection-pipeline.md': {'group': 'closed-unwitnessed',
                                                       'line': 9,
                                                       'quote': '8-component automated Wolfram class '
                                                                'detector for MMCA runs. Classifies',
                                                       'reason': 'Complete banner, component summary and '
                                                                 'accuracy result; no explicit '
                                                                 'all-obligations-met record or retirement '
                                                                 'reason.',
                                                       'sha256': '097d8d6498fd0127f596f3cdefb7fdc090c968643f375eeea33eb4a378c5c4d8'},
 'futon5/holes/missions/M-sci-reproduction.md': {'group': 'open',
                                                 'line': 3,
                                                 'quote': '- **Status:** INSTANTIATE — slices 1–3 done, '
                                                          'reviewed, published to',
                                                 'reason': 'INSTANTIATE slices 1–3 done; no whole-mission '
                                                           'terminal statement.',
                                                 'sha256': 'a52b35664d89f570cd2cafe46433839094a62b3024eaf395a0acc76893f94fd5'},
 'futon5/holes/missions/M-tpg-coupling-evolution.md': {'group': 'open',
                                                       'line': 4,
                                                       'quote': '**Status:** MAP (production run complete, '
                                                                'diversity-coupling tradeoff identified)',
                                                       'reason': 'MAP with completed production run; next '
                                                                 'evolution run is still described at 19–21.',
                                                       'sha256': 'ecdf0f55b21e1e2f8279cc936a584fe47fd67bed75bb46b6afae6348e182cf03'},
 'futon5a/holes/missions/M-explore-aiqa.md': {'group': 'closed-witnessed',
                                              'line': 200,
                                              'quote': '**DERIVE complete** (strawmen are the deliverable; '
                                                       "actual EoIs are Joe's later eoi-engine runs = his "
                                                       "INSTANTIATE, out of this mission's scope).",
                                              'reason': 'Own bounded deliverables are strawmen and two '
                                                        'reusable pattern files, all documented; actual EoI '
                                                        'writing explicitly out of this mission scope.',
                                              'sha256': '310741866089639b345589bde3bbabb1364e36213e073819fc6603d452bd6eff'},
 'futon5a/holes/missions/M-expressions-of-interest.md': {'group': 'open',
                                                         'line': 4,
                                                         'quote': '**Status:** MAP / DERIVE / ARGUE / VERIFY '
                                                                  'all complete 2026-05-11. INSTANTIATE *in '
                                                                  'progress* (encounter-first reordering): '
                                                                  'Anthropic EoI revised-draft landed '
                                                                  '2026-05-13 as proof-of-concept that the '
                                                                  'full eoi-engine + Arxana-Essays loop '
                                                                  'works; checkpoint recorded in §6. Step '
                                                                  '(a) send-decision still pending; steps '
                                                                  '(b)/(c)/(d) queued; step (e) — '
                                                                  'EoI-network-as-interest-model with '
                                                                  '**completeness-not-closure** as the '
                                                                  'property the corpus should have — added '
                                                                  '2026-05-13. Schema artefact at '
                                                                  '`/home/joe/code/atthangika-buckets.json`.',
                                                         'reason': 'Earlier phases complete but INSTANTIATE '
                                                                   'explicitly in progress.',
                                                         'sha256': '01247c81a553bcba9e4b7bab9d4083c4a8710b918afd01d35486085d7f9710af'},
 'futon6/holes/E-patch-agent-evidence-leaks.md': {'group': 'open',
                                                  'line': 4,
                                                  'quote': '**Status:** DERIVE-1 DONE (the shared classifier '
                                                           'is built and both miners route through it; the',
                                                  'reason': 'Only DERIVE-1 done; no whole-excursion close.',
                                                  'sha256': 'f4cbf653f4087171801c43b99b162e97cf654a0e61878e812756deb8abbb99df'},
 'futon6/holes/missions/M-P3-rational-reconstruction.md': {'group': 'closed-witnessed',
                                                           'line': 793,
                                                           'quote': '### Completion criteria check',
                                                           'reason': 'All five mission criteria checked, '
                                                                     'seven vPSR/vPUR pairs and three named '
                                                                     'pattern files; '
                                                                     'problem3-provenance.json recorded in '
                                                                     'final artifacts.',
                                                           'sha256': 'a3dfd9b1f7b2295a18f18a63502d9811cf314ab660aebc461ab382dafc6c3ead'},
 'futon6/holes/missions/M-P7-rational-reconstruction.md': {'group': 'undetermined',
                                                           'line': 609,
                                                           'quote': '**Status: VERIFY complete, INSTANTIATE '
                                                                    'partial (provenance JSON pending).**',
                                                           'reason': 'Header says COMPLETE while final '
                                                                     'status says INSTANTIATE partial; '
                                                                     'seventh criterion (provenance JSON) '
                                                                     'explicitly TODO. Conflicting '
                                                                     'mission/phase statuses, no retirement.',
                                                           'sha256': '91acc66fac2cb18976ab5addbf7fcfa79e39368163c5183c40df68af304ddfe9'},
 'futon6/holes/missions/M-P8-rational-reconstruction.md': {'group': 'undetermined',
                                                           'line': 738,
                                                           'quote': '**Status: VERIFY complete, INSTANTIATE '
                                                                    'partial (pattern commit + provenance '
                                                                    'JSON pending).**',
                                                           'reason': 'Header says COMPLETE while final '
                                                                     'status says INSTANTIATE partial; '
                                                                     'provenance JSON and pattern commit '
                                                                     'pending. Conflicting statuses, no '
                                                                     'retirement.',
                                                           'sha256': 'd1bd74e1cf125ccbce96422c79d15d68e9ed7cc9cf8194c81fdbab3e3ea3447a'},
 'futon6/holes/missions/M-bayesian-structure-learning.md': {'group': 'closed-witnessed',
                                                            'line': 4,
                                                            'quote': '**Status:** SUPERSEDED-AS-MISSION → '
                                                                     'recast toward '
                                                                     'Campaign-bayesian-structure-learning '
                                                                     '(Joe 2026-06-08; see §7b). '
                                                                     'Idea-of-record; NOT WM-pickable (a '
                                                                     'Campaign across math / '
                                                                     'differentiable-code / War-Machine '
                                                                     'domains, the last = E-efe-education).',
                                                            'reason': 'Explicit SUPERSEDED-AS-MISSION: '
                                                                      'recast as cross-domain campaign '
                                                                      'because no single mission can own the '
                                                                      'transfer claim; idea retained, not '
                                                                      'WM-pickable.',
                                                            'sha256': '8dc47e68966d4bacb729c98c12a805c097b1bb77c7f7b8e724026c06acedd380'},
 'futon6/holes/missions/M-futonzero-grounding.md': {'group': 'closed-witnessed',
                                                    'line': 6,
                                                    'quote': '**Status:** FOLDED into Campaign '
                                                             '`C-falsifiable-missions` (2026-06-10, '
                                                             "futon3c/holes/campaigns/) — this umbrella's "
                                                             'diagnosis (closed loop) + grounding edges '
                                                             "E1–E5 are the campaign's charter material; the "
                                                             'three chartered constituents '
                                                             '(M-peradam-grounding / M-pattern-posteriors / '
                                                             'M-arguing-worlds) operationalize them. '
                                                             '**Retired as a standalone mission** per '
                                                             '`:O-capstone-form` → campaign. Kept as the '
                                                             'diagnosis record.',
                                                    'reason': 'Explicitly retired as standalone mission with '
                                                              'reason and successor C-falsifiable-missions; '
                                                              'constituent missions carry its grounding '
                                                              'edges.',
                                                    'sha256': 'ed47611cf78a57ed966efbf6e1512214ec499bb37d23e2c077ce3a8f238d0927'},
 'futon6/holes/missions/M-live-efe-map.md': {'group': 'open',
                                             'line': 3,
                                             'quote': '**Status: VERIFY complete (2026-07-04) — HEAD and '
                                                      'IDENTIFY ratified by',
                                             'reason': 'VERIFY phase complete only; mission not declared '
                                                       'closed by that phase completion.',
                                             'sha256': '0c05463e180281b27e476e5620425196a4b8fa30e90d05f7a6726fa310748006'},
 'futon6/holes/missions/M-metric-harness.md': {'group': 'open',
                                               'line': 3,
                                               'quote': '**Status:** HEAD complete; IDENTIFY authored; MAP '
                                                        'complete (scratch-work survey);',
                                               'reason': 'HEAD/IDENTIFY/MAP milestones only; no mission '
                                                         'close.',
                                               'sha256': 'ecc0ddf76cc25014b05950a288948df6bd2410bf26eb0d1c7f79e6ad7ee722eb'},
 'futon6/holes/missions/M-structure-seed-promotion.md': {'group': 'open',
                                                         'line': 4,
                                                         'quote': '**Status:** INSTANTIATE (sections 3.1–3.3 '
                                                                  '+ anti-clobber filter landed 2026-05-20; '
                                                                  'loss-of-loss stopping rule open)',
                                                         'reason': 'Loss-of-loss stopping rule explicitly '
                                                                   'open.',
                                                         'sha256': '4d634385b801be096bc80c559cce1a83c29cec9d87f42596d11008f35602b581'},
 'futon7/holes/M-demonstration-foundry.md': {'group': 'open',
                                             'line': 3,
                                             'quote': '**Status: ARGUE (DERIVE complete 2026-07-06; ARGUE in '
                                                      'progress).',
                                             'reason': 'ARGUE in progress, despite completed DERIVE.',
                                             'sha256': '2751fd22688dd0a60f1db725c44d6b9aa665f4c7388b8c1179b4a3561a2d71d6'},
 'futon7/holes/M-interim-director.md': {'group': 'open',
                                        'line': 228,
                                        'quote': '### 7.3 Status: `READY-TO-CLOSE`',
                                        'reason': 'READY-TO-CLOSE explicitly not CLOSED; two formal-closure '
                                                  'conditions are required, without a final joint discharge '
                                                  'record.',
                                        'sha256': '92b2d5972ef184c860dd35cacf9a08b89f4f8e102d56a55b550b1fe30ba39bda'},
 'futon7/holes/M-self-documenting-stack.md': {'group': 'closed-witnessed',
                                              'line': 1973,
                                              'quote': '**Final lifecycle stamp**: '
                                                       '**M-self-documenting-stack POC COMPLETE — usable; 5 '
                                                       'substantive tickets landed (T-9/T-9b/T-9c/T-8/T-1); '
                                                       '8 named follow-ups '
                                                       '(T-2/T-3/T-4/T-5/T-6/T-7/T-8b/T-9d) carry forward as '
                                                       "non-blocking refinements.** Mission's POC scope is "
                                                       'fully met.',
                                              'reason': 'Final lifecycle stamp explicitly supersedes '
                                                        'premature close/reopen history, states POC scope '
                                                        'fully met with five landed tickets and live '
                                                        'mission-search demonstration; remaining eight '
                                                        'tickets nonblocking by recorded scope.',
                                              'sha256': '519f224565564981d2a0780f3d20c5c7542f26be9e9ef229cba99ee3c3c580d5'},
 'futon7/holes/missions/M-f7-lead-report.md': {'group': 'closed-witnessed',
                                               'line': 55,
                                               'quote': '## PUR',
                                               'reason': 'PUR records successful implementation and '
                                                         'generated 293-repo report; three deliverables '
                                                         'named at section Deliverables (report.clj, '
                                                         'lead-report.md and outcome record).',
                                               'sha256': '2a773ebc9efb91493cd935d0b18918d1209cea2a3c68120aebd5bd847662a227'}}
FULL_READ_SAMPLE = ['futon5/holes/missions/M-diagram-composition.md',
 'futon5/holes/missions/M-pattern-exotype-bridge.md',
 'futon5/holes/missions/M-sci-detection-pipeline.md',
 'futon5/holes/missions/M-tpg-coupling-evolution.md',
 'futon3c/holes/missions/M-war-machine-first-outing-expectations.md',
 'futon3c/holes/missions/M-alfworld-pattern-discovery.md',
 'futon3c/holes/missions/M-substrate-metric.R2-curvature-full-report.md',
 'futon3/holes/missions/M-understand-fucodex.md',
 'futon3c/holes/missions/M-substrate-metric.OR-sample.md',
 'futon3c/holes/excursions/E-scope-organism-copar.md',
 'futon3c/holes/missions/M-substrate-metric.R2-curvature-report.md',
 'futon3c/holes/missions/M-typed-holes-example-scope-query.md',
 'futon3c/holes/excursions/E-first-flights-transferred-work.md',
 'futon3c/holes/excursions/E-shutdown-agents-killed-the-pools.md',
 'futon2/holes/E-evaluate-policies-spikes.md',
 'futon2/holes/M-peradam-mechanization.md',
 'futon3c/holes/missions/M-operational-readiness.md',
 'futon3c/holes/missions/M-typed-holes-lean-wave2-design.md',
 'futon3/holes/missions/M-make-agency-work-properly.md',
 'futon3/holes/missions/M-drawbridge-multi-agent.md']

FIELD = re.compile(r'(?i)(?:^\s*(?:[-*>]\s+|#{1,3}\s+)?(?:\*\*)?|\*\*)(status(?:\s*\([^)]*\))?|lifecycle(?:\s+(?:phase|status))?|phase(?:-state)?|owner(?:/reviewer)?)\s*(?:\*\*)?\s*:\s*(?:\*\*)?')
TERMINAL = re.compile(r'(?i)\b(closed|complete|completed|done|retired|superseded|folded|stopped|landed|delivered|archived|abandoned)\b')
CHECKBOX = re.compile(r'^\s*(?:[-*]|\d+[.)])\s+\[([ xX])\]')

def inventory(root):
    result = []
    for repo in REPOS:
        base = root / repo / 'holes'
        if base.exists():
            for p in base.rglob('*.md'):
                if re.match(r'^[MCE]-', p.name) and not any(
                    x.startswith('.') or 'archive' in x.lower()
                    for x in p.relative_to(base).parts[:-1]
                ) and not p.is_symlink():
                    result.append(p)
    return sorted(result)

def metadata(lines):
    fields = []
    fenced = False
    for i, line in enumerate(lines):
        if line.lstrip().startswith(('```', '~~~')):
            fenced = not fenced
        if fenced:
            continue
        if (heading:=re.match(r'(?i)^#{1,4}\s+(owner|status|lifecycle|phase)\s*$', line)):
            for j in range(i+1, min(i+6, len(lines))):
                if lines[j].strip():
                    fields.append((j+1, heading[1].lower(), lines[j].strip(), lines[j]))
                    break
        inline_owner = re.search(r'(?i)\bowner(?:-driver|s)?\s*:(?:\*\*)?\s*(.+)',line)
        if inline_owner and not any(f[0]==i+1 and f[1]=='owner' for f in fields) and not any(m[1].lower().startswith('owner') for m in FIELD.finditer(line)):
            fields.append((i+1,'owner',inline_owner[1],line))
        for m in FIELD.finditer(line):
            name = m[1].lower()
            value = line[m.end():].strip().strip('*').strip()
            fields.append((i+1, name, value, line))
    return fields

def git_history(root, repo):
    # First occurrence matches git log -1 semantics; no --follow across renames.
    proc = subprocess.run(['git', '-C', str(root/repo), 'log',
                           '--format=%x1e%H%x1f%cI', '--name-only', '--', 'holes'],
                          text=True, capture_output=True, check=True)
    result = {}
    for block in proc.stdout.split('\x1e')[1:]:
        lines = block.strip().splitlines()
        sha, stamp = lines[0].split('\x1f')
        for name in lines[1:]:
            if name.strip():
                result.setdefault(name.strip(), (sha, stamp))
    return result

def classify(path, digest, fields):
    review = REVIEWS.get(path)
    if review:
        if digest != review['sha256']:
            return 'undetermined', 'Source changed since manual review; re-review required.', 0, 'stale-review'
        return review['group'], review['reason'], review['line'], 'manual'
    candidates = [f for f in fields if f[0] <= 80 and f[1].startswith(('status','lifecycle','phase')) and 'historical' not in f[1]]
    if not candidates:
        return 'undetermined', 'No recognized document status/lifecycle field in first 80 lines; later section statuses are not promoted to mission status.', 0, 'no-header'
    line, _, value, _ = candidates[0]
    if value.startswith('|'):
        return 'undetermined', 'Status section is a table, not an unambiguous lifecycle value.', line, 'ambiguous-table'
    if TERMINAL.search(value):
        return 'undetermined', 'Closure/phase-completion wording requires a fresh criteria-and-witness inspection; regex alone cannot classify closure.', line, 'needs-review'
    return 'open', 'Nonterminal stated status/lifecycle; no reviewed whole-document closure or retirement record.', line, 'nonterminal-header'

def scan(root):
    histories = {r: git_history(root,r) for r in REPOS if (root/r/'.git').exists()}
    rows = []
    for p in inventory(root):
        relative = p.relative_to(root); repo = relative.parts[0]
        data = p.read_bytes(); digest = hashlib.sha256(data).hexdigest()
        lines = data.decode('utf-8').splitlines(); fields = metadata(lines)
        group, reason, evidence_line, method = classify(str(relative),digest,fields)
        baseline_match = re.search(r'(?i)(status|lifecycle)\W{0,6}\s*([^\n]{0,80})', data.decode('utf-8')[:1500])
        baseline_status = baseline_match[2].lower() if baseline_match else 'unknown'
        baseline_closed = bool(re.search(r'\b(complete[d]?|closed|done|retired|superseded|abandoned|archived)\b', baseline_status))
        statuses = [f for f in fields if f[0] <= 80 and f[1].startswith(('status','lifecycle','phase'))]
        owners = [f for f in fields if f[1].startswith('owner')]
        phases = [f for f in fields if f[1].startswith(('lifecycle','phase'))]
        sha, stamp = histories.get(repo,{}).get(str(p.relative_to(root/repo)), ('',''))
        age = (AS_OF-dt.datetime.fromisoformat(stamp).astimezone(dt.timezone.utc).date()).days if stamp else ''
        boxes = [m[1] for l in lines if (m:=CHECKBOX.match(l))]
        quoted = lines[evidence_line-1] if evidence_line else ''
        rows.append(dict(repo=repo,path=str(relative),id=p.stem,group=group,
            stated_status_line=' | '.join(f'L{f[0]}: {f[3]}' for f in statuses),
            lifecycle_phase=' | '.join(f'L{f[0]}: {f[2]}' for f in phases) or ' | '.join(
                f'L{f[0]} (status phase mentions): '+', '.join(re.findall(r'\b(?:HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE|DOCUMENT|MONITOR|EXPLORE)\b',f[2]))
                for f in statuses if re.search(r'\b(?:HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE|DOCUMENT|MONITOR|EXPLORE)\b',f[2])),
            owner=' | '.join(f'L{f[0]}: {f[2]}' for f in owners),
            baseline_status=baseline_status,baseline_closed='yes' if baseline_closed else 'no',
            last_git_commit_date=stamp,last_git_commit=sha,age_days=age,
            evidence_line=evidence_line or '',evidence_quote=quoted,evidence=reason,
            classification_method=method,checked_boxes=sum(x.lower()=='x' for x in boxes),
            unchecked_boxes=boxes.count(' '),source_sha256=digest,
            retirement_candidate='',retirement_reason='',duplicate_id_across_repos=''))
    # Named supersession is inspected, not inferred from any mention of a successor.
    successors = {
        'futon0/holes/missions/M-futonzero-mvp.md':
          'Named successor M-futonzero-capability: futon0/holes/missions/M-futonzero-capability.md:10 explicitly supersedes this document.',
        'futon3c/holes/excursions/E-memory-whitepaper-plan.md':
          'Planning scope Experiment 0 superseded by E-memory-whitepaper-v2-plan.md:7; partial-scope candidate only, not retirement of the full doc.',
        'futon3c/holes/excursions/E-memory-whitepaper-v2-plan.md':
          'Executable programme section 4 superseded by E-memory-whitepaper-v2-programme.md:5; findings record explicitly retained, partial-scope review only.',
    }
    ids = collections.defaultdict(list)
    for r in rows:
        ids[r['id']].append(r)
    for r in rows:
        reasons = []
        if r['group']=='open' and r['age_days']!='' and r['age_days']>=30:
            reasons.append(f"Open; last Git touch {dt.datetime.fromisoformat(r['last_git_commit_date']).astimezone(dt.timezone.utc).date()} UTC ({r['age_days']} days before census date).")
        if r['group'] in ('open','undetermined') and r['path'] in successors:
            reasons.append(successors[r['path']])
        r['retirement_candidate'] = 'yes' if reasons else 'no'
        r['retirement_reason'] = ' '.join(reasons)
        r['duplicate_id_across_repos'] = ' | '.join(x['path'] for x in ids[r['id']]) if len({x['repo'] for x in ids[r['id']]})>1 else ''
    return rows

def md(s):
    return str(s).replace('|','\\|').replace('\n',' ')

def write_outputs(rows, output, root):
    with (output/'census.csv').open('w',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]),lineterminator='\n');w.writeheader();w.writerows(rows)
    counts=collections.Counter(r['group'] for r in rows)
    candidates=[r for r in rows if r['retirement_candidate']=='yes']
    indexed={r['path']:r for r in rows}
    report=[
        '# Mission/campaign/excursion census — 2026-09-21', '',
        f"{len(rows)} documents: "+', '.join(f"**{counts[g]} {g}**" for g in GROUPS)+f". **{len(candidates)} retirement candidates**, for Joe to review; no mission was edited or retired by this census.", '',
        '## Scope and reproducibility', '',
        'Canonical repositories only: '+', '.join(f'`{r}`' for r in REPOS)+'. Recursive `holes/**/[MCE]-*.md`; filenames define the document ID (full stem, including companion suffixes). Hidden-directory components, archive-named directories, and symlink files are excluded. Worktree copies are not roots. Frozen mission copies inside canonical run/authority directories remain in scope (the repeated U88 ID below is an example); they are not treated as separate canonical repositories. Untracked files are included if present; absent Git history is explicitly blank. No stores queried.', '',
        'Run `python3 holes/labs/wm-contract/runs/mission-census-2026-09-21/census.py --root /home/joe/code --self-check` from futon2. Outputs are written beside the script. Standard library only. The cutoff for ages is the UTC calendar date 2026-09-21; Git committer timestamps are retained with their original offset and normalized to UTC dates for age arithmetic, not replaced by filesystem mtimes. Git history follows the current path, not renames. A commit touching a file is evidence of a touch, not necessarily substantive mission progress.', '',
        '## Classification method and limits', '',
        'A field parser routes the first recognized status/lifecycle/phase field in the first 80 lines (outside fenced code). CSV retains all such header fields, all explicit lifecycle/phase fields throughout the file, and explicitly labelled owners throughout the file, with source line numbers. Owners are not inferred from authors or committers. Some later owner/phase fields refer to subwork; their text and lines are retained rather than collapsed into a false single authority. Phase information embedded in a status is retained as explicitly labelled status-phase mentions; this does not choose a current phase from a phase history.', '',
        'Closure words only select documents for inspection. Every initial closure-word route and every additional archived/heading-style terminal route exposed by the original baseline was inspected against its criteria, checklists, scope and closing records. Additional whole-document closure/retirement records found in body searches were inspected separately. Nonterminal headers default to open; missing recognizable headers default to undetermined. This is not a claim that an undetermined document has no deliverable or no status anywhere in its prose.', '',
        'The inspection heuristic for “all obligations met” is: find the document’s own bounded scope and final criteria; require a nonempty all-met checklist, explicit all-criteria outcome table, or a closing record that explicitly disposes every scoped obligation, plus a named commit/receipt/file. Phase completion, passing tests alone, unchecked acceptance criteria, and a bare operator close are insufficient. Recorded scope exclusions and revised exits are honored when explicit. Retirement instead requires an explicit replacement/stand-down with its reason. No closure is inferred automatically from checkbox totals. Those totals include historical and subphase checklists and are diagnostic only.', '',
        'An undated archive stamp plus an incompatible active header is undetermined unless a later clear closure resolves it; archive without a reason otherwise routes to closed-unwitnessed. Manual decisions, evidence lines, quotations and source SHA-256 hashes are embedded in census.py. If a reviewed source changes, rerunning returns undetermined until that judgment is refreshed. Newly encountered terminal wording also returns undetermined rather than manufacturing closure. Evidence means the document records an identifiable witness; no tests, Lean proofs, live demos, store receipts or historical commits were independently re-executed. Some witness files may have moved. The classifications assess the documentation, not current runtime fitness.', '',
        'The required sample check read the **20 shortest documents by UTF-8 file size among the initial 121 closure-word routes in full**, not just regex snippets. This is a purposive, reproducible sample, not a random accuracy estimate. Other routed documents were read at their criteria, closure and relevant evidence sections. The sample found prospective criteria and phase-only completion, which were not counted as witnessed closure.', '',
        '## Counts by repository', '',
        '| Repo | closed-witnessed | closed-unwitnessed | open | undetermined | Total |',
        '|---|---:|---:|---:|---:|---:|']
    for repo in REPOS:
        c=collections.Counter(r['group'] for r in rows if r['repo']==repo)
        report.append('| '+repo+' | '+' | '.join(str(c[g]) for g in GROUPS)+f' | {sum(c.values())} |')
    report += ['| **Total** | '+' | '.join(str(counts[g]) for g in GROUPS)+f' | {len(rows)} |','',
        '## Reconciliation with 642 documents / 493 not marked closed', '',
        'The inventory and original baseline reproduce **642 / 149 regex-closed / 493 not regex-closed** exactly. Claude supplied the original rule in Agency job invoke-1790025295764-23074-30db54d1: search the first 1,500 characters for the first `(status|lifecycle)` followed by up to six nonword characters and up to 80 non-newline characters; test that value for complete/completed/closed/done/retired/superseded/abandoned/archived. census.py implements the exact regex and census.csv retains both baseline fields. This matches any text, truncates long fields, counts phase completion and negations, and treats archived as closed regardless of reason. Initial closure routing omitted archived and heading-style status fields; the baseline comparison exposed these and they were separately inspected before final classification.', '',
        f"Under the stricter rule, {counts['closed-witnessed']+counts['closed-unwitnessed']} are classified closed, of which only {counts['closed-witnessed']} have a recorded closure witness; {counts['open']} are open and {counts['undetermined']} undetermined. Thus neither 493 nor all non-witnessed documents should be called an open-mission backlog. This is a document census: a companion MAP/ARGUE report may close while its parent remains open.", '',
        '## Age of open documents', '',
        '| Days since last Git touch | Count |','|---|---:|']
    # Place a complete row-level baseline reconciliation before the age section.
    position=report.index('## Age of open documents')-1
    cross=['', '| Original regex group | closed-witnessed | closed-unwitnessed | open | undetermined | Total |',
           '|---|---:|---:|---:|---:|---:|']
    for flag,label in [('yes','Marked closed'),('no','Not marked closed')]:
        c=collections.Counter(r['group'] for r in rows if r['baseline_closed']==flag)
        cross.append('| '+label+' | '+' | '.join(str(c[g]) for g in GROUPS)+f' | {sum(c.values())} |')
    report[position:position]=cross
    bins=collections.Counter()
    for r in rows:
        if r['group']=='open':
            age=r['age_days'];b='unknown' if age=='' else '<0 (future timestamp)' if age<0 else '0–6' if age<7 else '7–29' if age<30 else '30–59' if age<60 else '60–89' if age<90 else '90–179' if age<180 else '180+'
            bins[b]+=1
    for b in ['<0 (future timestamp)','0–6','7–29','30–59','60–89','90–179','180+','unknown']:
        report.append(f'| {b} | {bins[b]} |')
    report += ['', '## Retirement candidates — proposals only', '',
        'Rule: open and untouched in Git for at least 30 days, or a specifically identified named supersession while not already closed. Partial-scope replacements are marked as such. Age alone does not establish abandonment. Closed-unwitnessed and contradictory-status documents require evidence/status repair and are not silently retired.', '',
        '| Document | Reason |','|---|---|']
    for r in candidates:
        report.append(f"| `{r['path']}` | {md(r['retirement_reason'])} |")
    duplicates={r['id']:r['duplicate_id_across_repos'] for r in rows if r['duplicate_id_across_repos']}
    report += ['', '## Duplicate IDs across repositories', '',
               f'{len(duplicates)} filename IDs occur in more than one repository. No deduplication was applied. Same-repo companion reports retain distinct full-stem IDs.', '',
               '| ID | Paths |','|---|---|']
    for key, val in sorted(duplicates.items()):
        report.append(f'| `{key}` | {md(val)} |')
    if not duplicates: report.append('| — | None found |')
    report += ['', '## Full-read sample (20)', '',
               '| Document | Classification | Evidence / decision |','|---|---|---|']
    for p in FULL_READ_SAMPLE:
        r=indexed.get(p)
        if r: report.append(f"| `{p}` | {r['group']} | L{r['evidence_line']}: {md(r['evidence'])} |")
    report += ['', '## Review queue and validation', '',
               f"{sum(r['classification_method']=='manual' for r in rows)} source-pinned manual judgments; {sum(r['classification_method']=='stale-review' for r in rows)} changed reviewed sources; {sum(r['classification_method']=='needs-review' for r in rows)} newly routed terminal headers. CSV supplies the evidence and source hash for every row.", '',
               'Self-checks enforce one row and one group per path, exact source/quotation agreement for reviewed files, terminal-language refusal without manual review, and actual-source regression cases: M-agency-unified-routing (unchecked success criteria), M-portfolio-inference (weekly heartbeat exit still open), M-IRC-stability (witnessed checklists), and M-P7-rational-reconstruction (contradictory statuses). The initial snapshot also matched every source SHA/quotation and 20 Git-log date/hash spot-checks selected with seed 20260921. Markdown/Python only: clj-kondo and Lisp check-parens are not applicable.', '']
    (output/'README.md').write_text('\n'.join(report))

def self_check(rows, root):
    assert len(rows)==len({r['path'] for r in rows})
    assert all(r['group'] in GROUPS for r in rows)
    for r in rows:
        if r['classification_method']=='manual':
            rev=REVIEWS[r['path']]
            assert r['source_sha256']==rev['sha256']
            assert r['evidence_quote']==rev['quote']
        if r['group'].startswith('closed-'):
            assert r['classification_method']=='manual'
    expected={
      'futon3/holes/missions/M-agency-unified-routing.md':'closed-unwitnessed',
      'futon3c/holes/missions/M-portfolio-inference.md':'closed-unwitnessed',
      'futon3c/holes/missions/M-IRC-stability.md':'closed-witnessed',
      'futon6/holes/missions/M-P7-rational-reconstruction.md':'undetermined'}
    indexed={r['path']:r for r in rows}
    for path,group in expected.items():
        r=indexed[path]; assert r['group']==group,(path,r['group'])
    bad=indexed['futon3/holes/missions/M-agency-unified-routing.md']
    assert bad['unchecked_boxes']>=7  # Real numbered unchecked criteria, not a stub.
    p='futon3c/holes/missions/M-IRC-stability.md'
    assert classify(p,'changed',[])[0]=='undetermined'
    assert classify('new.md','',[(3,'status','COMPLETE','Status: COMPLETE')])[0]=='undetermined'
    assert classify('new.md','',[])[0]=='undetermined'
    print('Self-checks passed; real-source unchecked/contradictory cases retained.')

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--root',type=Path,default=Path('/home/joe/code'))
    ap.add_argument('--self-check',action='store_true')
    args=ap.parse_args();rows=scan(args.root)
    if args.self_check:self_check(rows,args.root)
    write_outputs(rows,Path(__file__).resolve().parent,args.root)
    print(json.dumps({'documents':len(rows),'groups':dict(collections.Counter(r['group'] for r in rows)),
        'retirement_candidates':sum(r['retirement_candidate']=='yes' for r in rows)}))

if __name__=='__main__':main()
