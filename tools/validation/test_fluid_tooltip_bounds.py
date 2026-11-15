"""Execute the exact positioner against recording API doubles; not a native render test."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[2]
POSITIONER=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/FluidTooltipPositioner.java'

STUBS={
'org/joml/Vector2ic.java':'package org.joml; public interface Vector2ic { int x(); int y(); }',
'org/joml/Vector2i.java':'package org.joml; public record Vector2i(int x,int y) implements Vector2ic {}',
'net/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner.java':'''package net.minecraft.client.gui.screens.inventory.tooltip;
public interface ClientTooltipPositioner { org.joml.Vector2ic positionTooltip(int sw,int sh,int mx,int my,int w,int h); }''',
'net/minecraft/client/gui/screens/inventory/tooltip/DefaultTooltipPositioner.java':'''package net.minecraft.client.gui.screens.inventory.tooltip;
public final class DefaultTooltipPositioner implements ClientTooltipPositioner {
 public static final DefaultTooltipPositioner INSTANCE=new DefaultTooltipPositioner();
 public static int x,y,calls;
 public org.joml.Vector2ic positionTooltip(int sw,int sh,int mx,int my,int w,int h) {
  calls++; return new org.joml.Vector2i(x,y);
 }
}''',
'com/oblixorprime/engineersdecorreforged/client/FluidTooltipBoundsProbe.java':'''package com.oblixorprime.engineersdecorreforged.client;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
public class FluidTooltipBoundsProbe {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 static void positioned(int sw,int sh,int w,int h,int x,int y,int ex,int ey){
  DefaultTooltipPositioner.x=x;DefaultTooltipPositioner.y=y;int before=DefaultTooltipPositioner.calls;
  var out=FluidTooltipPositioner.INSTANCE.positionTooltip(sw,sh,200,36,w,h);
  check(DefaultTooltipPositioner.calls==before+1,"Must delegate native position first");
  check(out.x()==ex&&out.y()==ey,"Unexpected position");
  check(out.x()-4>=0&&out.y()-4>=0&&out.x()+w+4<=sw&&out.y()+h+4<=sh,"Full painted frame outside viewport");
 }
 public static void main(String[]args){
  // Exact observed FR header origin/viewport: text can fit while its frame does not.
  positioned(400,300,188,8,212,24,208,24);
  positioned(400,300,187,8,212,24,209,24);
  // No movement when the entire frame already fits, including collection at x=4.
  positioned(320,270,240,28,4,20,4,20);
  positioned(1280,720,188,8,400,300,400,300);
  // Native top/bottom overflow and left/right margins, independently and combined.
  positioned(400,300,100,30,-2,-12,4,4);
  positioned(400,300,100,30,12,267,12,266);
  positioned(320,270,240,60,100,240,76,206);
  positioned(320,270,312,262,100,100,4,4);
  for(int sw:new int[]{320,400,1280})for(int sh:new int[]{270,300,720})
   for(int w:new int[]{1,20,sw-8})for(int h:new int[]{1,8,sh-8})
    for(int x:new int[]{-100,0,4,sw,sw+100})for(int y:new int[]{-100,0,4,sh,sh+100})
     positioned(sw,sh,w,h,x,y,Math.max(4,Math.min(x,sw-w-4)),Math.max(4,Math.min(y,sh-h-4)));
  System.out.println("Fluid tooltip full-frame bounds passed");
 }
}'''
}

def probe(source):
    with tempfile.TemporaryDirectory(prefix='fluid-tooltip-bounds-') as folder:
        root=Path(folder)
        files=dict(STUBS)
        files['com/oblixorprime/engineersdecorreforged/client/FluidTooltipPositioner.java']=source
        paths=[]
        for name,contents in files.items():
            target=root/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(contents);paths.append(str(target))
        result=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-source','21','-target','21','-d',str(root),*paths],capture_output=True,text=True)
        if result.returncode:raise AssertionError(result.stderr)
        return subprocess.run(['java','-cp',str(root),'com.oblixorprime.engineersdecorreforged.client.FluidTooltipBoundsProbe'],capture_output=True,text=True)

class FluidTooltipBoundsTests(unittest.TestCase):
    def test_native_placement_preserved_unless_frame_overflows(self):
        result=probe(POSITIONER.read_text());self.assertEqual(result.returncode,0,result.stderr)
    def test_text_only_bounds_regression_rejected(self):
        result=probe(POSITIONER.read_text().replace('FRAME_MARGIN = 4','FRAME_MARGIN = 0'))
        self.assertNotEqual(result.returncode,0)
    def test_three_pixel_padding_without_outer_strip_rejected(self):
        result=probe(POSITIONER.read_text().replace('FRAME_MARGIN = 4','FRAME_MARGIN = 3'))
        self.assertNotEqual(result.returncode,0)
    def test_vertical_clamp_cannot_be_removed(self):
        result=probe(POSITIONER.read_text().replace('clampOrigin(nativePosition.y(), screenHeight, tooltipHeight)','nativePosition.y()'))
        self.assertNotEqual(result.returncode,0)

if __name__=='__main__':unittest.main()
