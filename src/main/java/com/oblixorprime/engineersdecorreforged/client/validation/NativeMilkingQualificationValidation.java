package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
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
import net.minecraft.resources.ResourceLocation;
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

/** Explicit opt-in only: isolated native client/server qualification, not visual acceptance. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, value = Dist.CLIENT)
public final class NativeMilkingQualificationValidation {
   private static final String SUITE="native-milking-qualification-v1";
   private static final BlockPos POS=new BlockPos(0,80,0);
   private enum Stage { BOOT,WORLD,SETUP,SLOT_READY,PICKED,RETURNED,CAPTURE,DONE }
   private static Stage stage=Stage.BOOT;
   private static Path directory;
   private static String nonce;
   private static UUID playerId;
   private static long deadline, lastClickNanos, lastClickIntervalNanos;
   private static final long MIN_CLICK_INTERVAL_NANOS=300_000_000L;
   private static int ticks,slotIndex,captureIndex,serverMenuId,captureStableTicks;
   private static boolean complete;
   private static boolean isolatedDirectoryValidated;
   private static MachineBlockEntity machine;
   private static CompletableFuture<?> pending;
   private static CompletableFuture<Boolean> inspection;
   private static final JsonArray ROUTES=new JsonArray(),CAPTURES=new JsonArray();
   private static final JsonObject CONVERSION=new JsonObject();
   private static String serverFluid;
   private NativeMilkingQualificationValidation() {}
   @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
      if(!Boolean.getBoolean("iedctr.milkingQualificationNative") || complete)return;
      Minecraft mc=Minecraft.getInstance();
      try {
         if(deadline==0)deadline=System.nanoTime()+900_000_000_000L;
         require(System.nanoTime()<deadline && ++ticks<1800,"Native Milking timeout at "+stage);
         if(stage==Stage.BOOT){if(mc.screen instanceof TitleScreen && mc.getOverlay()==null)boot(mc);return;}
         if(stage==Stage.WORLD){
            if(mc.level==null || mc.player==null || mc.gameMode==null || mc.screen!=null)return;
            require(mc.getSingleplayerServer()!=null && mc.player.connection.getConnection().isMemoryConnection(),"Only isolated integrated worlds permitted");
            playerId=mc.player.getUUID();
            pending=mc.getSingleplayerServer().submit(()->{
               var p=player(mc);p.setGameMode(GameType.SURVIVAL);var level=p.serverLevel();
               for(int x=-1;x<=1;x++)for(int z=2;z<=4;z++)level.setBlock(new BlockPos(x,80,z),Blocks.STONE.defaultBlockState(),3);
               p.connection.teleport(.5,81,3.5,180,0);
               level.setBlock(POS.below(),Blocks.STONE.defaultBlockState(),3);
               level.setBlock(POS,ModBlocks.SMALL_MILKING_MACHINE.get().defaultBlockState(),3);
               require(level.getBlockEntity(POS) instanceof MachineBlockEntity,"Missing Milking entity");
               machine=(MachineBlockEntity)level.getBlockEntity(POS);p.openMenu(machine);
               require(p.containerMenu instanceof MachineMenu && p.containerMenu.slots.size()==38,"Wrong server menu");
               serverMenuId=p.containerMenu.containerId;
            });enter(Stage.SETUP);return;
         }
         require(mc.level!=null && mc.player!=null && mc.getSingleplayerServer()!=null,"World disconnected");
         if(!ready())return;
         if(!(mc.screen instanceof MachineScreen.SmallMilkingMachineScreen screen) || !(mc.player.containerMenu instanceof MachineMenu menu) || menu.containerId!=serverMenuId)return;
         switch(stage){
            case SETUP -> {verifyGeometry(screen,menu);prepareSlot(mc);}
            case SLOT_READY -> {
               if(!clickIntervalElapsed() || ticks<8 || !matches(menu.getSlot(slotIndex).getItem(),fixtureItem()) || !menu.getCarried().isEmpty() || menu.fluidAmount()!=0 || (slotIndex==1 && !menu.getSlot(0).getItem().isEmpty()))return;
               if(!serverMatches(mc,p->{
                  boolean valid=matches(p.containerMenu.getSlot(slotIndex).getItem(),fixtureItem()) && p.containerMenu.getCarried().isEmpty() && total(p)==1 && machine.fluidHandler(Direction.UP).getFluidInTank(0).isEmpty() && (slotIndex!=1 || p.containerMenu.getSlot(0).getItem().isEmpty());
                  if(valid && slotIndex==1)CONVERSION.add("after",conversionState(p));return valid;
               }))return;
               if(slotIndex==1){CONVERSION.addProperty("server_confirmed",true);CONVERSION.addProperty("client_confirmed",true);CONVERSION.addProperty("seeded_output",false);CONVERSION.addProperty("after_fluid_amount",0);CONVERSION.addProperty("after_input_count",0);CONVERSION.addProperty("after_output_count",1);CONVERSION.addProperty("native_tick_observed",true);CONVERSION.addProperty("conserved_milk_mb",1000);}
               verifyGeometry(screen,menu);click(screen,slotIndex);enter(Stage.PICKED);
            }
            case PICKED -> {
               // A same-slot return inside vanilla's double-click window can become PICKUP_ALL.
               // Bound this by monotonic elapsed time, independent of client/server tick rate.
               if(!clickIntervalElapsed() || !menu.getSlot(slotIndex).getItem().isEmpty() || !menu.getSlot(destination()).getItem().isEmpty()
                  || !matches(menu.getCarried(),fixtureItem()) || menu.fluidAmount()!=0
                  || !menu.getSlot(0).getItem().isEmpty() || !menu.getSlot(1).getItem().isEmpty()
                  || !serverMatches(mc,p->p.containerMenu.getSlot(slotIndex).getItem().isEmpty() && p.containerMenu.getSlot(destination()).getItem().isEmpty()
                     && p.containerMenu.getSlot(0).getItem().isEmpty() && p.containerMenu.getSlot(1).getItem().isEmpty()
                     && matches(p.containerMenu.getCarried(),fixtureItem()) && total(p)==1 && machine.fluidHandler(Direction.UP).getFluidInTank(0).isEmpty()))return;
               click(screen,destination());enter(Stage.RETURNED);
            }
            case RETURNED -> {
               if(!menu.getCarried().isEmpty() || !matches(menu.getSlot(destination()).getItem(),fixtureItem()) || !serverMatches(mc,p->p.containerMenu.getCarried().isEmpty() && matches(p.containerMenu.getSlot(destination()).getItem(),fixtureItem()) && total(p)==1 && machine.fluidHandler(Direction.UP).getFluidInTank(0).isEmpty()))return;
               var r=new JsonObject();r.addProperty("source_menu_index",slotIndex);r.addProperty("destination_menu_index",destination());
               r.addProperty("source_x",screen.getGuiLeft()+anchor(slotIndex).x()+8);r.addProperty("source_y",screen.getGuiTop()+anchor(slotIndex).y()+8);
               r.addProperty("destination_x",screen.getGuiLeft()+anchor(destination()).x()+8);r.addProperty("destination_y",screen.getGuiTop()+anchor(destination()).y()+8);
               r.addProperty("item_id",BuiltInRegistries.ITEM.getKey(fixtureItem()).toString());r.addProperty("final_fluid_amount",0);r.addProperty("count",1);r.addProperty("conserved_items",1);
               r.addProperty("pickup_to_return_interval_nanos",lastClickIntervalNanos);
               r.addProperty("pickup_server_ack",true);r.addProperty("return_server_ack",true);r.addProperty("menu_id",menu.containerId);ROUTES.add(r);
               if(++slotIndex<38)prepareSlot(mc);else prepareFluid(mc);
            }
            case CAPTURE -> {
               int amount=captureIndex*2000;
               // Count stable synchronized client state, not time spent awaiting the server fill.
               if(menu.fluidAmount()!=amount || menu.fluidCapacity()!=4000 || !menu.getCarried().isEmpty()
                  || menu.slots.stream().anyMatch(slot->!slot.getItem().isEmpty())){
                  captureStableTicks=0;inspection=null;return;
               }
               if(++captureStableTicks<12)return;
               if(!serverMatches(mc,p->{var tank=machine.fluidHandler(Direction.UP);var fluid=tank.getFluidInTank(0);serverFluid=BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();return p.containerMenu.containerId==serverMenuId && fluid.getAmount()==amount && tank.getTankCapacity(0)==4000 && (amount==0?fluid.isEmpty():fluid.getFluid()==NeoForgeMod.MILK.value()) && total(p)==0;}))return;
               verifyGeometry(screen,menu);capture(mc,menu,new String[]{"empty","half","full"}[captureIndex]);
               if(++captureIndex<3)prepareFluid(mc);else enter(Stage.DONE);
            }
            case DONE -> finish(mc,null);
            default -> throw new IllegalStateException("Unexpected stage "+stage);
         }
      }catch(Throwable error){finish(mc,error);}
   }
   private static void boot(Minecraft mc) throws Exception {
      require(!Boolean.getBoolean("iedctr.clientGameplay") && !Boolean.getBoolean("iedctr.barrelReferenceNative") && !Boolean.getBoolean("iedctr.solarV2ReferenceNative"),"Dedicated fixture only");
      nonce=System.getProperty("iedctr.clientGameplayNonce","");
      require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha","").matches("[a-f0-9]{40}"),"Missing nonce or exact revision");
      directory=mc.gameDirectory.toPath().toRealPath();
      require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory","")).toRealPath()) && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),"Isolation marker mismatch");
      require(mc.level==null && mc.getSingleplayerServer()==null && !Files.exists(directory.resolve("milking-qualification-screenshots"))
         && !Files.exists(directory.resolve("client-milking-qualification-result.json")),"Refusing existing world/captures/result");
      if(Files.exists(directory.resolve("saves"))) try(var entries=Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(),"Existing saves forbidden"); }
      isolatedDirectoryValidated=true;
      verifyViewport(mc);
      verifyArt(mc,"background","871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b");
      verifyArt(mc,"milk","f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5");
      String name="iedctr-milking-qualification-"+nonce;
      mc.options.pauseOnLostFocus=false;
      enter(Stage.WORLD);
      mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
         new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
   }
   private static void verifyArt(Minecraft mc,String name,String expected)throws Exception{
      var id=ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,"textures/gui/milking_redesign_v1/"+name+".png");
      try(var input=mc.getResourceManager().getResourceOrThrow(id).open()){
         require(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes())).equals(expected),"Active resource artwork differs: "+name);
      }
   }
   private static void prepareSlot(Minecraft mc){
      pending=mc.getSingleplayerServer().submit(()->{
         var p=player(mc);p.getInventory().clearContent();machine.clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);
         var tank=machine.fluidHandler(Direction.UP);require(tank!=null && tank.getTankCapacity(0)==4000,"Wrong Milking tank");tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
         if(slotIndex==1){
            require(tank.fill(new FluidStack(NeoForgeMod.MILK.value(),1000),FluidAction.EXECUTE)==1000,"Milk seed rejected");
            p.containerMenu.getSlot(0).set(new ItemStack(Items.BUCKET));
            require(matches(p.containerMenu.getSlot(0).getItem(),Items.BUCKET) && total(p)==1 && p.containerMenu.getCarried().isEmpty() && p.containerMenu.getSlot(1).getItem().isEmpty() && tank.getFluidInTank(0).getAmount()==1000,"Conversion precondition failed");
            CONVERSION.add("before",conversionState(p));CONVERSION.addProperty("before_fluid_amount",1000);CONVERSION.addProperty("before_input_count",1);CONVERSION.addProperty("before_output_count",0);
            CONVERSION.addProperty("fluid_id",BuiltInRegistries.FLUID.getKey(tank.getFluidInTank(0).getFluid()).toString());
            CONVERSION.addProperty("method","authoritative milk fill plus empty input bucket; wait for native server tick, never seed output");
         }else p.containerMenu.getSlot(slotIndex).set(new ItemStack(fixtureItem()));
         machine.setChanged();p.containerMenu.broadcastChanges();
      });enter(Stage.SLOT_READY);
   }
   private static Item fixtureItem(){return slotIndex==0?Items.BUCKET:slotIndex==1?Items.MILK_BUCKET:Items.DIRT;}
   private static int destination(){return slotIndex==1?2:slotIndex;}
   private static boolean matches(ItemStack stack,Item item){return stack.is(item) && stack.getCount()==1;}
   private static int total(ServerPlayer p){int n=p.containerMenu.getCarried().getCount();for(var slot:p.containerMenu.slots)n+=slot.getItem().getCount();return n;}
   private static MilkingRedesignLayout.Rect anchor(int index){return index==0?MilkingRedesignLayout.INPUT_SLOT:index==1?MilkingRedesignLayout.OUTPUT_SLOT:index<29?MilkingRedesignLayout.playerSlot((index-2)%9,(index-2)/9):MilkingRedesignLayout.hotbarSlot(index-29);}
   private static boolean clickIntervalElapsed(){return lastClickNanos==0 || System.nanoTime()-lastClickNanos>=MIN_CLICK_INTERVAL_NANOS;}
   private static void click(MachineScreen screen,int index){
      long now=System.nanoTime();
      require(lastClickNanos==0 || now-lastClickNanos>=MIN_CLICK_INTERVAL_NANOS,"Single-click spacing violated");
      lastClickIntervalNanos=lastClickNanos==0?0:now-lastClickNanos;
      var a=anchor(index);double x=screen.getGuiLeft()+a.x()+8,y=screen.getGuiTop()+a.y()+8;
      screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);lastClickNanos=System.nanoTime();
   }
   private static void verifyGeometry(MachineScreen screen,MachineMenu menu){
      verifyViewport(Minecraft.getInstance());
      require(menu.kind()==MachineKind.SMALL_MILKING_MACHINE && menu.slots.size()==38 && menu.visibleMachineSlots()==2,"Wrong native Milking menu");
      require(screen.getXSize()==228 && screen.getYSize()==228,"Wrong Milking canvas");
      require(screen.getGuiLeft()==(screen.width-228)/2 && screen.getGuiTop()==(screen.height-228)/2,"Public integer origin differs");
      for(int i=0;i<38;i++){var a=anchor(i);var slot=menu.getSlot(i);int inventoryIndex=i<2?i:i<29?i+7:i-29;require(slot.x==a.x() && slot.y==a.y() && slot.getSlotIndex()==inventoryIndex,"Slot mapping differs at "+i);}
      require(!menu.getSlot(1).mayPlace(new ItemStack(Items.MILK_BUCKET)),"Output must reject insertion");
   }
   private static void prepareFluid(Minecraft mc){
      captureStableTicks=0;
      ((MachineScreen)mc.screen).setSuppressTooltipsForPanelCapture(true);
      pending=mc.getSingleplayerServer().submit(()->{var p=player(mc);p.getInventory().clearContent();machine.clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);
         var tank=machine.fluidHandler(Direction.UP);tank.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);int amount=captureIndex*2000;
         if(amount>0)require(tank.fill(new FluidStack(NeoForgeMod.MILK.value(),amount),FluidAction.EXECUTE)==amount,"Milk fill rejected");
         machine.setChanged();p.containerMenu.broadcastChanges();});
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),mc.getWindow().getScreenWidth()-2,mc.getWindow().getScreenHeight()-2);enter(Stage.CAPTURE);
   }
   private static void verifyViewport(Minecraft mc) {
      require(mc.getLanguageManager().getSelected().equals(System.getProperty("iedctr.requestedLanguage","")),"Requested locale not applied");
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
      proof.addProperty("requested_width",Integer.getInteger("iedctr.requestedWidth",0));proof.addProperty("requested_height",Integer.getInteger("iedctr.requestedHeight",0));
      proof.addProperty("requested_gui_scale",Integer.getInteger("iedctr.requestedGuiScale",0));
      proof.addProperty("actual_gui_scale",window.getGuiScale());
      proof.addProperty("gui_width",window.getGuiScaledWidth());proof.addProperty("gui_height",window.getGuiScaledHeight());
      proof.addProperty("framebuffer_width",window.getWidth());proof.addProperty("framebuffer_height",window.getHeight());
   }
   private static void capture(Minecraft mc,MachineMenu menu,String label)throws Exception{
      require(mc.getOverlay()==null,"Cannot capture loading overlay");
      Path folder=directory.resolve("milking-qualification-screenshots");Files.createDirectories(folder);
      Path file=folder.resolve(label+".png"),cropFile=folder.resolve(label+"-tank.png");require(!Files.exists(file) && !Files.exists(cropFile),"Stale capture");
      var screen=(MachineScreen)mc.screen;var c=new JsonObject();addViewport(c,mc);
      c.addProperty("presentation_gui_left",screen.getGuiLeft());c.addProperty("presentation_gui_top",screen.getGuiTop());
      c.addProperty("presentation_gui_width",screen.getXSize());c.addProperty("presentation_gui_height",screen.getYSize());
      double scale=mc.getWindow().getGuiScale();
      // Use actual public integer screen origin, not a floating-point centered canvas.
      int x0=(int)((screen.getGuiLeft()+104)*scale),y0=(int)((screen.getGuiTop()+49)*scale);
      int width=(int)(22*scale),height=(int)(51*scale);
      try(var frame=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
         require(frame.getWidth()==mc.getWindow().getWidth() && frame.getHeight()==mc.getWindow().getHeight(),"Framebuffer differs");
         require(x0>=0 && y0>=0 && x0+width<=frame.getWidth() && y0+height<=frame.getHeight(),"Tank outside framebuffer");
         frame.writeToFile(file);c.addProperty("width",frame.getWidth());c.addProperty("height",frame.getHeight());
         try(var crop=new NativeImage(width,height,false)){
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)crop.setPixelRGBA(x,y,frame.getPixelRGBA(x0+x,y0+y));
            crop.writeToFile(cropFile);
         }
      }
      c.addProperty("name",file.getFileName().toString());c.addProperty("sha256",hash(file));
      c.addProperty("tank_name",cropFile.getFileName().toString());c.addProperty("tank_sha256",hash(cropFile));
      c.addProperty("tank_x",x0);c.addProperty("tank_y",y0);c.addProperty("tank_width",width);c.addProperty("tank_height",height);
      c.addProperty("amount",menu.fluidAmount());c.addProperty("client_fluid_amount",menu.fluidAmount());c.addProperty("server_amount",captureIndex*2000);
      c.addProperty("capacity",menu.fluidCapacity());c.addProperty("fluid_id",serverFluid);
      c.addProperty("menu_id",menu.containerId);c.addProperty("server_menu_id",serverMenuId);c.addProperty("screen_class",screen.getClass().getName());
      require(menu.getCarried().isEmpty() && menu.slots.stream().allMatch(slot->slot.getItem().isEmpty()),"Client slots not empty");
      c.addProperty("settled_client_ticks",captureStableTicks);
      c.addProperty("empty_slots_and_cursor",true);c.addProperty("server_confirmed",true);CAPTURES.add(c);
   }
   private static JsonObject conversionState(ServerPlayer p){
      var s=new JsonObject();s.addProperty("amount",machine.fluidHandler(Direction.UP).getFluidInTank(0).getAmount());
      var input=p.containerMenu.getSlot(0).getItem();var output=p.containerMenu.getSlot(1).getItem();
      s.addProperty("input_buckets",input.is(Items.BUCKET)?input.getCount():0);
      s.addProperty("output_milk_buckets",output.is(Items.MILK_BUCKET)?output.getCount():0);
      int items=0;for(int i=0;i<p.getInventory().getContainerSize();i++)items+=p.getInventory().getItem(i).getCount();
      s.addProperty("player_items",items);s.addProperty("carried_items",p.containerMenu.getCarried().getCount());return s;
   }
   private static String hash(Path file)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
   private static void enter(Stage next){stage=next;ticks=0;inspection=null;}
   private static boolean ready(){if(pending==null)return true;if(!pending.isDone())return false;pending.join();pending=null;return true;}
   private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);require(p!=null,"Player vanished");return p;}
   private static boolean serverMatches(Minecraft mc,Predicate<ServerPlayer> test){if(inspection==null){inspection=mc.getSingleplayerServer().submit(()->{var p=player(mc);require(p.containerMenu.containerId==serverMenuId && p.serverLevel().getBlockEntity(POS)==machine,"Authoritative menu or machine changed");return test.test(p);});return false;}if(!inspection.isDone())return false;boolean b=inspection.join();inspection=null;return b;}
   private static void finish(Minecraft mc,Throwable error){
      complete=true;
      // An opt-in in an ordinary game directory must never write evidence or quit
      // the user's client when isolation checks fail. Disable only this fixture.
      if(!isolatedDirectoryValidated){
         EngineersDecorReforged.LOGGER.error("IEDCTR_MILKING_QUALIFICATION rejected before isolation validation at "+stage,error);
         return;
      }
      try{if(directory!=null){
         var p=new JsonObject();p.addProperty("suite",SUITE);p.addProperty("status",error==null?"passed_bounded_smoke":"failed");
         p.addProperty("nonce",nonce);p.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));
         p.addProperty("requested_language",System.getProperty("iedctr.requestedLanguage",""));p.addProperty("selected_language",mc.getLanguageManager().getSelected());
         p.addProperty("source_background_sha256","871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b");
         p.addProperty("source_milk_sha256","f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5");p.addProperty("glass_overlay_enabled",false);
         addViewport(p,mc);p.addProperty("actual_integrated_world",mc.getSingleplayerServer()!=null);
         p.addProperty("native_visual_acceptance",false);p.addProperty("menu_slot_count",38);p.addProperty("screen_class",MachineScreen.SmallMilkingMachineScreen.class.getName());
         p.addProperty("input_method","Screen.mouseClicked/mouseReleased with vanilla packets and server observation; single-click pickup/return only, double-click behavior untested; no OS mouse input");
         p.addProperty("minimum_click_interval_nanos",MIN_CLICK_INTERVAL_NANOS);
         p.add("slot_routes",ROUTES);p.add("bucket_conversion",CONVERSION);p.add("captures",CAPTURES);
         var cases=new JsonArray();if(error==null){cases.add("all_38_pickup_return_server_confirmed");cases.add("native_empty_half_full_milk");cases.add("real_bucket_conversion_conserves_items_and_fluid");}p.add("cases",cases);
         var deferred=new JsonArray();for(String check:new String[]{"resource_reload","close_reopen","shift_click","complex_gestures","physical_os_input","jei_actions"})deferred.add(check);p.add("deferred_checks",deferred);
         if(error!=null)p.addProperty("error",error.toString());
         Files.writeString(directory.resolve("client-milking-qualification-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(p)+"\n");
      }}catch(Exception writeError){EngineersDecorReforged.LOGGER.error("Milking qualification proof write failed",writeError);}
      if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_MILKING_QUALIFICATION PASSED nonce={}",nonce);
      else EngineersDecorReforged.LOGGER.error("IEDCTR_MILKING_QUALIFICATION FAILED at "+stage,error);
      mc.stop();
   }
   private static void require(boolean b,String message){if(!b)throw new IllegalStateException(message);}
}
