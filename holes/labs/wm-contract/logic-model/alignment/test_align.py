import importlib.util
import tempfile
import unittest
from pathlib import Path

_SPEC = importlib.util.spec_from_file_location("align", Path(__file__).with_name("align.py"))
_ALIGN = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_ALIGN)
align = _ALIGN.align


def site(fn, reason, line, tag="P", kind="refusal"):
  return {"id": f"{tag}:{line}:{reason.split('/')[-1]}", "tag": tag, "fn": fn,
          "line": line, "kind": kind, "reason": reason}


def entity(name, text):
  return {"id": f"lean:RouteState.{name}", "inductive": "RouteState",
          "name": name, "line": 1, "text": text}


def edge_ids(edges, basis):
  return next((edge["hx/endpoints"][1:] for edge in edges
               if edge["props"]["basis"] == basis), [])


class AnchorFunctionScopingTest(unittest.TestCase):
  def setUp(self):
    self.sites = [site("production-readiness", "meta-self-heal/production-config-required", 10),
                  site("route!", "meta-self-heal/production-config-required", 20)]

  def test_keyword_inside_anchor_is_scoped_to_its_function(self):
    edges = align(self.sites, [entity("readinessRefused",
                                     "(P production-readiness :meta-self-heal/production-config-required)")])
    self.assertEqual(["P:10:production-config-required"], edge_ids(edges, "keyword"))

  def test_keyword_outside_any_anchor_keeps_unscoped_behavior(self):
    edges = align(self.sites, [entity("readinessRefused",
                                     ":meta-self-heal/production-config-required")])
    self.assertEqual(["P:10:production-config-required", "P:20:production-config-required"],
                     edge_ids(edges, "keyword"))

  def test_negative_anchor_excludes_its_keyword_from_all_keyword_edges(self):
    sites = [site("classify", "reason-x", 10, "T")]
    positive = align(sites, [entity("positive", "(T classify :reason-x)")])
    negative = align(sites, [entity("negative", "(not T classify :reason-x) :reason-x")])
    self.assertEqual(["T:10:reason-x"], edge_ids(positive, "keyword"))
    self.assertEqual([], negative)

  def test_bare_keyword_in_anchored_docstring_reaches_only_anchored_functions(self):
    # rev 12's form: a function anchor with no keyword, then that function's reasons bare
    edges = align(self.sites, [entity("readinessRefused",
                                     "(P production-readiness) :meta-self-heal/production-config-required")])
    self.assertEqual(["P:10:production-config-required"], edge_ids(edges, "keyword"))

  def test_line_citation_makes_no_edge(self):
    # rev 20: line numbers are not a citation basis at all
    sites = [site("coordinate-queued-retry", "retry-budget-spent", 261, "C")]
    self.assertEqual([], align(sites, [entity("abandoned", "returned at C:261")]))

  def test_positive_anchor_makes_bare_keywords_prose(self):
    sites = [site("production-readiness", "alpha", 10),
             site("route!", "beta", 20)]
    edges = align(sites, [entity("readinessRefused", "(P production-readiness :alpha) :beta")])
    self.assertEqual(["P:10:alpha"], edge_ids(edges, "keyword"))

  def test_anchor_callee_matches_only_its_anchored_function(self):
    call = site("coordinate", "ledger/append-retry-result", 10, "C", "call")
    edges = align([call], [entity("reopened", "(C coordinate ledger/append-retry-result)")])
    self.assertEqual(["C:10:append-retry-result"], edge_ids(edges, "call"))
    wrong_function = align([call], [entity("queued", "(C coordinate-queued-retry ledger/append-retry-result)")])
    self.assertEqual([], wrong_function)

  def test_bare_or_negative_callee_never_creates_a_call_edge(self):
    call = site("coordinate", "ledger/append-retry-result", 10, "C", "call")
    self.assertEqual([], align([call], [entity("bare", "ledger/append-retry-result")]))
    self.assertEqual([], align([call], [entity("negative", "(not C coordinate ledger/append-retry-result)")]))

  def test_keyword_and_name_citations_do_not_reach_a_call_site(self):
    call = site("coordinate", "ledger/append-retry-result", 10, "C", "call")
    self.assertEqual([], align([call], [entity("reopened", "the requeue says `:append-retry-result`")]))
    self.assertEqual([], align([call], [entity("appendRetryResult", "no anchor names the callee")]))

  def test_name_rule_is_scoped_to_anchored_function(self):
    sites = [site("prepare-acceptance", "acceptance-unavailable", 30),
             site("produce-acceptance-from-request!", "acceptance-unavailable", 40)]
    edges = align(sites, [entity("acceptanceUnavailable", "(P prepare-acceptance)")])
    self.assertEqual(["P:30:acceptance-unavailable"], edge_ids(edges, "name"))


def status_map(fn, status, line, tag="C", reason=None):
  row = {"id": f"{tag}:{line}", "tag": tag, "fn": fn, "line": line, "kind": "status-map", "status": status}
  if reason:
    row["reason"] = reason
  return row


