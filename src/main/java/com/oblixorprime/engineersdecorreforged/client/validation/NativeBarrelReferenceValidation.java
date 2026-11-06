package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.BarrelReferenceScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.lwjgl.glfw.GLFW;

/** Dedicated fresh-world opt-in; never enables or edits the other gameplay harness. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class NativeBarrelReferenceValidation {
   private static final String SUITE = "native-barrel-reference-v2";
   private static final BlockPos POS = new BlockPos(0,80,0);
   private enum Stage { BOOT, WORLD, SETUP, SLOT_READY, PICKED, RETURNED, CAPTURE, RELOAD_PREPARE, RELOAD_WAIT, RELOAD_SETTLE, RELOAD_DRAIN, CLOSE, REOPEN, DONE }
   private static Stage stage = Stage.BOOT;
   private static Path directory;
   private static String nonce;
   private static UUID playerId;
   private static long deadline;
   private static int ticks, slotIndex, captureIndex, serverMenuId, oldMenuId;
   private static boolean complete, postReload;
   private static CompletableFuture<Void> reloadFuture;
   private static CompletableFuture<ReloadServerSnapshot> reloadInspection;
   private static ReloadServerSnapshot beforeServer;
   private static Object beforeClientLevel, beforeClientMenu, beforeClientPlayer, beforeClientScreen;
   private static JsonObject beforeClient;
   private static long reloadDeadline;
   private static int reloadStableTicks;
   private record ReloadServerSnapshot(Object server, Object level, Object menu, Object blockEntity, Object player, JsonObject state) {}
   private static MachineBlockEntity machine;
   private static CompletableFuture<?> pending;
   private static CompletableFuture<Boolean> inspection;
   private static final JsonArray ROUTES = new JsonArray(), CAPTURES = new JsonArray();
   private static final JsonArray CASES = new JsonArray(), POST_RELOAD_ROUTES = new JsonArray();
   private static final JsonObject RELOAD = new JsonObject();
   private NativeBarrelReferenceValidation() {}

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      if (!Boolean.getBoolean("iedctr.barrelReferenceNative") || complete) return;
      Minecraft mc = Minecraft.getInstance();
      try {
         if (deadline == 0) deadline = System.nanoTime() + 900_000_000_000L;
         require(System.nanoTime() < deadline && ++ticks < 1800, "Native Barrel timeout at " + stage);
         if (stage == Stage.BOOT) {
            if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) boot(mc);
            return;
         }
         if (stage == Stage.WORLD) {
            if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
            require(mc.getSingleplayerServer() != null && mc.player.connection.getConnection().isMemoryConnection(), "Only isolated integrated worlds permitted");
            playerId = mc.player.getUUID();
            pending = mc.getSingleplayerServer().submit(() -> {
               var p = player(mc); p.setGameMode(GameType.SURVIVAL);
               var level = p.serverLevel();
               for(int x=-1;x<=1;x++) for(int z=2;z<=4;z++) level.setBlock(new BlockPos(x,80,z),Blocks.STONE.defaultBlockState(),3);
               p.connection.teleport(.5,81,3.5,180,0);
               level.setBlock(POS.below(),Blocks.STONE.defaultBlockState(),3);
               level.setBlock(POS,ModBlocks.FLUID_BARREL.get().defaultBlockState(),3);
               require(level.getBlockEntity(POS) instanceof MachineBlockEntity,"Missing Barrel entity");
               machine=(MachineBlockEntity)level.getBlockEntity(POS);
               p.openMenu(machine);
               require(p.containerMenu instanceof MachineMenu m && !m.barrelReferencePresentation() && m.slots.size()==38,"Server projection changed");
               serverMenuId=p.containerMenu.containerId;
            });
            enter(Stage.SETUP); return;
         }
         require(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null,"World disconnected");
         if (!ready()) return;
         // Poll the client reload without blocking its executor or depending on a
         // visible container screen. The future and loading overlay have separate lifetimes.
         if (stage == Stage.RELOAD_WAIT) {
            require(System.nanoTime()<reloadDeadline,"Resource reload/overlay timeout");
            require(reloadFuture!=null,"Missing actual resource reload future");
            if (!reloadFuture.isDone()) return;
            reloadFuture.join();
            RELOAD.addProperty("completed_count",1);
            if (mc.getOverlay()!=null) return;
            RELOAD.addProperty("overlay_dismissed",true);enter(Stage.RELOAD_SETTLE);return;
         }
         if (stage == Stage.RELOAD_SETTLE) {
            require(System.nanoTime()<reloadDeadline,"Post-reload settling timeout");
            if (mc.getOverlay()!=null) { reloadStableTicks=0;return; }
            if (++reloadStableTicks<12) return;
         }
         if (stage == Stage.CLOSE) {
            if (!serverMatches(mc,p->p.containerMenu==p.inventoryMenu)) return;
            CASES.add("close_reaches_server");
            pending=mc.getSingleplayerServer().submit(()->{player(mc).openMenu(machine);serverMenuId=player(mc).containerMenu.containerId;});
            enter(Stage.REOPEN);return;
         }
         if (!(mc.screen instanceof BarrelReferenceScreen screen) || !(mc.player.containerMenu instanceof MachineMenu menu) || menu.containerId!=serverMenuId) return;
         switch(stage) {
            case SETUP -> { verifyGeometry(screen,menu); prepareSlot(mc); }
            case SLOT_READY -> {
               if(ticks<8 || !matches(menu.getSlot(slotIndex).getItem(),fixtureItem()) || !menu.getCarried().isEmpty()) return;
               verifyGeometry(screen,menu); click(screen,slotIndex); enter(Stage.PICKED);
            }
            case PICKED -> {
               if(!menu.getSlot(slotIndex).getItem().isEmpty() || !matches(menu.getCarried(),fixtureItem())
                  || !serverMatches(mc,p->p.containerMenu.getSlot(slotIndex).getItem().isEmpty() && matches(p.containerMenu.getCarried(),fixtureItem()))) return;
               click(screen,destination());enter(Stage.RETURNED);
            }
            case RETURNED -> {
               if(!menu.getCarried().isEmpty() || !matches(menu.getSlot(destination()).getItem(),fixtureItem())
                  || !serverMatches(mc,p->p.containerMenu.getCarried().isEmpty() && matches(p.containerMenu.getSlot(destination()).getItem(),fixtureItem()) && total(p)==1)) return;
               (postReload?POST_RELOAD_ROUTES:ROUTES).add(slotIndex);
               if(++slotIndex<38) prepareSlot(mc);
               else { CASES.add(postReload?"post_reload_all_38_pickup_return_server_confirmed":"all_38_pickup_return_server_confirmed"); prepareFluid(mc); }
            }
            case CAPTURE -> {
               int amount=captureIndex==0?0:menu.fluidCapacity()*(captureIndex%2==1?1:2)/2;
               Fluid fluid=captureIndex==0?Fluids.EMPTY:fixtureFluid();
               if(menu.fluidAmount()!=amount || menu.storedFluid()!=fluid || ticks<12) return;
               verifyGeometry(screen,menu);
               String label=captureIndex==0?"empty":new String[]{"water","lava","milk"}[(captureIndex-1)/2]+(captureIndex%2==1?"-half":"-full");
               capture(mc,menu,(postReload?"post-reload-":"")+label,postReload?"post_reload":"baseline");
               if(++captureIndex<7) prepareFluid(mc);
               else if (!postReload) { CASES.add("native_empty_water_lava_milk_half_full");enter(Stage.RELOAD_PREPARE); }
               else { CASES.add("post_reload_native_empty_water_lava_milk_half_full");oldMenuId=menu.containerId;screen.onClose();enter(Stage.CLOSE); }
            }
            case RELOAD_PREPARE -> {
               if(mc.getOverlay()!=null) return;
               if(reloadInspection==null) { reloadInspection=mc.getSingleplayerServer().submit(()->serverSnapshot(mc));return; }
               if(!reloadInspection.isDone()) return;
               beforeServer=reloadInspection.join();reloadInspection=null;
               beforeClient=clientSnapshot(mc,menu);
               require(beforeClient.equals(beforeServer.state()),"Client/server state differs before reload");
               require(menu.storedFluid()==NeoForgeMod.MILK.value() && menu.fluidAmount()==menu.fluidCapacity()
                  && total(menu)==0 && inventoryItems(mc.player.getInventory())==0,"Reload requires full milk and empty slots/carried stack");
               beforeClientLevel=mc.level;beforeClientMenu=menu;beforeClientPlayer=mc.player;beforeClientScreen=screen;
               RELOAD.add("before",snapshots(beforeClient,beforeServer.state()));
               RELOAD.addProperty("method","Minecraft.reloadResourcePacks");RELOAD.addProperty("requested_count",1);
               RELOAD.addProperty("completed_count",0);
               reloadDeadline=System.nanoTime()+120_000_000_000L;
               reloadFuture=mc.reloadResourcePacks();enter(Stage.RELOAD_WAIT);
            }
            case RELOAD_SETTLE -> {
               if(reloadInspection==null) { reloadInspection=mc.getSingleplayerServer().submit(()->serverSnapshot(mc));return; }
               if(!reloadInspection.isDone()) return;
               var afterServer=reloadInspection.join();reloadInspection=null;
               var afterClient=clientSnapshot(mc,menu);
               require(mc.level==beforeClientLevel && menu==beforeClientMenu && mc.player==beforeClientPlayer && screen==beforeClientScreen
                  && afterServer.server()==beforeServer.server() && afterServer.level()==beforeServer.level(),"Reload replaced world or menu");
               require(afterServer.menu()==beforeServer.menu() && afterServer.blockEntity()==beforeServer.blockEntity() && afterServer.player()==beforeServer.player(),"Reload replaced server menu or Barrel");
               require(beforeClient.equals(afterClient) && beforeServer.state().equals(afterServer.state())
                  && afterClient.equals(afterServer.state()),"Reload changed client/server fluid, menu or inventory");
               verifyGeometry(screen,menu);
               RELOAD.add("after",snapshots(afterClient,afterServer.state()));
               RELOAD.addProperty("same_world_and_server",true);RELOAD.addProperty("same_menu_and_machine",true);
               RELOAD.addProperty("same_player_and_screen",true);
               RELOAD.addProperty("state_preserved",true);RELOAD.addProperty("settle_ticks",reloadStableTicks);
               capture(mc,menu,"reload-preserved-milk-full","reload_preserved");
               CASES.add("resource_reload_preserves_server_client_state");
               // Do not insert slot-0's empty bucket into the preserved full tank:
               // that would legitimately consume 1000 mB. Mutate only AFTER proof.
               pending=mc.getSingleplayerServer().submit(()->{
                  var tank=machine.fluidHandler(Direction.UP);var drained=tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
                  require(drained.getFluid()==NeoForgeMod.MILK.value() && drained.getAmount()==tank.getTankCapacity(0)
                     && tank.getFluidInTank(0).isEmpty(),"Post-reload fixture drain differs");
                  RELOAD.addProperty("fixture_drain_amount",drained.getAmount());
                  machine.setChanged();player(mc).containerMenu.broadcastChanges();
               });
               postReload=true;slotIndex=0;captureIndex=0;enter(Stage.RELOAD_DRAIN);
            }
            case RELOAD_DRAIN -> {
               if(menu.fluidAmount()!=0 || menu.storedFluid()!=Fluids.EMPTY) return;
               if(!serverMatches(mc,p->machine.fluidHandler(Direction.UP).getFluidInTank(0).isEmpty() && total(p)==0)) return;
               RELOAD.addProperty("fixture_drain_confirmed",true);prepareSlot(mc);
            }
            case REOPEN -> {
               if(ticks<12 || menu.containerId==oldMenuId) return;
               verifyGeometry(screen,menu);
               require(menu.fluidAmount()==menu.fluidCapacity() && menu.storedFluid()==NeoForgeMod.MILK.value(),"Reopen lost tank state");
               CASES.add("reopen_preserves_tank_and_geometry");enter(Stage.DONE);
            }
            case DONE -> finish(mc,null);
            default -> throw new IllegalStateException("Unexpected stage "+stage);
         }
      } catch(Throwable error) { finish(mc,error); }
   }
   private static void boot(Minecraft mc) throws Exception {
      require(!Boolean.getBoolean("iedctr.clientGameplay") && Boolean.getBoolean(MachineMenu.DEV_BARREL_REFERENCE_PROPERTY),"Dedicated fixture only; V5 must be enabled");
      nonce=System.getProperty("iedctr.clientGameplayNonce","");
      require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha","").matches("[a-f0-9]{40}"),"Missing nonce or exact revision");
      directory=mc.gameDirectory.toPath().toRealPath();
      require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory","")).toRealPath()) && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),"Isolation marker mismatch");
      require(mc.level==null && mc.getSingleplayerServer()==null && !Files.exists(directory.resolve("barrel-reference-screenshots")),"Refusing existing world/captures");
      verifyViewport(mc);
      if(Files.exists(directory.resolve("saves"))) try(var entries=Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(),"Existing saves forbidden"); }
      String name="iedctr-barrel-v5-"+nonce;
      mc.options.pauseOnLostFocus=false;
      enter(Stage.WORLD);
      mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
         new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
   }
   private static void prepareSlot(Minecraft mc) {
      pending=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);p.getInventory().clearContent();machine.clearContent();
         p.containerMenu.setCarried(ItemStack.EMPTY);
         p.containerMenu.getSlot(slotIndex).set(new ItemStack(fixtureItem()));p.containerMenu.broadcastChanges();
      });enter(Stage.SLOT_READY);
   }
   private static Item fixtureItem() { return slotIndex==0?Items.BUCKET:Items.STONE; }
   private static int destination() { return slotIndex==29?30:29; }
   private static boolean matches(ItemStack stack,Item item) { return stack.is(item) && stack.getCount()==1; }
   private static int total(ServerPlayer p) { return total(p.containerMenu); }
   private static int total(net.minecraft.world.inventory.AbstractContainerMenu menu) {
      int n=menu.getCarried().getCount();for(var slot:menu.slots)n+=slot.getItem().getCount();return n;
   }
   private static JsonObject state(int menuId,Fluid fluid,int amount,int capacity,int slots,int total,int inventoryItems,String dimension,UUID player) {
      var state=new JsonObject();state.addProperty("menu_id",menuId);state.addProperty("fluid_id",BuiltInRegistries.FLUID.getKey(fluid).toString());
      state.addProperty("amount",amount);state.addProperty("capacity",capacity);state.addProperty("slot_count",slots);
      state.addProperty("total_items",total);state.addProperty("inventory_items",inventoryItems);state.addProperty("dimension",dimension);state.addProperty("player_uuid",player.toString());return state;
   }
   private static int inventoryItems(Inventory inventory) {
      int n=0;for(int i=0;i<inventory.getContainerSize();i++)n+=inventory.getItem(i).getCount();return n;
   }
   private static JsonObject clientSnapshot(Minecraft mc,MachineMenu menu) {
      return state(menu.containerId,menu.storedFluid(),menu.fluidAmount(),menu.fluidCapacity(),menu.slots.size(),total(menu),inventoryItems(mc.player.getInventory()),
         mc.level.dimension().location().toString(),mc.player.getUUID());
   }
   private static ReloadServerSnapshot serverSnapshot(Minecraft mc) {
      var p=player(mc);var tank=machine.fluidHandler(Direction.UP);
      require(p.containerMenu instanceof MachineMenu menu && !menu.barrelReferencePresentation()
         && p.serverLevel().getBlockEntity(POS)==machine,"Authoritative Barrel/menu changed");
      var fluid=tank.getFluidInTank(0);
      return new ReloadServerSnapshot(mc.getSingleplayerServer(),p.serverLevel(),p.containerMenu,machine,p,
         state(p.containerMenu.containerId,fluid.getFluid(),fluid.getAmount(),tank.getTankCapacity(0),p.containerMenu.slots.size(),total(p),inventoryItems(p.getInventory()),
            p.serverLevel().dimension().location().toString(),p.getUUID()));
   }
   private static JsonObject snapshots(JsonObject client,JsonObject server) {
      var states=new JsonObject();states.add("client",client);states.add("server",server);return states;
   }
   private static Fluid fixtureFluid() {
      return switch((captureIndex-1)/2) { case 0->Fluids.WATER;case 1->Fluids.LAVA;default->{require(NeoForgeMod.MILK.isBound(),"Milk missing");yield NeoForgeMod.MILK.value();} };
   }
   private static void prepareFluid(Minecraft mc) {
      pending=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);p.getInventory().clearContent();machine.clearContent();
         var tank=machine.fluidHandler(Direction.UP);require(tank!=null && tank.getTanks()>0,"Missing tank");
         tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
         int amount=captureIndex==0?0:tank.getTankCapacity(0)*(captureIndex%2==1?1:2)/2;
         if(amount>0)require(tank.fill(new FluidStack(fixtureFluid(),amount),FluidAction.EXECUTE)==amount,"Actual fluid fill rejected");
         machine.setChanged();p.containerMenu.broadcastChanges();
      });
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),mc.getWindow().getScreenWidth()-2,mc.getWindow().getScreenHeight()-2);
      enter(Stage.CAPTURE);
   }
   private static void click(BarrelReferenceScreen screen,int index) {
      var a=BarrelReferenceGeometry.anchor(index);var v=screen.validationViewTransform();
      screen.mouseClicked(v.screenX(a.x()+8),v.screenY(a.y()+8),0);
      screen.mouseReleased(v.screenX(a.x()+8),v.screenY(a.y()+8),0);
   }
   private static void verifyGeometry(BarrelReferenceScreen screen,MachineMenu menu) {
      verifyViewport(Minecraft.getInstance());
      require(menu.barrelReferencePresentation() && menu.kind()==MachineKind.FLUID_BARREL && menu.slots.size()==38 && menu.visibleMachineSlots()==2,"Wrong native menu");
      var v=screen.validationViewTransform();
      int left=(int)Math.floor(v.screenX(0)),top=(int)Math.floor(v.screenY(0));
      require(screen.getGuiLeft()==left && screen.getGuiTop()==top
         && screen.getXSize()==(int)Math.ceil(v.screenX(BarrelReferenceGeometry.CANVAS_WIDTH))-left
         && screen.getYSize()==(int)Math.ceil(v.screenY(BarrelReferenceGeometry.CANVAS_HEIGHT))-top,
         "Public presentation bounds differ from transformed Barrel canvas");
      for(var a:BarrelReferenceGeometry.SLOT_ANCHORS) {
         var s=menu.getSlot(a.menuIndex());int index=a.menuIndex()<2?a.menuIndex():a.menuIndex()<29?a.menuIndex()+7:a.menuIndex()-29;
         require(s.getSlotIndex()==index && s.x==a.x() && s.y==a.y(),"Slot mapping differs");
         require(screen.validationSlotHoveredAt(a.menuIndex(),v.screenX(a.x()+8),v.screenY(a.y()+8)),"Centre not hoverable");
         require(!screen.validationSlotHoveredAt(a.menuIndex(),v.screenX(a.hoverLeft()-.01),v.screenY(a.y()+8)) && !screen.validationSlotHoveredAt(a.menuIndex(),v.screenX(a.hoverRight()+.01),v.screenY(a.y()+8)),"Horizontal boundary escaped");
         require(!screen.validationSlotHoveredAt(a.menuIndex(),v.screenX(a.x()+8),v.screenY(a.hoverTop()-.01)) && !screen.validationSlotHoveredAt(a.menuIndex(),v.screenX(a.x()+8),v.screenY(a.hoverBottom()+.01)),"Vertical boundary escaped");
      }
   }
   private static void verifyViewport(Minecraft mc) {
      int requested=Integer.getInteger("iedctr.requestedGuiScale",0);
      var window=mc.getWindow();double actual=window.getGuiScale();
      require(requested>=1 && requested<=3 && mc.options.guiScale().get()==requested && actual==requested,
         "Requested GUI scale was not applied");
      require(window.getWidth()==Integer.getInteger("iedctr.requestedWidth",0)
         && window.getHeight()==Integer.getInteger("iedctr.requestedHeight",0),"Requested framebuffer size was not applied");
      require(window.getGuiScaledWidth()==(int)Math.ceil(window.getWidth()/actual)
         && window.getGuiScaledHeight()==(int)Math.ceil(window.getHeight()/actual),"Scaled GUI dimensions differ");
   }
   private static void addViewport(JsonObject proof,Minecraft mc) {
      var window=mc.getWindow();
      proof.addProperty("requested_gui_scale",Integer.getInteger("iedctr.requestedGuiScale",0));
      proof.addProperty("actual_gui_scale",window.getGuiScale());
      proof.addProperty("gui_width",window.getGuiScaledWidth());proof.addProperty("gui_height",window.getGuiScaledHeight());
      proof.addProperty("framebuffer_width",window.getWidth());proof.addProperty("framebuffer_height",window.getHeight());
   }
   private static void capture(Minecraft mc,MachineMenu menu,String label,String phase) throws Exception {
      require(mc.getOverlay()==null,"Cannot capture a loading overlay");
      Path folder=directory.resolve("barrel-reference-screenshots");Files.createDirectories(folder);
      Path file=folder.resolve(label+".png");require(!Files.exists(file),"Stale capture");
      JsonObject c=new JsonObject();var screen=(BarrelReferenceScreen)mc.screen;var v=screen.validationViewTransform();
      c.addProperty("phase",phase);c.addProperty("fluid_id",BuiltInRegistries.FLUID.getKey(menu.storedFluid()).toString());
      c.addProperty("presentation_gui_left",screen.getGuiLeft());c.addProperty("presentation_gui_top",screen.getGuiTop());
      c.addProperty("presentation_gui_width",screen.getXSize());c.addProperty("presentation_gui_height",screen.getYSize());
      addViewport(c,mc);double guiScale=mc.getWindow().getGuiScale();
      try(var frame=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         frame.writeToFile(file);c.addProperty("width",frame.getWidth());c.addProperty("height",frame.getHeight());
         require(mc.getWindow().getWidth()==frame.getWidth() && mc.getWindow().getHeight()==frame.getHeight(),"Framebuffer capture size differs");
         // The renderer rounds its scissor in GUI coordinates before Minecraft
         // multiplies by the window GUI scale. Preserve that order for the crop.
         int x0=(int)(Math.ceil(v.screenX(170))*guiScale),y0=(int)(Math.ceil(v.screenY(115))*guiScale);
         int x1=(int)(Math.floor(v.screenX(211))*guiScale),y1=(int)(Math.floor(v.screenY(193))*guiScale);
         require(x0>=0 && y0>=0 && x1<=frame.getWidth() && y1<=frame.getHeight() && x1>x0 && y1>y0,"Invalid native tank bounds");
         Path cropFile=folder.resolve(label+"-tank.png");
         try(var crop=new NativeImage(x1-x0,y1-y0,false)) {
            for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++)crop.setPixelRGBA(x-x0,y-y0,frame.getPixelRGBA(x,y));
            crop.writeToFile(cropFile);
         }
         c.addProperty("tank_name",cropFile.getFileName().toString());c.addProperty("tank_sha256",hash(cropFile));
         c.addProperty("tank_x",x0);c.addProperty("tank_y",y0);
         c.addProperty("tank_width",x1-x0);c.addProperty("tank_height",y1-y0);
         // Hash a ring around the glass in native pixels, excluding all fluid pixels
         // plus a two-framebuffer-pixel safety band for raster boundary rounding.
         var digest=MessageDigest.getInstance("SHA-256");
         for(int y=(int)Math.ceil(v.screenY(106)*guiScale);y<(int)Math.floor(v.screenY(201)*guiScale);y++)
            for(int x=(int)Math.ceil(v.screenX(160)*guiScale);x<(int)Math.floor(v.screenX(220)*guiScale);x++) {
               if(x>=Math.floor(v.screenX(170)*guiScale)-2 && x<=Math.ceil(v.screenX(211)*guiScale)+2
                  && y>=Math.floor(v.screenY(115)*guiScale)-2 && y<=Math.ceil(v.screenY(193)*guiScale)+2)continue;
               int pixel=frame.getPixelRGBA(x,y);for(int b=0;b<4;b++)digest.update((byte)(pixel>>>(8*b)));
            }
         c.addProperty("frame_ring_sha256",HexFormat.of().formatHex(digest.digest()));
      }
      c.addProperty("name",file.getFileName().toString());c.addProperty("sha256",hash(file));
      c.addProperty("amount",menu.fluidAmount());c.addProperty("capacity",menu.fluidCapacity());
      c.addProperty("menu_id",menu.containerId);c.addProperty("screen_class",mc.screen.getClass().getName());c.addProperty("view_scale",v.scale());CAPTURES.add(c);
   }
   private static String hash(Path file)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
   private static void enter(Stage next){stage=next;ticks=0;inspection=null;}
   private static boolean ready(){if(pending==null)return true;if(!pending.isDone())return false;pending.join();pending=null;return true;}
   private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);require(p!=null,"Player vanished");return p;}
   private static boolean serverMatches(Minecraft mc,Predicate<ServerPlayer> test){if(inspection==null){inspection=mc.getSingleplayerServer().submit(()->test.test(player(mc)));return false;}if(!inspection.isDone())return false;boolean b=inspection.join();inspection=null;return b;}
   private static void finish(Minecraft mc,Throwable error) {
      complete=true;
      try {
         if(directory!=null) {
            var p=new JsonObject();p.addProperty("suite",SUITE);p.addProperty("status",error==null?"passed_bounded_smoke":"failed");
            p.addProperty("nonce",nonce);p.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));
            p.addProperty("selected_language",mc.getLanguageManager().getSelected());p.addProperty("source_art_sha256",BarrelReferenceGeometry.SOURCE_SHA256);
            addViewport(p,mc);
            p.addProperty("actual_integrated_world",mc.getSingleplayerServer()!=null);p.addProperty("native_visual_acceptance",false);p.addProperty("menu_slot_count",38);
            p.addProperty("input_method","Screen.mouseClicked/mouseReleased with vanilla packets and server observation; no OS mouse input");
            p.add("slot_routes",ROUTES);p.add("post_reload_slot_routes",POST_RELOAD_ROUTES);p.add("resource_reload",RELOAD);
            p.add("captures",CAPTURES);p.add("cases",CASES);
            if(error!=null)p.addProperty("error",error.toString());
            Files.writeString(directory.resolve("client-barrel-reference-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(p)+"\n");
         }
      }catch(Exception writeError){EngineersDecorReforged.LOGGER.error("Barrel proof write failed",writeError);}
      if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_BARREL_REFERENCE PASSED nonce={}",nonce);
      else EngineersDecorReforged.LOGGER.error("IEDCTR_BARREL_REFERENCE FAILED at "+stage,error);
      mc.stop();
   }
   private static void require(boolean b,String message){if(!b)throw new IllegalStateException(message);}
}
