(ns futon2.aif.wm.injury41-data-vars
  "Synthetic patch targets for the injury 41a00c62 acceptance test: data
  vars of three shapes that Clojure's `ifn?` admits (a map, a set and a
  keyword are all callable with one argument, as a lookup) but that are not
  functions of the resume context.  Registered with load-identity so the
  production patch-loader's source-digest check can pass for them and the
  function-valued check is the one under test.  No production code reads
  this namespace."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses-map
  "A bare decision map, the same shape as the authored `clauses` var."
  {["futon-theory/mission-lifecycle" 1] {:operation :record-open-identify-status}})

(def clauses-set
  "A set: callable as a lookup, never a function of the context."
  #{["futon-theory/mission-lifecycle" 1]})

(def clauses-keyword
  "A keyword: callable as a lookup, never a function of the context."
  :clauses)
