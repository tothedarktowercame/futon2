import DarkTower.WarMachine.Holes

open Set
namespace DarkTower.WarMachine.Holes.R9D2

def wmVerdictsLedgerAloneFixture : VerdictTable := [
  { row := "O1", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O2", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O3", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O5", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O6", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O7", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O8", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O9", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O14", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O15", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O16", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O17", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown },
  { row := "O20", declarationSource := .paperSentence, producer := "unknown", declaredPart := [], verdict := .unknown }
]

def wmVerdictsDeclaredFixture : VerdictTable := [
  { row := "O1", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self },
  { row := "O2", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self },
  { row := "O3", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self },
  { row := "O5", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self },
  { row := "O6", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self },
  { row := "O7", declarationSource := .rowText "O7", producer := "codex-1", declaredPart := ["author", "codex-1", "codex-7", "zai"], verdict := .self },
  { row := "O8", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self },
  { row := "O9", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self },
  { row := "O14", declarationSource := .rowText "O14", producer := "codex-1", declaredPart := ["author", "codex-1", "codex-7", "zai"], verdict := .self },
  { row := "O15", declarationSource := .rowText "O15", producer := "zai", declaredPart := ["author", "codex-1", "codex-7", "zai"], verdict := .self },
  { row := "O16", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self },
  { row := "O17", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self },
  { row := "O20", declarationSource := .paperSentence, producer := "author", declaredPart := ["author"], verdict := .self }
]

theorem verdictConsultsChecker :
    ∀ {Part : Type*} [DecidableEq Part] (claim : Claim Part) (w : Witness Part),
      w.producer ∈ claim.producingPart →
      ∃ decide? : Part → Set Part → Bool,
        ¬ (∀ p S, decide? p S = true ↔ p ∈ S) ∧
        independenceVerdict (some claim) w decide? = .independent := by
  intro Part inst claim w hw
  refine ⟨fun _ _ => false, ?_, ?_⟩
  · intro h
    have := (h w.producer claim.producingPart).mpr hw
    simp at this
  · simp [independenceVerdict]

theorem recordedVerdictsSound : r9VerdictsSound wmVerdictsDeclaredFixture := by
  simp [r9VerdictsSound, wmVerdictsDeclaredFixture, VerdictRow.inDeclaredPart]
theorem perRowDeclarations : r9PerRowDeclarations wmVerdictsDeclaredFixture := by
  simp [r9PerRowDeclarations, wmVerdictsDeclaredFixture]
theorem twoRunCensus :
    wmVerdictsLedgerAloneFixture.length = 13 ∧ wmVerdictsDeclaredFixture.length = 13 ∧
    (∀ r ∈ wmVerdictsLedgerAloneFixture, r.verdict = .unknown) ∧
    (∀ r ∈ wmVerdictsDeclaredFixture, r.verdict = .self) := by
  simp [wmVerdictsLedgerAloneFixture, wmVerdictsDeclaredFixture]

end DarkTower.WarMachine.Holes.R9D2
