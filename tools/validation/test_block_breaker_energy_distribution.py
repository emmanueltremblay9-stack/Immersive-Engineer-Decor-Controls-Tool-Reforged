"""Execute the production FE apportionment method, including overflow boundaries."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java'


def method():
    source = SOURCE.read_text()
    start = source.index('   private int blockBreakerEnergyCostPerTick(int totalCost,')
    return source[start:source.index('   private int blockBreakerEnergyInputRemaining()', start)]


PROBE = '''public class EnergyProbe {
 static final class Mth { static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));} }
 /* METHOD */
 public static void main(String[] args) {
  var probe = new EnergyProbe();
  for (int cost : new int[]{0,240,241,32000,Integer.MAX_VALUE}) {
   for (int duration : new int[]{1,20,21,40,80,1000,Integer.MAX_VALUE}) {
    for (int elapsed : new int[]{-1,0,1,duration/2,duration-1,Integer.MAX_VALUE}) {
     int clamped = Mth.clamp(elapsed,0,duration-1);
     long expected = (long)cost*(clamped+1L)/duration-(long)cost*clamped/duration;
     int actual = probe.blockBreakerEnergyCostPerTick(cost,elapsed,duration);
     if (actual != expected) throw new AssertionError("FE apportionment overflow/mismatch");
    }
    if (duration <= 1000) {
     long sum=0;
     for(int tick=0;tick<duration;tick++)sum+=probe.blockBreakerEnergyCostPerTick(cost,tick,duration);
     if(sum!=cost)throw new AssertionError("Total FE changed");
    }
   }
  }
  if(probe.blockBreakerEnergyCostPerTick(240,1,0)!=0 || probe.blockBreakerEnergyCostPerTick(240,1,-1)!=0)
   throw new AssertionError("Invalid duration guard");
 }
}'''


def run(source):
    with tempfile.TemporaryDirectory(prefix='breaker-energy-') as directory:
        path = Path(directory) / 'EnergyProbe.java'
        path.write_text(PROBE.replace('/* METHOD */', source))
        compile_result = subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main',
                                         '-d', directory, str(path)], capture_output=True, text=True)
        if compile_result.returncode:
            raise AssertionError(compile_result.stderr)
        return subprocess.run(['java', '-cp', directory, 'EnergyProbe'], capture_output=True, text=True)


class EnergyDistributionTests(unittest.TestCase):
    def test_production_method_preserves_cost_and_boundaries(self):
        result = run(method())
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_int_multiplication_regression_is_rejected(self):
        source = method()
        mutated = source.replace('(long)totalCost * elapsed', 'totalCost * elapsed')
        self.assertNotEqual(source, mutated)
        result = run(mutated)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('FE apportionment overflow/mismatch', result.stderr)


if __name__ == '__main__':
    unittest.main()
