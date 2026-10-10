# Correct untyped failure

**Status:** OPEN

## Observed failure

Target: T-repair-occ-487ca3f2205bf084f5d9bbb6b753cdc3908f106e8ff3aec60243a1bff7c34581; stage: :admission.

Context: {:phase :admission, :selected-entry {:action {:interpretation-receipts {:structure/unresolved-tensions-at-closure {:request-id "request-84ca111d67a7f792", :validator {:ns "futon2.aif.want-interpretation", :source-sha256 "e2278dd44059e91de451da49ff87a99858653f1f0a33e58577b98cdb3754b347"}, :source {:path "futon3/library/structure/unresolved-tensions-at-closure.flexiarg", :sha256 "f0e48e155b61616b77d26f45f4001fca794604d8d42fada0c90a6aff3723b81f"}, :kind :machine-requested, :scope-limit "The pattern supplies a closure test and disposition vocabulary; it does not prove this repair is complete, authorize concealing unresolved work, or make an OPEN ticket closable merely because its primary artifact exists.", :want :ticket-closure/h91bc7bfa40f1, :by "codex-proof2c", :validated {:checks [:canonical-id :library-source-sha :produces-want :guard-tokens-known :owner-constraints :constructs-through-it :admitted], :candidate {:precedence [:structure/unresolved-tensions-at-closure], :construction-receipt {:locator-coverage :token-set-not-supplied, :unknown-read-as-not-established [], :g-of-best {:value 2.392703229198059, :universe [:ticket-closure/h91bc7bfa40f1]}, :relations {:status :computed, :support {:relations [], :basis :produced-token-consumed-by-guard}, :meet {:relations [], :missing [], :basis :greatest-common-descendant}, :precedence {:relations [], :basis :generative-support, :linear-extension [:structure/unresolved-tensions-at-closure], :violations []}}, :moves [{:move-id :compose-by-need, :value 1.0, :g-comparison {:delta 1.0, :universe [:ticket-closure/h91bc7bfa40f1]}, :parts {:pragmatic 1.0, :epistemic nil, :epistemic-added 0.0, :cost 0.0, :includes-unformalised-novelty false}}], :unreached-wants [], :coverage {:moves-taken [:compose-by-need], :final-evaluation {}}, :stop-reason :no-admitted-move, :horizon 4, :search {:expanded 2, :limit 20000}, :ordering [{:move-id :order-by-need, :before [:structure/unresolved-tensions-at-closure], :after [:structure/unresolved-te … (see finding).

Requested array size exceeds VM limit

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance evidence

Retain a reproduction or regression check that detects the reported failure and passes after the change, together with the scoped validation output. Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-10b67fac78ac48c9aa5be0ee3766e4c0de30aeef93fe8837964a1eca3c310190](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-10b67fac78ac48c9aa5be0ee3766e4c0de30aeef93fe8837964a1eca3c310190.edn)

Finding SHA-256: `630c2ec58e513deb33e2677cce105641af2bae7b96cb20f05f586beb79005280`
