# C403 — derived census subjects

`repository_census_basis_check` now derives source subjects from backticked
path-like citations in each registered artifact. It resolves explicit repository
prefixes, tracked repository-relative paths, and unique tracked basenames, while
stripping line annotations before identity comparison.

The derived and declared sets must be equal. An omission is
`:derived-subject-undeclared`; a surplus declaration is
`:declared-subject-not-cited`; an ambiguous or unresolved path is typed rather
than guessed. Every derived subject's repository must also have a recorded
basis commit.

The first derivation corrected the packet's premise: C385 has **11** resolvable
source paths, not nine—eight Futon2, two Futon3c, and one p4ng. C401 declared all
eight Futon2 paths. The remaining three are now reported both undeclared and
unpinned. The checker exits 1 (`:unavailable`) until the census owner records
historically justified Futon3c/p4ng bases or explicitly revises the census's
scope; this packet does not invent retroactive commits or silently populate the
registry.

The convention therefore requires one basis per cited repository. Scoping a
census to one repository is valid only when its document does not use other
repositories' sources as census evidence.

Focused checks: the live incomplete declaration exits 1 with the three named
cross-repository omissions; the omission control removes a declared Futon2
subject and exits 0 only after detecting it; clj-kondo is clean; inventory has
zero unknown and missing checks.
