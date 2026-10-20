import importlib.util
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

  def test_negative_anchor_keyword_does_not_confirm_a_line_citation(self):
    sites = [site("coordinate-queued-retry", "retry-budget-spent", 261, "C")]
    edges = align(sites, [entity("abandoned",
                                 "a second literal at C:261 is not this constructor: (not C coordinate-queued-retry :retry-budget-spent)")])
    self.assertEqual([], edge_ids(edges, "line"))
    self.assertEqual(["C:261:retry-budget-spent"], edge_ids(edges, "line-unconfirmed"))

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

  def test_line_and_name_citations_do_not_reach_a_call_site(self):
    call = site("coordinate", "ledger/append-retry-result", 10, "C", "call")
    self.assertEqual([], align([call], [entity("reopened", "the requeue at C:10 says `:append-retry-result`")]))
    self.assertEqual([], align([call], [entity("appendRetryResult", "no anchor names the callee")]))

  def test_name_rule_is_scoped_to_anchored_function(self):
    sites = [site("prepare-acceptance", "acceptance-unavailable", 30),
             site("produce-acceptance-from-request!", "acceptance-unavailable", 40)]
    edges = align(sites, [entity("acceptanceUnavailable", "(P prepare-acceptance)")])
    self.assertEqual(["P:30:acceptance-unavailable"], edge_ids(edges, "name"))


class StaleCitationDefnLineTest(unittest.TestCase):
  """A cite that drifts onto a defn line is stale unless the text names that function."""
  def setUp(self):
    self.rows = [{"id": "P:30", "tag": "P", "fn": "admit-acceptance!", "line": 30, "kind": "defn"},
                 site("admit-acceptance!", "meta-self-heal/acceptance-run-refused", 40)]
    self.owed = [r for r in self.rows if r["kind"] != "defn"]

  def test_drifted_cite_on_defn_line_is_stale(self):
    ents = [entity("runRefused", "`:acceptance-run-refused` (P:30): refused.")]
    self.assertEqual(_ALIGN.stale_citations(self.rows, self.owed, ents),
                     [("lean:RouteState.runRefused", "P:30")])

  def test_cite_of_named_function_line_is_not_stale(self):
    ents = [entity("runRefused", "refused inside `admit-acceptance!` (P:30).")]
    self.assertEqual(_ALIGN.stale_citations(self.rows, self.owed, ents), [])

  def test_cite_of_site_line_is_not_stale(self):
    ents = [entity("runRefused", "`:acceptance-run-refused` (P:40).")]
    self.assertEqual(_ALIGN.stale_citations(self.rows, self.owed, ents), [])


if __name__ == "__main__":
  unittest.main()
