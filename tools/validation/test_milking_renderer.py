"""Compile the real active artwork renderer with recording doubles, not Minecraft."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from check_milking_qualification import check_textless_milking_labels, SmokeFailure

ROOT=Path(__file__).resolve().parents[2]
STUBS={
'net/minecraft/resources/ResourceLocation.java':'''package net.minecraft.resources;
public record ResourceLocation(String namespace,String path) {
 public static ResourceLocation fromNamespaceAndPath(String n,String p){return new ResourceLocation(n,p);}
}''',
'com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java':'''package com.oblixorprime.engineersdecorreforged.menu;
public record MachineMenu(int fluidAmount,int fluidCapacity) {public int energyStored(){return 0;} public int energyCapacity(){return 0;}}
''',
'com/oblixorprime/engineersdecorreforged/utility/BlockBreakerRedesignLayout.java':'''package com.oblixorprime.engineersdecorreforged.utility;
public class BlockBreakerRedesignLayout {public static final int IMAGE_WIDTH=1;public static final R FE=new R();
 public static class R{public int x(){return 0;}public int y(){return 0;}public int right(){return 1;}public int bottom(){return 1;}public int width(){return 1;}public int height(){return 1;}}}
''',
'net/minecraft/client/gui/GuiGraphics.java':'''package net.minecraft.client.gui;
import java.util.*;import net.minecraft.resources.ResourceLocation;
public class GuiGraphics {
 public final List<String> calls=new ArrayList<>();
 public void enableScissor(int a,int b,int c,int d){calls.add("clip:"+a+","+b+","+c+","+d);}
 public void disableScissor(){calls.add("unclip");}
 public void fill(int a,int b,int c,int d,int e){throw new AssertionError("Unexpected invented fill");}
 public void blit(ResourceLocation t,int x,int y,int w,int h,float u,float v,int sw,int sh,int tw,int th){
  calls.add("blit:"+t.namespace()+":"+t.path()+":"+x+","+y+","+w+","+h+","+(int)u+","+(int)v+","+sw+","+sh+","+tw+","+th);}
}''',
'com/oblixorprime/engineersdecorreforged/client/MilkingRendererProbe.java':'''package com.oblixorprime.engineersdecorreforged.client;
import java.util.*;import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MilkingRedesignLayout;
import net.minecraft.client.gui.GuiGraphics;
public class MilkingRendererProbe {
 public static void main(String[] args){
  for(int amount:new int[]{-1,0,1,2000,3999,4000,5000}) for(int capacity:new int[]{0,4000}){
   int fill=capacity<=0||amount<=0?0:Math.min(amount,capacity)*51/capacity;
   var g=new GuiGraphics();RedesignedMachineArtwork.milking(g,new MachineMenu(amount,capacity),526,16);
   var expected=new ArrayList<String>();String root="blit:immersive_engineer_decor_controls_tool_reforged:textures/gui/milking_redesign_v1/";
   expected.add(root+"background.png:526,16,228,228,0,0,1254,1254,1254,1254");
   if(fill>0){expected.add("clip:630,"+(116-fill)+",652,116");expected.add(root+"milk.png:630,65,22,51,479,228,296,802,1254,1254");expected.add("unclip");}
   if(!g.calls.equals(expected))throw new AssertionError(g.calls+" != "+expected);
   if(MilkingRedesignLayout.fillPixels(amount,capacity,51)!=fill)throw new AssertionError("Layout/active renderer fill mismatch");
  }
  System.out.println("PASS actual active renderer: 14 amount/capacity cases, background+milk UVs, bottom-up scissor, no glass or invented state; recording doubles only");
 }
}'''
}


class MilkingRendererTest(unittest.TestCase):
    def test_active_screen_has_no_permanent_milking_text(self):
        screen=(ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java').read_text()
        check_textless_milking_labels(screen)

    def test_permanent_text_or_mask_regressions_are_rejected(self):
        path=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java'
        screen=path.read_text()
        start=screen.index('protected void renderLabels(')
        end=screen.index('private void drawSmallFreezerTexture(',start)
        before,labels,after=screen[:start],screen[start:end],screen[end:]
        for replacement in ('', 'drawSmallMilkingMachineLabels(graphics);',
                            'graphics.drawString(font, "Cow", 0, 0, 0); return;',
                            'graphics.fill(0, 0, 228, 24, 0xff000000); return;'):
            with self.subTest(replacement=replacement):
                mutated=before+labels.replace('return;',replacement,1)+after
                with self.assertRaisesRegex(SmokeFailure,'permanent text or masks'):
                    check_textless_milking_labels(mutated)

    def test_drawing_before_milking_early_return_is_rejected(self):
        path=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java'
        screen=path.read_text()
        start=screen.index('protected void renderLabels(')
        brace=screen.index('{',start)+1
        mutated=screen[:brace]+' graphics.drawString(font, "Cow", 0, 0, 0); '+screen[brace:]
        with self.assertRaisesRegex(SmokeFailure,'permanent text or masks'):
            check_textless_milking_labels(mutated)

    def test_actual_active_renderer_recorded_calls(self):
        with tempfile.TemporaryDirectory(prefix='milking-renderer-') as tmp:
            root=Path(tmp);sources=[]
            for name,text in STUBS.items():
                p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);sources.append(p)
            j=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged'
            sources.extend([j/'client/RedesignedMachineArtwork.java',j/'utility/MilkingRedesignLayout.java'])
            subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-source','21','-target','21','-d',str(root),*map(str,sources)],check=True)
            subprocess.run(['java','-cp',str(root),'com.oblixorprime.engineersdecorreforged.client.MilkingRendererProbe'],check=True)


if __name__=='__main__':unittest.main()
