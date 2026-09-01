# C453 — citation verification for `aif-equations.edn`

Date: 2026-09-01

Scope: query 3d of `holes/TN-edge-review-aif-wiring.md`. Discovery only; the
registry was not edited.

## Method and verdict rule

No identifiable copies of the six sources were present in the local source
tree. Network access was available, so open copies of the four articles, the
Kass--Raftery paper, and the Parr--Pezzulo--Friston book were downloaded to a
temporary directory and inspected with `pdftotext`. Equation numbers below
come from those source texts, not from secondary summaries.

`VERIFIED` means the recorded use is present in the inspected source.
`UNVERIFIED AS RECORDED` means the source supports part or a more general
ancestor of the formulation, but the complete registry attribution is not in
that source. This deliberately treats a partial match as unverified for
publication.

## Reference-by-reference results

| key | reference as recorded | verdict | source finding |
|---|---|---|---|
| `:buckley2017` | Buckley, C. L., Kim, C. S., McGregor, S., & Seth, A. K. (2017). *The free energy principle for action and perception: a mathematical review*. Journal of Mathematical Psychology, 81, 55–79. | **UNVERIFIED AS RECORDED** | The inspected author preprint supports the continuous/Laplace family: multivariate Laplace-encoded energy is a precision-weighted sum of squared prediction errors in eq. (77); prediction errors are defined in eqs. (78)–(79); precision is inverse variance in eq. (84); recognition dynamics are gradient descent in eqs. (48)–(50), with explicit precision-weighted errors in eqs. (82)–(86). It does **not** contain the registry's exact channel equations `eps_k := o_k - mu_k`, `Pi_k := 1/max(Var_k,eps0)`, `F = 1/2 mean_k(Pi_k eps_k^2)`, or the discrete update `mu <- mu + alpha Pi eps`. Those are local simplifications/specializations; the variance floor and channel mean in particular are not established by this citation. Verifying them requires either labeling them as stack choices derived from the cited general equations or citing a source that writes those discrete channel equations explicitly. |
| `:dacosta2020` | Da Costa, L., Parr, T., Sajid, N., Veselic, S., Neacsu, V., & Friston, K. (2020). *Active inference on discrete state-spaces: a synthesis*. Journal of Mathematical Psychology, 99, 102447. | **UNVERIFIED AS RECORDED** | Much of the attribution is directly present. Policy prediction and depth are developed in §6; `Q(pi)=sigma(-G(pi))` and risk/ambiguity appear in eq. (10), the decomposition again in eq. (13), outcome-space risk in eq. (15), and the explicit outcome-risk form in eq. (50). Dirichlet learning of `A` is eqs. (17)–(21), with `B`/`D` learning in appendix A.1, eqs. (27)–(33). However, the registry's `Q(pi) := softmax(ln E(pi) - G(pi)/tau)` is not eq. (10): the paper's main equation omits `E` and temperature; footnote 5 gives a more complete `sigma(-log E - F - G)`, and appendix A.2 separately adds inverse temperature `gamma`. The exact combined registry formula therefore is not verified by this reference. |
| `:friston2017` | Friston, K., FitzGerald, T., Rigoli, F., Schwartenbeck, P., & Pezzulo, G. (2017). *Active inference: a process theory*. Neural Computation, 29(1), 1–49. | **UNVERIFIED AS RECORDED** | The process and several rows are present: the generative model and policy prior `P(pi)=sigma(-gamma G(pi))` are eq. (2.1); policies and posteriors are eq. (2.2); action selection and the observation-following-action loop are eq. (2.3); expected free energy is eq. (2.5), with risk/ambiguity in eq. (2.6). The paper explicitly identifies `gamma` as inverse temperature/precision. It discusses habit formation, but the inspected equations do not use the registry's symbolic habit prior `E`; `E` in that exact policy-softmax role is therefore not verified from this source. The `action` row's “argmax or sample of Q(pi)” is also not the paper's eq. (2.3), which minimizes expected outcome prediction error. |
| `:parr2022` | Parr, T., Pezzulo, G., & Friston, K. J. (2022). *Active Inference: The Free Energy Principle in Mind, Brain, and Behavior*. MIT Press. | **VERIFIED** | The inspected book gives the consolidated discrete notation and risk/ambiguity decomposition in chapter 4: policy prior and expected free energy eq. (4.7), information-gain/pragmatic and ambiguity/risk forms eq. (4.9), and the linear-algebra form with `A`, `C`, predicted outcomes and softmax eq. (4.10), pp. 72–74. Appendix B gives `pi₀ = sigma(ln E - G)` in eq. (B.7), identifies `E` as the fixed policy bias/habit term, and gives the posterior `pi = sigma(ln E - F - G)` in eq. (B.9), pp. 246–247. This source does support the reference-map description, although no equation row currently names `:parr2022`. |
| `:friston2018bmr` | Friston, K., Parr, T., & Zeidman, P. (2018). *Bayesian model reduction*. arXiv:1805.07092. | **UNVERIFIED AS RECORDED** | Bayesian model reduction itself is present: §3 derives reduced-model free energy/evidence in eqs. (9)–(10), and §5/Table 1 gives distribution-specific log-evidence changes `Delta F`, including the Dirichlet case. But the derivation requires the full posterior plus full and reduced priors; it is not a transformation importing only the scalar `F`, as the registry row says. The paper also does not supply the registry's acceptance rule `Delta F <= -3`; its worked discussion accepts a reduced model when its relative evidence is greater than zero under the paper's convention. The BMR citation verifies the family, not the complete registry equation/import/sign rule. |
| `:kass1995` | Kass, R. E., & Raftery, A. E. (1995). *Bayes factors*. Journal of the American Statistical Association, 90(430), 773–795. | **VERIFIED (magnitude only)** | Table 2 on printed p. 777 classifies `2 ln(B10)` from 6 to 10 (equivalently `ln(B10)` from 3 to 5) as “Strong.” This verifies a log-Bayes-factor magnitude threshold of 3. It does not determine the registry's sign: `Delta F <= -3` is valid only after the registry defines `Delta F` in the direction opposite `ln(B10)` for the preferred reduced model. Kass--Raftery supplies the magnitude/label, not that local orientation. |

