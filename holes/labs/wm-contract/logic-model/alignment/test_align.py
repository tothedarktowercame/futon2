import importlib.util
import unittest
from pathlib import Path

_SPEC = importlib.util.spec_from_file_location("align", Path(__file__).with_name("align.py"))
_ALIGN = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_ALIGN)
align = _ALIGN.align


def site(fn, reason, line, tag="P"):
  return {"id": f"{tag}:{line}:{reason.split('/')[-1]}", "tag": tag, "fn": fn,
          "line": line, "kind": "refusal", "reason": reason}


def entity(name, text):
  return {"id": f"lean:RouteState.{name}", "inductive": "RouteState",
          "name": name, "line": 1, "text": text}


def edge_ids(edges, basis):
  return next(edge["hx/endpoints"][1:] for edge in edges
              if edge["props"]["basis"] == basis)


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

  def test_positive_anchor_makes_bare_keywords_prose(self):
    sites = [site("production-readiness", "alpha", 10),
             site("route!", "beta", 20)]
    edges = align(sites, [entity("readinessRefused", "(P production-readiness :alpha) :beta")])
    self.assertEqual(["P:10:alpha"], edge_ids(edges, "keyword"))

  def test_name_rule_is_scoped_to_anchored_function(self):
    sites = [site("prepare-acceptance", "acceptance-unavailable", 30),
             site("produce-acceptance-from-request!", "acceptance-unavailable", 40)]
    edges = align(sites, [entity("acceptanceUnavailable", "(P prepare-acceptance)")])
    self.assertEqual(["P:30:acceptance-unavailable"], edge_ids(edges, "name"))


if __name__ == "__main__":
  unittest.main()