class StatusMapTest(unittest.TestCase):
  """A reasonless status map is reached by its status, inside an anchor only (rev 20)."""
  def test_anchored_status_reaches_reasonless_map(self):
    edges = align([status_map("coordinate-queued-retry", "awaiting-retry", 248)],
                  [entity("awaitingRetry", "`(C coordinate-queued-retry :awaiting-retry)`")])
    self.assertEqual(["C:248"], edge_ids(edges, "keyword"))

  def test_bare_status_reaches_nothing(self):
    self.assertEqual([], align([status_map("coordinate", "discharged", 415)],
                               [entity("discharged", "`:discharged`: the retry succeeded")]))

  def test_status_in_another_function_is_not_reached(self):
    self.assertEqual([], align([status_map("coordinate", "awaiting-retry", 400)],
                               [entity("awaitingRetry", "`(C coordinate-queued-retry :awaiting-retry)`")]))

  def test_anchor_naming_a_reason_does_not_reach_reasonless_maps(self):
    sites = [status_map("run-opportunity!", "not-required", 479, "R", "meta-self-heal/acceptance-retry-not-prepared"),
             status_map("run-opportunity!", "not-required", 498, "R")]
    edges = align(sites, [entity("retryClick",
                                 "(R run-opportunity! {:status :not-required :reason :meta-self-heal/acceptance-retry-not-prepared})")])
    self.assertEqual(["R:479"], edge_ids(edges, "keyword"))

  def test_status_does_not_reach_a_map_with_another_reason(self):
    sites = [status_map("prepare-diagnosis", "unavailable", 882, "P", "meta-self-heal/diagnosis-active-set-unavailable"),
             status_map("prepare-diagnosis", "unavailable", 892, "P", "meta-self-heal/diagnosis-ledger-unavailable")]
    edges = align(sites, [entity("activeSetUnavailable",
                                 "(P prepare-diagnosis {:status :unavailable :reason :meta-self-heal/diagnosis-active-set-unavailable})")])
    self.assertEqual(["P:882"], edge_ids(edges, "keyword"))

  def test_status_only_anchor_does_not_reach_a_map_with_a_reason(self):
    sites = [status_map("prepare-diagnosis", "unavailable", 892, "P", "meta-self-heal/diagnosis-ledger-unavailable"),
             status_map("prepare-diagnosis", "unavailable", 900, "P")]
    edges = align(sites, [entity("ledgerUnreadable", "(P prepare-diagnosis {:status :unavailable})")])
    self.assertEqual(["P:900"], edge_ids(edges, "keyword"))


def defn(fn, source, tag="P"):
  return {"tag": tag, "fn": fn, "line": 1, "end-line": 1, "kind": "defn", "source": source}


class AnchorResolutionTest(unittest.TestCase):
  """Every anchor must resolve against the code; one that does not is an error (rev 20)."""
  def setUp(self):
    self.rows = [defn("admit-acceptance*", '(defn admit-acceptance* [x] (refusal :meta-self-heal/acceptance-run-refused) (ledger/append x) "identity-keys")'),
                 defn("admit-acceptance!", "(defn admit-acceptance! [x] (admit-acceptance* x))"),
                 defn("route!", "(defn route! [] {:status ::checkpoint-persist-failed})"),
                 site("admit-acceptance*", "meta-self-heal/acceptance-run-refused", 5)]

  def unresolved(self, text):
    with tempfile.NamedTemporaryFile("w", suffix=".lean", delete=False) as f:
      f.write(text)
    return [why for _f, _n, _a, why in _ALIGN.unresolved_anchors(self.rows, [f.name])]

  def test_good_anchors_resolve(self):
    self.assertEqual([], self.unresolved(
      '(P admit-acceptance* :meta-self-heal/acceptance-run-refused) (P admit-acceptance* ledger/append) '
      '(P admit-acceptance* "identity-keys") (P admit-acceptance!) (P route! :meta-self-heal/checkpoint-persist-failed)'))

  def test_missing_function(self):
    self.assertEqual(["no function `admit-acceptance` in P"], self.unresolved("(P admit-acceptance)"))

  def test_keyword_that_moved_to_another_function(self):
    # rev 19's eight `(P admit-acceptance! :...)` anchors after the split into admit-acceptance*
    self.assertEqual(["not in `admit-acceptance!`: :meta-self-heal/acceptance-run-refused"],
                     self.unresolved("(P admit-acceptance! :meta-self-heal/acceptance-run-refused)"))

  def test_missing_callee_and_needle(self):
    self.assertEqual(['not in `admit-acceptance*`: ledger/append-requeue, "persist-acceptance!"'],
                     self.unresolved('(P admit-acceptance* ledger/append-requeue "persist-acceptance!")'))

  def test_negative_anchor_is_checked_too(self):
    self.assertEqual(["no function `nope` in P"], self.unresolved("(not P nope :x)"))

  def test_keyword_inside_a_needle_is_not_cited(self):
    rows = [defn("integrate", "(defn integrate [p] (nil? previous))", "T")]
    with tempfile.NamedTemporaryFile("w", suffix=".lean", delete=False) as f:
      f.write('(T integrate "nil? previous")')
    self.assertEqual([], _ALIGN.unresolved_anchors(rows, [f.name]))
    forms = _ALIGN.anchor_forms('(T integrate "a :kw ns/callee")')
    self.assertEqual((set(), set(), ["a :kw ns/callee"]), (forms[0][2], forms[0][3], forms[0][6]))

  def test_line_shaped_anchor_is_unresolved(self):
    self.assertEqual(["no function `lines` in P"], self.unresolved("(P lines 1029-1031)"))


class LineCitationTest(unittest.TestCase):
  def test_line_citations_are_listed(self):
    text = ("P:12, C line 260, T lines 247-251, `full_loop_runtime.clj:475-505`, "
            "`meta_injury_store.clj` L36-49, (L231), but not units L1 or L2")
    with tempfile.NamedTemporaryFile("w", suffix=".lean", delete=False) as f:
      f.write(text)
    self.assertEqual(["P:12", "C line 260", "T lines 247-251", ".clj:475", "L36-49", "L231"],
                     [c for _f, _n, c in _ALIGN.line_citations([f.name])])


if __name__ == "__main__":
  unittest.main()
