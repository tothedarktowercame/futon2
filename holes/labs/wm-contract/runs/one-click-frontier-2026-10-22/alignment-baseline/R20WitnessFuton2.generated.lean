import DarkTower.WarMachine.R20Witness

namespace DarkTower.WarMachine.R20Witness.Generated
open DarkTower.WarMachine.R20Witness
open DarkTower.WarMachine.R20Graph

def futon2Witness : Witness :=
  { sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738"
    edges := [
    ⟨.selectionToPrecisionHabit, [{ writer := "futon2/src/futon2/aif/policy.clj#selection-candidate", field := ":habit", record := ":selection-candidate", fieldType := "quantity/policy-prior", reader := "futon2/src/futon2/aif/policy_precision_carry.clj#family", confirmed := true, sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738" }, { writer := "futon2/src/futon2/aif/policy_precision_carry.clj#family", field := ":candidates", record := ":precision-family", fieldType := "receipt/frozen-policy-family", reader := "futon2/src/futon2/aif/policy_precision_carry.clj#advance", confirmed := true, sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738" }]⟩,
    ⟨.selectionToPrecisionG, [{ writer := "futon2/src/futon2/aif/policy.clj#selection-candidate", field := ":g", record := ":selection-candidate", fieldType := "quantity/expected-free-energy", reader := "futon2/src/futon2/aif/policy_precision_carry.clj#family", confirmed := true, sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738" }, { writer := "futon2/src/futon2/aif/policy_precision_carry.clj#family", field := ":candidates", record := ":precision-family", fieldType := "receipt/frozen-policy-family", reader := "futon2/src/futon2/aif/policy_precision_carry.clj#advance", confirmed := true, sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738" }]⟩,
    ⟨.inferenceToPrecisionF, [{ writer := "futon2/src/futon2/aif/policy_precision_carry.clj#retrospective-f", field := ":f", record := ":precision-retrospective", fieldType := "quantity/free-energy", reader := "futon2/src/futon2/aif/policy_precision_carry.clj#advance", confirmed := true, sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738" }]⟩,
    ⟨.strategicFocusToClassPreference, [{ writer := "futon2/src/futon2/aif/focus_receipt.clj#decision-focus", field := ":focus", record := ":decision-focus", fieldType := "quantity/preference", reader := "futon2/src/futon2/aif/meta_live_outer_selector.clj#selector", confirmed := false, sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738" }, { writer := "futon2/src/futon2/aif/meta_live_outer_selector.clj#selector", field := ":scorer-class", record := ":outer-target-class", fieldType := "quantity/preference", reader := "futon2/src/futon2/aif/cascade_shape_g.clj#class-emission-model", confirmed := false, sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738" }]⟩,
    ⟨.selectionToClassPreference, [{ writer := "futon2/src/futon2/aif/observation_model.clj#evaluate", field := ":prediction", record := ":class-step", fieldType := "quantity/predicted-class-distribution", reader := "futon2/src/futon2/aif/wm/apparatus_certificates.clj#class-preference-row", confirmed := true, sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738" }]⟩,
    ⟨.classPreferenceToSelectionG, [{ writer := "futon2/src/futon2/aif/cascade_shape_g.clj#class-emission-model", field := ":class-preference", record := ":class-model", fieldType := "quantity/preference-schedule", reader := "futon2/src/futon2/aif/observation_model.clj#evaluate", confirmed := true, sha := "bf5190475250ca2ef7fecf1aa9af6beb1b396738" }]⟩] }

#eval verdicts futon2Witness

end DarkTower.WarMachine.R20Witness.Generated
