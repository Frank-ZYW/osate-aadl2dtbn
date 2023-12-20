package org.osate2.bayes.dtbn.Util;

import java.util.Map;

import org.apache.commons.math3.distribution.*;

public class Sampler {

    public Integer T; // Task duration
    public Integer N; // Task duration divide number
    public Double sliceSize; // size of time slice

    public Sampler(Integer T, Integer N) {
        this.T = T;
        this.N = N;
        this.sliceSize = (double) T / N;
    }

    public Double[] sampling(String distributionType, Map<String, Double> params) {
        AbstractRealDistribution distribution;
        Double[] samples = new Double[N + 1];

        switch (distributionType) {
            case "Exponential" ->
                // β: mean = 1 \ λ
                    distribution = new ExponentialDistribution(params.get("mean"));
            case "Weibull" ->
                // k: shape, λ: scale
                    distribution = new WeibullDistribution(params.get("shape"), params.get("scale"));
            case "Normal" ->
                // μ: mean, σ: sd (standard deviation)
                    distribution = new NormalDistribution(params.get("mean"), params.get("sd"));
            default -> throw new RuntimeException("Distribution type must in Exponential, Normal, Weibull.");
        }

        double sumProbability = 0.0;
        for (int i = 0; i < N; i++) {
            samples[i] = distribution.probability(i * sliceSize, (i+1) * sliceSize);
            sumProbability += samples[i];
        }
        samples[N] = 1 - sumProbability;

        return samples;
    }

    public Integer timeToSlice(Double timePoint) {
        return timePoint > T ? N + 1 : (int) Math.ceil(timePoint / sliceSize);
    }

}
