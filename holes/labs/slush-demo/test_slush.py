"""Pytest: verify the slush ambiguity formula matches hand-computed values.

Tests the EXACT port of futon2.aif.bmr/dirichlet-moments and
futon2.aif.a4a/concept->stddev (RMS aggregation). Ships STDDEV, never variance.
"""
import math

import pytest

from slush_proxy import concept_stddev, dirichlet_moments


def test_dirichlet_moments_basic():
    """Hand-computed: alpha=[2,3,4], alpha0=9."""
    moments = dirichlet_moments([2.0, 3.0, 4.0])
    alpha0 = 9.0
    assert len(moments) == 3
    for i, (ai, m) in enumerate(zip([2.0, 3.0, 4.0], moments)):
        expected_mean = ai / alpha0
        expected_var = ai * (alpha0 - ai) / (alpha0**2 * (alpha0 + 1))
        expected_std = math.sqrt(expected_var)
        assert m["mean"] == pytest.approx(expected_mean, rel=1e-12)
        assert m["stddev"] == pytest.approx(expected_std, rel=1e-12)


def test_dirichlet_moments_uniform():
    """Uniform alpha=[1,1,1,1]: all components identical."""
    moments = dirichlet_moments([1.0, 1.0, 1.0, 1.0])
    alpha0 = 4.0
    assert len(moments) == 4
    # All means equal
    assert moments[0]["mean"] == pytest.approx(0.25, rel=1e-12)
    # All stddevs equal
    expected_var = 1.0 * (4.0 - 1.0) / (16.0 * 5.0)
    expected_std = math.sqrt(expected_var)
    for m in moments:
        assert m["stddev"] == pytest.approx(expected_std, rel=1e-12)
    # Check the actual value
    assert moments[0]["stddev"] == pytest.approx(0.1936492, rel=1e-5)


def test_dirichlet_moments_high_concentration_low_stddev():
    """High concentration (well-learned) -> low stddev (sharp)."""
    sharp = dirichlet_moments([100.0, 100.0, 100.0, 100.0])
    blurry = dirichlet_moments([1.0, 1.0, 1.0, 1.0])
    assert sharp[0]["stddev"] < blurry[0]["stddev"]


def test_concept_stddev_rms():
    """concept_stddev = RMS of per-outcome stddevs (not arithmetic mean)."""
    alpha = [2.0, 3.0, 4.0]
    moments = dirichlet_moments(alpha)
    stds = [m["stddev"] for m in moments]
    expected_rms = math.sqrt(sum(s * s for s in stds) / len(stds))
    assert concept_stddev(alpha) == pytest.approx(expected_rms, rel=1e-12)
    # Verify it's NOT the arithmetic mean
    arith_mean = sum(stds) / len(stds)
    assert concept_stddev(alpha) != pytest.approx(arith_mean, rel=1e-6)


def test_concept_stddev_sharp_vs_blurry():
    """Sharp concepts (high concentration) have lower aggregate stddev."""
    sharp_std = concept_stddev([20.0, 30.0, 10.0, 40.0])
    blurry_std = concept_stddev([1.0, 2.0, 1.0, 1.0])
    assert sharp_std < blurry_std
    assert sharp_std < 0.10  # sharp concepts should be well below 0.1
    assert blurry_std > 0.10  # blurry concepts should be above 0.1


def test_concept_stddev_hand_computed():
    """Exact hand-computed RMS stddev for alpha=[2,3,4]."""
    # stddevs: sqrt(0.017284)=0.131468, sqrt(0.022222)=0.149071, sqrt(0.024691)=0.157135
    # RMS = sqrt((0.131468^2 + 0.149071^2 + 0.157135^2)/3)
    expected = 0.146285
    assert concept_stddev([2.0, 3.0, 4.0]) == pytest.approx(expected, rel=1e-4)


def test_variance_never_exposed():
    """The formula ships stddev, never raw variance — verify key naming."""
    moments = dirichlet_moments([1.0, 2.0, 3.0])
    for m in moments:
        assert "stddev" in m
        assert "variance" not in m  # contract: never expose variance
        assert m["stddev"] > 0  # stddev is always positive
