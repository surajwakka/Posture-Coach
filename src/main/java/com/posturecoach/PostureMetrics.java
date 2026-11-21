package com.posturecoach;

import ai.djl.modality.cv.output.Joints.Joint;
import java.util.*;

/** Geometry on COCO keypoints: 0 nose, 1/2 eyes, 5/6 shoulders. */
public record PostureMetrics(boolean detected, double shoulderTiltDeg, double distanceIndex,
                             double headDropRatio, List<String> issues) {

    static final double MIN_CONF = 0.3;
    static final double MAX_TILT_DEG = 8;        // shoulder tilt threshold
    static final double TOO_CLOSE_INDEX = 0.16;  // eye distance / frame width (rough proxy for screen distance)
    static final double SLOUCH_RATIO = 0.55;     // (shoulderMidY - noseY) / shoulderWidth

    static PostureMetrics from(List<Joint> j, int w, int h) {
        if (j.size() < 7) return none();
        Joint nose = j.get(0), le = j.get(1), re = j.get(2), ls = j.get(5), rs = j.get(6);
        for (Joint p : List.of(nose, le, re, ls, rs)) if (p.getConfidence() < MIN_CONF) return none();

        double lx = ls.getX() * w, ly = ls.getY() * h, rx = rs.getX() * w, ry = rs.getY() * h;
        double shoulderW = Math.hypot(lx - rx, ly - ry);
        double tilt = Math.toDegrees(Math.atan2(ly - ry, lx - rx));
        tilt = Math.abs(tilt > 90 ? tilt - 180 : tilt < -90 ? tilt + 180 : tilt);

        double eyeDist = Math.hypot((le.getX() - re.getX()) * w, (le.getY() - re.getY()) * h);
        double distIdx = eyeDist / w;

        double midY = (ly + ry) / 2.0;
        double headDrop = shoulderW == 0 ? 1 : (midY - nose.getY() * h) / shoulderW;

        List<String> issues = new ArrayList<>();
        if (tilt > MAX_TILT_DEG) issues.add("TILT");
        if (distIdx > TOO_CLOSE_INDEX) issues.add("TOO_CLOSE");
        if (headDrop < SLOUCH_RATIO) issues.add("SLOUCH");
        return new PostureMetrics(true, round(tilt), round(distIdx), round(headDrop), issues);
    }

    static PostureMetrics none() { return new PostureMetrics(false, 0, 0, 0, List.of()); }
    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
