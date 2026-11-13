(ns futon2.aif.wm.authored.req-8faa7f03
  "Repair patch for requisition 8faa7f036efb4be4335e7e2978b55a037367f6b70fc11eb2e9daa22cdbf8266d,
   cascade 2c5d1cbbcda2636bf96f62ee3a7acd39892f19124a1252f33f486b0601d9983f phase :compile.
   Supplies the operational clause owed for each of the five held
   :operational-clause-owed holes (owner :wm/enactment-compiler)."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["contracts/every-entry-has-a-falsifier" 0]
   {:operation :require-named-falsifier-at-entry-admission
    :capability :wm/pattern-apply
    :consumes [:entry-with-premise]
    :produces [:admitted-entry-with-falsifier]
    :completion-check
    [:admission-refuses-entry-without-falsifier
     "an entry that names no observable that would falsify it is refused at admission rather than compiled in"]}
   ["contracts/resolve-pointers-at-head" 1]
   {:operation :resolve-pointer-chain-iteratively-at-head
    :capability :wm/pattern-apply
    :consumes [:cascade-head-with-pointer-entries]
    :produces [:head-with-fully-resolved-referents]
    :completion-check
    [:no-unresolved-pointer-remains-at-head
     "after the head pass every pointer entry names its concrete referent and no dangling or self-referential pointer survives"]}
   ["contracts/holder-states-the-claim" 3]
   {:operation :bind-claim-to-named-holder-before-entry
    :capability :wm/pattern-apply
    :consumes [:claim-without-holder]
    :produces [:entry-whose-holder-states-the-claim]
    :completion-check
    [:entry-records-holder-and-claim-together
     "each compiled entry records both the holder that states the claim and the claim text; a holderless claim is not admitted"]}
   ["contracts/resolve-pointers-at-head" 4]
   {:operation :dereference-pointer-to-resolved-target-entry
    :capability :wm/pattern-apply
    :consumes [:pointer-entry :resolved-head]
    :produces [:dereferenced-entry-at-position-4]
    :completion-check
    [:dereference-yields-the-resolved-target-not-the-pointer
     "dereferencing at head position 4 yields the resolved target entry's content, and the pointer itself is not treated as the value"]}
   ["contracts/every-entry-has-a-falsifier" 5]
   {:operation :void-or-uphold-entry-by-named-falsifier
    :capability :wm/pattern-apply
    :consumes [:entry-with-premise]
    :produces [:falsified-or-standing-entry-verdict]
    :completion-check
    [:every-entry-names-its-falsifier
     "each compiled cascade entry carries an observable that would falsify it; entries without one are voided, not admitted"]}})

(defn decisions
  "Function of the resume context {:cascade-digest :phase}; returns the
  operational clause map keyed by hole id, bound to the cascade this patch
  repairs.  A foreign context is a typed mismatch, never a silent answer."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "2c5d1cbbcda2636bf96f62ee3a7acd39892f19124a1252f33f486b0601d9983f")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
