package com.oblixorprime.engineersdecorreforged.client;

public final class ManualViewportFitTest {
    @org.junit.Test public void boundedScale() {
        String own="immersive_engineer_decor_controls_tool_reforged";
        for(int w:new int[]{100,320,1280,1920}) for(int h:new int[]{100,260,480,720,1080})
            for(double s:new double[]{1,2,3,4,6}) {
                double f=ManualViewportFit.fit(own,s,w,h,186,198);
                org.junit.Assert.assertTrue(f>0 && f<=s && f*186<=w+.0001 && f*198<=h+.0001);
                org.junit.Assert.assertEquals(s,ManualViewportFit.fit("immersiveengineering",s,w,h,186,198),0);
                org.junit.Assert.assertEquals(s,ManualViewportFit.fit("other_addon",s,w,h,186,198),0);
                org.junit.Assert.assertEquals(s,ManualViewportFit.fit(null,s,w,h,186,198),0);
            }
        org.junit.Assert.assertEquals(3,ManualViewportFit.fit(own,4,1280,720,186,198),0);
        org.junit.Assert.assertEquals(2,ManualViewportFit.fit(own,2,1280,480,186,198),0);
        org.junit.Assert.assertEquals(4,ManualViewportFit.fit(own,4,1920,1080,186,198),0);
    }
}
