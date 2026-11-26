"""Fixture-only Java doubles: not mapped compilation or native framebuffer proof."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged'
FIXTURE = BASE / 'client/validation/NativeCorrectedMachineGuiValidation.java'


def method(source, signature):
    start = source.index(signature)
    brace = source.index('{', start)
    depth = 1
    end = brace + 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end]


class FreezerSnapshotHoldTest(unittest.TestCase):
    def test_hold_and_restore_actual_fixture_methods(self):
        source = FIXTURE.read_text()
        methods = method(source, 'private static void scheduleFreezerNearEnd(') + '\n' + method(source, 'static void restoreFreezerSnapshot(')
        java = r'''
import java.util.*;
import java.util.concurrent.*;
public class SnapshotCheck {
 static boolean freezerSnapshotHeld, freezerPreviouslyFrozen;
 static CompletableFuture<?> pending;
 static MachineBlockEntity serverMachine;
 static class Rate { boolean frozen; boolean isFrozen(){return frozen;} void setFrozen(boolean v){frozen=v;} }
 static class Server {
  Rate rate=new Rate(); ArrayDeque<Runnable> queue=new ArrayDeque<>(); ServerPlayer player=new ServerPlayer();
  Rate tickRateManager(){return rate;} Server getPlayerList(){return this;} ServerPlayer getPlayer(UUID id){return player;}
  CompletableFuture<Void> submit(Runnable action) {
   CompletableFuture<Void> f=new CompletableFuture<>() {
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
     drain(); return super.get(timeout,unit);
    }
   };
   queue.add(()->{try{action.run();f.complete(null);}catch(Throwable e){f.completeExceptionally(e);}}); return f;
  }
  void drain(){while(!queue.isEmpty())queue.remove().run();}
  void tick(){if(!rate.frozen) serverMachine.tick();}
 }
 static class Minecraft { Server server=new Server(); ServerPlayer player=server.player; Server getSingleplayerServer(){return server;} }
 static class ServerPlayer { Object containerMenu=new MachineMenu(); UUID getUUID(){return new UUID(0,1);} }
 static class MachineMenu { void broadcastChanges(){} }
 static class MachineBlocks { static final Object PHASE_0_4=new Object(); }
 static class BlockPos {}
 static class BlockState {
  int phase;
  boolean hasProperty(Object property){return property==MachineBlocks.PHASE_0_4;}
  BlockState setValue(Object property,int value){BlockState copy=new BlockState();copy.phase=value;return copy;}
 }
 static class Level {
  MachineBlockEntity entity;
  Level(MachineBlockEntity entity){this.entity=entity;}
  void setBlock(BlockPos pos,BlockState state,int flags){entity.state=state;}
 }
 static class MachineBlockEntity {
  static final int FREEZER_PROCESS_TICKS=180; int progress,energy; boolean fail;
  BlockState state=new BlockState(); Level level=new Level(this);
  MachineBlockEntity dataAccessForTests(){return this;}
  void set(int field,int value){if(fail)throw new IllegalStateException("setup");if(field==1)progress=value;else if(field==3)energy=value;}
  BlockState getBlockState(){return state;} Level getLevel(){return level;} BlockPos getBlockPos(){return new BlockPos();}
  void setChanged(){}
  // Deliberately small server model; production guards below pin these semantics.
  void tick(){if(energy<8)progress=0;else{energy-=8;progress++;if(progress>=180)progress=0;}}
 }
 static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
''' + methods + r'''
 public static void main(String[] args) throws Exception {
  // Original schedule without a freeze loses the snapshot after two server ticks.
  MachineBlockEntity old=new MachineBlockEntity();old.progress=178;old.energy=8;old.tick();old.tick();
  require(old.progress==0,"negative control must reproduce the lost window");
  for(boolean prior:new boolean[]{false,true}) {
   Minecraft mc=new Minecraft();mc.server.rate.frozen=prior;serverMachine=new MachineBlockEntity();
   scheduleFreezerNearEnd(mc);mc.server.drain();pending.join();
   require(mc.server.rate.frozen,"hold missing");
   require(serverMachine.progress==178 && serverMachine.energy==8 && serverMachine.state.phase==3,
      "near-end snapshot must keep progress, energy and server phase coherent");
   // One tick may already have been admitted before setFrozen changed the flag.
   serverMachine.tick();
   for(int i=0;i<600;i++)mc.server.tick();
   require(serverMachine.progress==179,"slow delivery lost snapshot");
   restoreFreezerSnapshot(mc);require(mc.server.rate.frozen==prior,"prior state lost");
   restoreFreezerSnapshot(mc);require(mc.server.rate.frozen==prior,"cleanup not idempotent");
  }
  // Cleanup queued before setup completes still runs after it on the server.
  Minecraft mc=new Minecraft();serverMachine=new MachineBlockEntity();scheduleFreezerNearEnd(mc);
  restoreFreezerSnapshot(mc);require(!mc.server.rate.frozen&&!freezerSnapshotHeld,"pending setup leaked freeze");
  // Setup failure after acquiring hold is recovered by the same terminal cleanup.
  mc=new Minecraft();serverMachine=new MachineBlockEntity();serverMachine.fail=true;scheduleFreezerNearEnd(mc);mc.server.drain();
  require(pending.isCompletedExceptionally(),"setup failure missing");restoreFreezerSnapshot(mc);
  require(!mc.server.rate.frozen&&!freezerSnapshotHeld,"setup error leaked freeze");
  // Both a timeout and a capture failure use the common terminal restore hook.
  for(String failure:new String[]{"timeout","capture"}) {
   mc=new Minecraft();serverMachine=new MachineBlockEntity();scheduleFreezerNearEnd(mc);mc.server.drain();
   try{throw new IllegalStateException(failure);}catch(Exception expected){restoreFreezerSnapshot(mc);}
   require(!mc.server.rate.frozen&&!freezerSnapshotHeld,"failure leaked freeze");
  }
 }
}
'''
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'SnapshotCheck.java'
            path.write_text(java)
            result = subprocess.run(['java', str(path)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_render_wait_and_terminal_cleanup_are_wired(self):
        source = FIXTURE.read_text()
        waiting = source.split('case WAIT_FREEZER_NEAR_END -> {')[1].split('case WAIT_FREEZER_CAPTURE -> {')[0]
        capture = source.split('case WAIT_FREEZER_CAPTURE -> {')[1].split('case WAIT_HOVER -> {')[0]
        self.assertIn('stage = Stage.WAIT_FREEZER_CAPTURE;', waiting)
        self.assertIn('ticks = 0;', waiting)
        self.assertNotIn('capture(mc', waiting)
        self.assertIn('if (++ticks < 3) return false;', capture)
        self.assertIn('menu.containerId == serverMenuId', capture)
        self.assertIn('menu.freezerProgressTicks() < MachineBlockEntity.FREEZER_PROCESS_TICKS', capture)
        self.assertIn('captureCurrentScreen(mc, menu, SPECS.get(specIndex), "near-end")', capture)
        self.assertLess(capture.index('captureCurrentScreen('), capture.index('restoreFreezerSnapshot(mc)'))
        self.assertLess(capture.index('restoreFreezerSnapshot(mc)'), capture.index('scheduleState(mc'))
        finish = method((BASE / 'client/validation/NativeClientMiningValidation.java').read_text(), 'private static void finish(')
        self.assertLess(finish.index('restoreFreezerSnapshot(mc)'), finish.index('mc.stop()'))
        self.assertIn('error.addSuppressed(cleanupError)', finish)
        self.assertIn('if (error == null) error = cleanupError;', finish)

    def test_model_is_pinned_to_current_production_energy_reset(self):
        source = (BASE / 'utility/MachineBlockEntity.java').read_text()
        tick = method(source, 'private void tickFreezer(')
        stop = method(source, 'private void stopFreezerProcess(')
        self.assertIn('private static final int FREEZER_ENERGY_PER_TICK = 8;', source)
        self.assertIn('if (powered)', tick)
        redstone_stop = tick.split('if (powered)', 1)[1].split('return;', 1)[0]
        self.assertIn('this.stopFreezerProcess();', redstone_stop)
        self.assertIn('if (this.energy < FREEZER_ENERGY_PER_TICK)', tick)
        energy_stop = tick.split('if (this.energy < FREEZER_ENERGY_PER_TICK)', 1)[1].split('return;', 1)[0]
        self.assertIn('this.stopFreezerProcess();', energy_stop)
        self.assertIn('this.energy -= FREEZER_ENERGY_PER_TICK;', tick)
        self.assertIn('this.progress++;', tick)
        self.assertIn('this.progress = 0;', stop)


if __name__ == '__main__':
    unittest.main()