## Is any used formulation uncovered by a reference?

No clearly required **fourth external literature family** was found. The three
families named in the review note do cover the conceptual territory:

1. continuous/Laplace predictive coding and gradient descent — Buckley et al.;
2. discrete expected free energy, policy inference, and learning — Da Costa et
   al., Friston et al., and the Parr et al. synthesis;
3. Bayesian model reduction and evidence-strength interpretation — Friston et
   al. 2018 plus Kass--Raftery.

There are nevertheless formulations used by the registry that are not covered
by the **single reference attached to their row**:

- `Pi_k := 1/max(Var_k,eps0)` and the mean-over-channels `F` are local
  regularization/aggregation choices, not equations found in Buckley et al.
- `Q(pi) := softmax(ln E(pi) - G(pi)/tau)` is a composite. Da Costa et al.
  supplies `sigma(-G)` and separately inverse temperature; Parr et al. supplies
  `ln E - G` (and `ln E - F - G` for the posterior). The row cites only Da
  Costa et al.
- `Delta F` importing only `F` suppresses the priors and posterior required by
  BMR eqs. (9)–(10).
- `Delta F <= -3` combines the BMR quantity with Kass--Raftery's magnitude
  scale and a local sign convention. Neither source alone states that rule.
- “argmax or sample of `Q(pi)`” remains an unresolved implementation choice;
  Da Costa eq. (11) specifies a Bayesian-model-average argmax, while Friston
  eq. (2.3) uses expected prediction-error minimization.

Thus the bibliography has the relevant source families, but the per-equation
attribution is incomplete. The publication-safe remedy is not to invent a
seventh citation: it is to give composite rows multiple references and mark
the variance floor, channel aggregation, BMR abstraction, threshold sign, and
selection rule as local choices unless a source for their exact forms is
provided.

## Inspected source copies

- Buckley et al. author preprint: `https://arxiv.org/pdf/1705.09156`
- Da Costa et al. author preprint: `https://arxiv.org/pdf/2001.07203`
- Friston et al. published-version copy: `https://activeinference.github.io/papers/process_theory.pdf`
- Parr et al. MIT Press book PDF mirror (the PDF identifies the MIT Press book
  file `book_9780262369978.pdf`):
  `https://www.math4wisdom.com/files/2022_Textbook-ActiveInference.pdf`
- Friston, Parr & Zeidman preprint: `https://arxiv.org/pdf/1805.07092`
- Kass & Raftery article copy:
  `https://www.stat.cmu.edu/~kass/papers/bayesfactors.pdf`

All were accessed 2026-09-01. Temporary downloads were not added to the
repository.
