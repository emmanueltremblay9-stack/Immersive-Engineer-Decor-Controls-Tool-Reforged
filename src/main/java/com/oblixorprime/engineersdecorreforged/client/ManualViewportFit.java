package com.oblixorprime.engineersdecorreforged.client;

/** Pure, entry-scoped physical-scale ceiling. Never enlarges or edits user preferences. */
public final class ManualViewportFit {
    private ManualViewportFit() {}
    public static double fit(String namespace, double requested, int width, int height, int bookWidth, int bookHeight) {
        if (!"immersive_engineer_decor_controls_tool_reforged".equals(namespace)
                || !Double.isFinite(requested) || requested <= 0 || width <= 0 || height <= 0
                || bookWidth <= 0 || bookHeight <= 0) return requested;
        double ceiling = Math.min((double)width/bookWidth, (double)height/bookHeight);
        // Fractional scale only for a physical window smaller than the book at scale one.
        double fitting = ceiling >= 1 ? Math.floor(ceiling) : ceiling;
        return Math.min(requested, fitting);
    }
}
