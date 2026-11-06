package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.SolarV2ReferenceScreen;
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
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** Dedicated fresh-world opt-in; never enables or edits the other gameplay harness. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class NativeSolarV2ReferenceValidation {
   private static final String SUITE = "native-solar-v2-reference-v1";
   private static final BlockPos POS = new BlockPos(0,80,0);
   private enum Stage { BOOT, WORLD, SETUP, CELL_READY, CELL_PICKED, CELL_CHARGING, SLOT_READY, PICKED, RETURNED, CAPTURE, CLOSE, REOPEN, DONE }
   private static Stage stage = Stage.BOOT;
   private static Path directory;
   private static String nonce;
   private static UUID playerId;
   private static long deadline;
   private static int ticks, slotIndex, captureIndex, serverMenuId, oldMenuId;
   private static boolean complete;
   private static int chargedCellFe, remainingBufferFe;
   private static MachineBlockEntity machine;
   private static CompletableFuture<?> pending;
   private static CompletableFuture<Boolean> inspection;
   private static final JsonArray ROUTES = new JsonArray(), CAPTURES = new JsonArray();
   private static final JsonArray CASES = new JsonArray();
   private NativeSolarV2ReferenceValidation() {}

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      if (!Boolean.getBoolean("iedctr.solarV2ReferenceNative") || complete) return;
      Minecraft mc = Minecraft.getInstance();
      try {
         if (deadline == 0) deadline = System.nanoTime() + 900_000_000_000L;
         require(System.nanoTime() < deadline && ++ticks < 1800, "Native Solar V2 timeout at " + stage);
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
               level.setBlock(POS,ModBlocks.SMALL_SOLAR_PANEL.get().defaultBlockState(),3);
               level.setBlock(POS.above(2),Blocks.STONE.defaultBlockState(),3);
               require(level.getBlockEntity(POS) instanceof MachineBlockEntity,"Missing Solar V2 entity");
               machine=(MachineBlockEntity)level.getBlockEntity(POS);
               p.openMenu(machine);
               require(p.containerMenu instanceof MachineMenu m && !m.solarV2ReferencePresentation() && m.slots.size()==37,"Server projection changed");
               serverMenuId=p.containerMenu.containerId;
            });
            enter(Stage.SETUP); return;
         }
         require(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null,"World disconnected");
         if (!ready()) return;
         if (stage == Stage.CLOSE) {
            if (!serverMatches(mc,p->p.containerMenu==p.inventoryMenu)) return;
            CASES.add("close_reaches_server");
            pending=mc.getSingleplayerServer().submit(()->{player(mc).openMenu(machine);serverMenuId=player(mc).containerMenu.containerId;});
            enter(Stage.REOPEN);return;
         }
         if (!(mc.screen instanceof SolarV2ReferenceScreen screen) || !(mc.player.containerMenu instanceof MachineMenu menu) || menu.containerId!=serverMenuId) return;
         switch(stage) {
            case SETUP -> { verifyGeometry(screen,menu); prepareCell(mc); }
            case CELL_READY -> {
               if(ticks<8 || !matches(menu.getSlot(28).getItem(),cellItem())) return;
               // The client mirror uses SimpleContainer; only the server's machine-backed
               // slots authoritatively decide acceptance. Fail closed before either click.
               if(!serverMatches(mc,p->{
                  require(p.containerMenu instanceof MachineMenu,"Cell acceptance: server menu is "+p.containerMenu.getClass().getName());
                  var serverMenu=(MachineMenu)p.containerMenu;
                  require(serverMenu.kind()==MachineKind.SMALL_SOLAR_PANEL && serverMenu.containerId==serverMenuId
                     && serverMenu.slots.size()==37,"Cell acceptance: wrong server menu identity (kind="+serverMenu.kind()
                     +", id="+serverMenu.containerId+", expectedId="+serverMenuId+", slots="+serverMenu.slots.size()+")");
                  require(!p.containerMenu.getSlot(0).mayPlace(new ItemStack(Items.STONE)),
                     "Cell acceptance: authoritative server Cell slot accepted minecraft:stone");
                  require(p.containerMenu.getSlot(0).mayPlace(new ItemStack(cellItem())),
                     "Cell acceptance: authoritative server Cell slot rejected immersiveengineering:capacitor_lv");
                  return true;
               }))return;
               click(screen,28);enter(Stage.CELL_PICKED);
            }
            case CELL_PICKED -> {
               if(!matches(menu.getCarried(),cellItem()) || !serverMatches(mc,p->matches(p.containerMenu.getCarried(),cellItem()))) return;
               click(screen,0);enter(Stage.CELL_CHARGING);
            }
            case CELL_CHARGING -> {
               if(!menu.getCarried().isEmpty() || !matches(menu.getSlot(0).getItem(),cellItem()))return;
               if(!serverMatches(mc,p->{
                  var cell=machine.getItem(0).getCapability(EnergyStorage.ITEM);
                  if(cell==null || cell.getEnergyStored()<=0)return false;
                  chargedCellFe=cell.getEnergyStored();remainingBufferFe=machine.energyStorage(null).getEnergyStored();
                  require(chargedCellFe+remainingBufferFe==1000 && total(p)==1,"Cell charging did not conserve fixture FE/items");return true;
               }))return;
               CASES.add("cell_acceptance_and_real_fe_charging_server_confirmed");prepareSlot(mc);
            }
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
               ROUTES.add(slotIndex);
               if(++slotIndex<37) prepareSlot(mc);
               else { CASES.add("all_37_pickup_return_server_confirmed"); prepareEnergy(mc); }
            }
            case CAPTURE -> {
               int amount=menu.energyCapacity()*captureIndex/2;
               if(menu.energyStored()!=amount || ticks<12) return;
               if(!serverMatches(mc,p->machine.energyStorage(null).getEnergyStored()==amount && machine.comparatorOutput()==(int)Math.ceil(captureIndex*7.5)))return;
               require(menu.energyDisplayGranularity()==100 && menu.comparatorOutput()==(int)Math.ceil(captureIndex*7.5),"FE synchronization/comparator differs");
               verifyGeometry(screen,menu);capture(mc,menu);
               if(++captureIndex<3) prepareEnergy(mc);
               else { CASES.add("native_empty_half_full_fe");oldMenuId=menu.containerId;screen.onClose();enter(Stage.CLOSE); }
            }
            case REOPEN -> {
               if(ticks<12 || menu.containerId==oldMenuId) return;
               verifyGeometry(screen,menu);
               require(menu.energyStored()==menu.energyCapacity(),"Reopen lost energy state");
               CASES.add("reopen_preserves_energy_and_geometry");enter(Stage.DONE);
            }
            case DONE -> finish(mc,null);
            default -> throw new IllegalStateException("Unexpected stage "+stage);
         }
      } catch(Throwable error) { finish(mc,error); }
   }
   private static void boot(Minecraft mc) throws Exception {
      require(!Boolean.getBoolean("iedctr.clientGameplay") && Boolean.getBoolean(MachineMenu.DEV_SOLAR_V2_REFERENCE_PROPERTY),"Dedicated fixture only; V2 must be enabled");
      nonce=System.getProperty("iedctr.clientGameplayNonce","");
      require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha","").matches("[a-f0-9]{40}"),"Missing nonce or exact revision");
      directory=mc.gameDirectory.toPath().toRealPath();
      require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory","")).toRealPath()) && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),"Isolation marker mismatch");
      require(mc.level==null && mc.getSingleplayerServer()==null && !Files.exists(directory.resolve("solar-v2-reference-screenshots")),"Refusing existing world/captures");
      verifyViewport(mc);
      if(Files.exists(directory.resolve("saves"))) try(var entries=Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(),"Existing saves forbidden"); }
      String name="iedctr-solar-v2-"+nonce;
      mc.options.pauseOnLostFocus=false;
      enter(Stage.WORLD);
      mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
         new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
   }
   private static void prepareSlot(Minecraft mc) {
      pending=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);p.getInventory().clearContent();machine.clearContent();machine.dataAccessForTests().set(3,0);
         p.containerMenu.setCarried(ItemStack.EMPTY);
         p.containerMenu.getSlot(slotIndex).set(new ItemStack(fixtureItem()));p.containerMenu.broadcastChanges();
      });enter(Stage.SLOT_READY);
   }
   private static Item fixtureItem() { return slotIndex==0?cellItem():Items.STONE; }
   private static int destination() { return slotIndex==28?29:28; }
   private static boolean matches(ItemStack stack,Item item) { return stack.is(item) && stack.getCount()==1; }
   private static int total(ServerPlayer p) { int n=p.containerMenu.getCarried().getCount();for(var s:p.containerMenu.slots)n+=s.getItem().getCount();return n; }
   private static Item cellItem() {
      Item item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","capacitor_lv"));
      require(item!=Items.AIR,"Missing registered IE LV capacitor");return item;
   }
   private static void prepareCell(Minecraft mc) {
      pending=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);p.getInventory().clearContent();machine.clearContent();
         var handler=machine.itemHandler(Direction.UP);
         require(handler!=null && handler.insertItem(0,new ItemStack(Items.STONE),false).getCount()==1
            && machine.getItem(0).isEmpty(),"Non-energy insertion was accepted");
         var cell=new ItemStack(cellItem());var energy=cell.getCapability(EnergyStorage.ITEM);
         require(energy!=null && energy.canReceive() && energy.getEnergyStored()==0,"Real capacitor capability unavailable or not empty");
         machine.dataAccessForTests().set(3,10);
         p.containerMenu.getSlot(28).set(cell);p.containerMenu.broadcastChanges();
      });enter(Stage.CELL_READY);
   }
   private static void prepareEnergy(Minecraft mc) {
      pending=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);p.getInventory().clearContent();machine.clearContent();
         machine.dataAccessForTests().set(3,320*captureIndex);
         machine.setChanged();p.containerMenu.broadcastChanges();
      });
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),mc.getWindow().getScreenWidth()-2,mc.getWindow().getScreenHeight()-2);
      enter(Stage.CAPTURE);
   }
   private static void click(SolarV2ReferenceScreen screen,int index) {
      var a=SolarV2ReferenceGeometry.anchor(index);var v=screen.validationViewTransform();
      screen.mouseClicked(v.screenX(a.x()+8),v.screenY(a.y()+8),0);
      screen.mouseReleased(v.screenX(a.x()+8),v.screenY(a.y()+8),0);
   }
   private static void verifyGeometry(SolarV2ReferenceScreen screen,MachineMenu menu) {
      verifyViewport(Minecraft.getInstance());
      require(menu.solarV2ReferencePresentation() && menu.kind()==MachineKind.SMALL_SOLAR_PANEL && menu.slots.size()==37 && menu.visibleMachineSlots()==1,"Wrong native menu");
      var v=screen.validationViewTransform();
      int expectedLeft=(int)Math.floor(v.screenX(0)),expectedTop=(int)Math.floor(v.screenY(0));
      int expectedWidth=(int)Math.ceil(v.screenX(SolarV2ReferenceGeometry.CANVAS_WIDTH))-expectedLeft;
      int expectedHeight=(int)Math.ceil(v.screenY(SolarV2ReferenceGeometry.CANVAS_HEIGHT))-expectedTop;
      require(screen.getGuiLeft()==expectedLeft && screen.getGuiTop()==expectedTop
         && screen.getXSize()==expectedWidth && screen.getYSize()==expectedHeight,
         "Public presentation bounds differ from the transformed Solar canvas");
      for(var a:SolarV2ReferenceGeometry.SLOT_ANCHORS) {
         var s=menu.getSlot(a.menuIndex());int index=a.menuIndex()<1?a.menuIndex():a.menuIndex()<28?a.menuIndex()+8:a.menuIndex()-28;
         require(Math.abs(v.localX(v.screenX(a.x()+8))-(a.x()+8))<0.00001
            && Math.abs(v.localY(v.screenY(a.y()+8))-(a.y()+8))<0.00001,"Inverse pointer transform differs");
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
   private static void capture(Minecraft mc,MachineMenu menu) throws Exception {
      String label=new String[]{"empty","half","full"}[captureIndex];
      Path folder=directory.resolve("solar-v2-reference-screenshots");Files.createDirectories(folder);
      Path file=folder.resolve(label+".png");require(!Files.exists(file),"Stale capture");
      JsonObject c=new JsonObject();var v=((SolarV2ReferenceScreen)mc.screen).validationViewTransform();
      var screen=(SolarV2ReferenceScreen)mc.screen;
      c.addProperty("presentation_gui_left",screen.getGuiLeft());c.addProperty("presentation_gui_top",screen.getGuiTop());
      c.addProperty("presentation_gui_width",screen.getXSize());c.addProperty("presentation_gui_height",screen.getYSize());
      addViewport(c,mc);double guiScale=mc.getWindow().getGuiScale();
      try(var frame=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         frame.writeToFile(file);c.addProperty("width",frame.getWidth());c.addProperty("height",frame.getHeight());
         require(mc.getWindow().getWidth()==frame.getWidth() && mc.getWindow().getHeight()==frame.getHeight(),"Framebuffer capture size differs");
         // The renderer rounds its scissor in GUI coordinates before Minecraft
         // multiplies by the window GUI scale. Preserve that order for the crop.
         int x0=(int)(Math.ceil(v.screenX(SolarV2ReferenceState.METER_X))*guiScale),y0=(int)(Math.ceil(v.screenY(SolarV2ReferenceState.METER_Y))*guiScale);
         int x1=(int)(Math.floor(v.screenX(SolarV2ReferenceState.METER_X+SolarV2ReferenceState.METER_WIDTH))*guiScale),y1=(int)(Math.floor(v.screenY(SolarV2ReferenceState.METER_Y+SolarV2ReferenceState.METER_HEIGHT))*guiScale);
         require(x0>=0 && y0>=0 && x1<=frame.getWidth() && y1<=frame.getHeight() && x1>x0 && y1>y0,"Invalid native energy bounds");
         Path cropFile=folder.resolve(label+"-energy.png");
         try(var crop=new NativeImage(x1-x0,y1-y0,false)) {
            for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++)crop.setPixelRGBA(x-x0,y-y0,frame.getPixelRGBA(x,y));
            crop.writeToFile(cropFile);
         }
         c.addProperty("energy_name",cropFile.getFileName().toString());c.addProperty("energy_sha256",hash(cropFile));
         c.addProperty("energy_x",x0);c.addProperty("energy_y",y0);
         c.addProperty("energy_width",x1-x0);c.addProperty("energy_height",y1-y0);
         // Entire retained canvas, not just a ring. Exclude only the FE interior
         // and the actual 16px item slots (2 framebuffer pixels raster safety).
         var digest=MessageDigest.getInstance("SHA-256");
         for(int y=(int)Math.ceil(v.screenY(0)*guiScale);y<(int)Math.floor(v.screenY(418)*guiScale);y++)
            for(int x=(int)Math.ceil(v.screenX(0)*guiScale);x<(int)Math.floor(v.screenX(418)*guiScale);x++) {
               if(excludedPixel(x,y,v,guiScale))continue;
               int pixel=frame.getPixelRGBA(x,y);for(int b=0;b<4;b++)digest.update((byte)(pixel>>>(8*b)));
            }
         c.addProperty("frame_exterior_sha256",HexFormat.of().formatHex(digest.digest()));
      }
      c.addProperty("name",file.getFileName().toString());c.addProperty("sha256",hash(file));
      c.addProperty("amount",menu.energyStored());c.addProperty("capacity",menu.energyCapacity());
      c.addProperty("comparator",menu.comparatorOutput());c.addProperty("sync_granularity_fe",menu.energyDisplayGranularity());
      c.addProperty("menu_id",menu.containerId);c.addProperty("screen_class",mc.screen.getClass().getName());c.addProperty("view_scale",v.scale());CAPTURES.add(c);
   }
   private static boolean excludedPixel(int x,int y,SolarV2ReferenceGeometry.ViewTransform v,double scale) {
      if(inRect(x,y,v,scale,SolarV2ReferenceState.METER_X,SolarV2ReferenceState.METER_Y,SolarV2ReferenceState.METER_X+SolarV2ReferenceState.METER_WIDTH,SolarV2ReferenceState.METER_Y+SolarV2ReferenceState.METER_HEIGHT))return true;
      for(var a:SolarV2ReferenceGeometry.SLOT_ANCHORS)if(inRect(x,y,v,scale,a.x(),a.y(),a.x()+16,a.y()+16))return true;
      return false;
   }
   private static boolean inRect(int x,int y,SolarV2ReferenceGeometry.ViewTransform v,double scale,int l,int t,int r,int b) {
      return x>=Math.floor(v.screenX(l)*scale)-2 && x<=Math.ceil(v.screenX(r)*scale)+2
         && y>=Math.floor(v.screenY(t)*scale)-2 && y<=Math.ceil(v.screenY(b)*scale)+2;
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
            p.addProperty("selected_language",mc.getLanguageManager().getSelected());p.addProperty("source_art_sha256",SolarV2ReferenceGeometry.SOURCE_SHA256);
            addViewport(p,mc);
            p.addProperty("actual_integrated_world",mc.getSingleplayerServer()!=null);p.addProperty("native_visual_acceptance",false);p.addProperty("menu_slot_count",37);
            p.addProperty("input_method","Screen.mouseClicked/mouseReleased with vanilla packets and server observation; GLFW cursor parking only, no physical pointer, resource reload or complex gesture coverage");
            p.addProperty("cell_item","immersiveengineering:capacitor_lv");
            p.addProperty("cell_initial_buffer_fe",1000);p.addProperty("cell_initial_item_fe",0);
            p.addProperty("cell_charged_fe",chargedCellFe);p.addProperty("cell_remaining_buffer_fe",remainingBufferFe);
            p.add("slot_routes",ROUTES);p.add("captures",CAPTURES);p.add("cases",CASES);
            if(error!=null)p.addProperty("error",error.toString());
            Files.writeString(directory.resolve("client-solar-v2-reference-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(p)+"\n");
         }
      }catch(Exception writeError){EngineersDecorReforged.LOGGER.error("Solar V2 proof write failed",writeError);}
      if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_SOLAR_V2_REFERENCE PASSED nonce={}",nonce);
      else EngineersDecorReforged.LOGGER.error("IEDCTR_SOLAR_V2_REFERENCE FAILED at "+stage,error);
      mc.stop();
   }
   private static void require(boolean b,String message){if(!b)throw new IllegalStateException(message);}
}
