"use strict";

const assert = require("node:assert/strict");
const model = require("../resources/public/patchboard-viz.js");

const figure8 = model.analyse(model.clampShift(8));
assert.deepEqual(figure8.free, [7]);
assert.deepEqual(figure8.chain, [1, 2, 3, 4, 5, 6]);
assert.deepEqual(figure8.unsat, [0]);
assert.deepEqual(figure8.attractors.map(x => x.value), [42, 170]);

const nonInjective = model.analyse([0, 0, 1, 1]);
assert.deepEqual(nonInjective.free, [2, 3]);
assert.deepEqual(nonInjective.unsat, [0]);

for (let seed=0; seed<15; seed++) {
  const sim=model.createSimulation(60,seed);
  for (let generation=0; generation<120; generation++) {
    model.advance(sim,model.clampShift(8),2,121);
  }
  const terminalRules=[...new Set(sim.genotype)].map(bits=>parseInt(bits,2)).sort((a,b)=>a-b);
  assert.deepEqual(terminalRules,[42,170],`seed ${seed} reaches the Figure-8 pair`);
  assert.deepEqual(sim.pheHistory.at(-1),sim.pheHistory.at(-2),`seed ${seed} phenotype is static`);
}

console.log("patchboard browser model: assertions passed");
