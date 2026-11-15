"""Execute exact boundary guard; source checks complement, never replace native CI."""
from pathlib import Path
import subprocess
import tempfile
import unittest
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'src/tfcGameTest/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeTfcMilkingRecoveryValidation.java'

def guard():
    text=SOURCE.read_text();start=text.index('   private static boolean nativeInputMarkerMatches(')
    return text[start:text.index('   private static void initNativeInput()',start)]

def execute(method):
    fixture='''public class Probe {METHOD
      public static void main(String[] args){String n="a".repeat(32);
        if(!nativeInputMarkerMatches(":99",n+"\\n:99",n)||!nativeInputMarkerMatches(":99.0",n+"\\n:99.0",n))throw new AssertionError();
        for(String d:new String[]{null,"localhost:99",":99;bad","",":-1"})if(nativeInputMarkerMatches(d,n+"\\n"+d,n))throw new AssertionError();
        if(nativeInputMarkerMatches(":99",n+"\\n:100",n)||nativeInputMarkerMatches(":99",n+"\\n:99","b".repeat(32))||nativeInputMarkerMatches(":99","\\n:99","")||nativeInputMarkerMatches(":99",null,n))throw new AssertionError();
      }}'''.replace('METHOD',method)
    with tempfile.TemporaryDirectory() as tmp:
        file=Path(tmp)/'Probe.java';file.write_text(fixture)
        subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main',str(file)],check=True,capture_output=True)
        return subprocess.run(['java','-cp',tmp,'Probe'],capture_output=True).returncode

class NativeInputBoundaryTest(unittest.TestCase):
    def test_boundary_and_mutation(self):
        self.assertEqual(execute(guard()),0)
        self.assertNotEqual(execute(guard().replace('(runNonce+"\\n"+displayName).equals(marker)','true')),0)
    def test_no_bypassed_hover_or_callback(self):
        s=SOURCE.read_text()
        self.assertNotIn('glfwSetCursorPos(',s)
        self.assertNotIn('mouseHandler.onMove',s)
        self.assertIn('hoverTicks=actualRecoveryHover(mc,screen)?hoverTicks+1:0',s)
        self.assertIn('ticks<120 || hoverTicks>=8',s)
        self.assertIn('isolatedDirectoryValidated && nativeInputMarkerMatches',s)
        self.assertLess(s.index('isolatedDirectoryValidated && nativeInputMarkerMatches'),s.index('xDisplay=x11.getFunction("XOpenDisplay")'))
        self.assertIn('wx[0]+(int)Math.round(x*mc.getWindow().getScreenWidth()/screen.width)',s)
        driver=(ROOT/'tools/validation/tfc_milking_recovery_native.py').read_text()
        self.assertIn('assert not p.exists()',driver)
        self.assertIn('[sys.executable,str(bootstrap)]+gradle',driver)
