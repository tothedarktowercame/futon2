# RUN4 hierarchy disposition — 2026-09-09

Item 25 item 2: config-only disposition. The run config now declares the
single-level model, RUN4 scope, ruling source, and all follow-ons. No new
architectural model or probabilistic coupling is implemented. This is enough
for this item because the ruling accepts the existing single-level model and
the packet expressly permits a config/record declaration. No runtime trace
persistence of this new config field is claimed; the committed run config is
the provenance artifact. The final run should identify this config's commit.

Item 1's shared caller edit is pending codex-10 file release, per the chain's
wait-not-merge instruction. This independent item proceeds under its rule to
continue other items when one is blocked. No depth paths touched.
