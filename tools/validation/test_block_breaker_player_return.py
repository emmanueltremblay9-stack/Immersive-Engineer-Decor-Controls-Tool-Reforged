"""Execute the exact native-driver slot selector against recording container doubles."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
DRIVER = ROOT / 'src/factoryPlacerNativeValidation/java/com/oblixorprime/iedctrtest/factoryplacer/BlockBreakerPackagedValidation.java'


def selector():
    text = DRIVER.read_text()
    return text[text.index('    private static int playerReturnSlot('):text.index('    private static void clickSlot(')]


PROBE = '''import java.util.List;
public class PlayerReturnProbe {
 static final class Inventory {}
 static final class ItemStack { boolean empty; ItemStack(boolean e){empty=e;} boolean isEmpty(){return empty;} }
 static final class Slot {
  Object container; boolean active=true, accepts; ItemStack item=new ItemStack(true);
  Slot(Object c,boolean a){container=c;accepts=a;}
  boolean isActive(){return active;} boolean mayPlace(ItemStack s){return accepts;} ItemStack getItem(){return item;}
 }
 static final class MachineMenu {
  List<Slot> slots; MachineMenu(Slot... s){slots=List.of(s);} Slot getSlot(int i){return slots.get(i);}
 }
 static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
 /* SELECTOR */
 static void rejects(MachineMenu menu,Inventory inventory,ItemStack carried,String reason){
  try {playerReturnSlot(menu,inventory,carried);} catch(IllegalStateException expected){return;}
  throw new AssertionError(reason);
 }
 public static void main(String[] args){
  var inventory=new Inventory();var machine=new Object();var carried=new ItemStack(false);
  var output=new Slot(machine,false);var otherMachineSlot=new Slot(machine,true);
  var player=new Slot(inventory,true);var inactive=new Slot(inventory,true);inactive.active=false;
  var fallback=new Slot(inventory,true);
  var menu=new MachineMenu(output,otherMachineSlot,player,inactive,fallback);
  if(playerReturnSlot(menu,inventory,carried)!=2)throw new AssertionError("Not a player destination");
  player.item=new ItemStack(false);
  if(playerReturnSlot(menu,inventory,carried)!=4)throw new AssertionError("Occupied/inactive destination accepted");
  fallback.accepts=false;rejects(menu,inventory,carried,"Noninsertable destination accepted");
  player.item=new ItemStack(true);output.accepts=true;
  rejects(menu,inventory,carried,"Insertable output accepted");
  System.out.println("Player return routing passed");
 }
}
'''


def run(method):
    with tempfile.TemporaryDirectory(prefix='breaker-return-') as directory:
        target = Path(directory) / 'PlayerReturnProbe.java'
        target.write_text(PROBE.replace('/* SELECTOR */', method))
        compiled = subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-source','21','-target','21',
                                   '-d',directory,str(target)],capture_output=True,text=True)
        if compiled.returncode: raise AssertionError(compiled.stderr)
        return subprocess.run(['java','-cp',directory,'PlayerReturnProbe'],capture_output=True,text=True)


class PlayerReturnTests(unittest.TestCase):
    def test_actual_selector_obeys_output_and_player_roles(self):
        result=run(selector())
        self.assertEqual(result.returncode,0,result.stderr)

    def test_output_as_destination_is_rejected(self):
        result=run(selector().replace('return i;', 'return 0;'))
        self.assertNotEqual(result.returncode,0)
        self.assertIn('Not a player destination',result.stderr)

    def test_output_noninsertability_guard_cannot_be_removed(self):
        result=run(selector().replace('require(!menu.getSlot(0).mayPlace(carried), "Output must reject insertion");',''))
        self.assertNotEqual(result.returncode,0)
        self.assertIn('Insertable output accepted',result.stderr)

    def test_picked_stage_clicks_verified_player_destination(self):
        source=DRIVER.read_text()
        picked=source[source.index('if (stage == Stage.PICKED)'):source.index('if (stage == Stage.RETURNED)')]
        self.assertIn('returnMenuSlot = playerReturnSlot(menu, mc.player.getInventory(), menu.getCarried())',picked)
        self.assertIn('clickSlot(screen, menu, returnMenuSlot)',picked)
        self.assertNotIn('clickSlot(screen, menu, 0)',picked)


if __name__=='__main__':unittest.main()
