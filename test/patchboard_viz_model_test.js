"use strict";

const assert = require("node:assert/strict");
const model = require("../resources/public/patchboard-viz.js");

const figure8 = model.analyse(model.clampShift(8));
assert.deepEqual(figure8.free, [7]);
assert.deepEqual(figure8.chain, [1, 2, 3, 4, 5, 6]);
assert.deepEqual(figure8.unsat, [0]);
assert.deepEqual(figure8.attractors.map(x => x.value), [42, 170]);

const antSham = model.analyse(model.identity(14), 0, false);
assert.equal(antSham.free.length, 0);
assert.equal(antSham.unsat.length, 14);

const nonInjective = model.analyse([0, 0, 1, 1], 0, false);
assert.deepEqual(nonInjective.free, [2, 3]);
assert.deepEqual(nonInjective.unsat, [0]);

console.log("patchboard browser model: assertions passed");
