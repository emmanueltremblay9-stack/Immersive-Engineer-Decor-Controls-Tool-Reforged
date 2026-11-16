package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.*;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Test-only album capture, server-opened containers. No human input or gameplay qualification claimed. */
@EventBusSubscriber(modid=FactoryPlacerValidationMod.MOD_ID, value=Dist.CLIENT)
public final class GuiAlbumThree {
    private static final String[] NAMES={"factory-hopper","metal-crafting-table","small-electrical-furnace","small-waste-incinerator","labeled-crate","factory-dropper"};
    private static final String[] SCREENS={"FactoryHopperScreen","MetalCraftingTableScreen","SmallElectricalFurnaceScreen","SmallWasteIncineratorScreen","LabeledCrateScreen","FactoryDropperScreen"};
    private enum Stage {BOOT,WORLD,OPEN,WAIT,CLOSE}
    private static Stage stage=Stage.BOOT;
    private static boolean complete;
    private static long deadline;
    private static int ticks,index;
    private static UUID playerId;
    private static Path directory;
    private static String nonce;
    private static JsonObject packaged;
    private static final JsonArray cases=new JsonArray();
    private static final MachineBlockEntity[] machines=new MachineBlockEntity[NAMES.length];
    private static CompletableFuture<?> pending;
    private static CompletableFuture<Boolean> inspection;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("iedctr.guiAlbumThree")||complete)return;
        var mc=Minecraft.getInstance();
        try {
            if(deadline==0)deadline=System.nanoTime()+900_000_000_000L;
            require(System.nanoTime()<deadline&&++ticks<1800,"Timeout at "+stage);
            if(stage==Stage.BOOT){if(mc.screen instanceof TitleScreen&&mc.getOverlay()==null)boot(mc);return;}
            if(stage==Stage.WORLD){
                if(mc.level==null||mc.player==null||mc.gameMode==null||mc.screen!=null)return;
                require(mc.getSingleplayerServer()!=null&&mc.player.connection.getConnection().isMemoryConnection(),"Isolated integrated world required");
                playerId=mc.player.getUUID();pending=mc.getSingleplayerServer().submit(()->setup(mc));enter(Stage.OPEN);return;
            }
            require(mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null,"World disconnected");
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;}
            if(mc.getOverlay()!=null)return;
            switch(stage){
                case OPEN -> {
                    pending=mc.getSingleplayerServer().submit(()->{
                        var p=player(mc); var pos=machines[index].getBlockPos();
                        // Every case must remain within the native menu stillValid radius.
                        p.connection.teleport(pos.getX()+.5,pos.getY(),pos.getZ()+2.5,180,0);
                        p.openMenu(machines[index]);
                    });
                    enter(Stage.WAIT);
                }
                case WAIT -> {
                    if(ticks<(index==0?650:35)||mc.screen==null)return;
                    require(mc.screen.getClass().getName().equals("com.oblixorprime.engineersdecorreforged.client."+(index!=4?"MachineScreen$":"")+SCREENS[index]),"Wrong machine screen");
                    require(mc.screen instanceof AbstractContainerScreen<?> && (index==4 || mc.player.containerMenu instanceof MachineMenu),"Missing actual container");
                    if(inspection==null){inspection=mc.getSingleplayerServer().submit(()->player(mc).containerMenu.getClass()==mc.player.containerMenu.getClass() && player(mc).containerMenu.containerId==mc.player.containerMenu.containerId);return;}
                    if(!inspection.isDone())return;
                    require(inspection.join(),"Server menu mismatch");
                    var obs=new JsonObject();obs.addProperty("screen",mc.screen.getClass().getName());obs.addProperty("case",NAMES[index]);obs.addProperty("server_menu_acknowledged",true);
                    obs.addProperty("gui_scale",mc.getWindow().getGuiScale());
                    obs.addProperty("visible_nonempty_slots",mc.player.containerMenu.slots.stream().filter(slot->slot.hasItem()).count());
                    var production=ModList.get().getModFileById(EngineersDecorReforged.MOD_ID).getFile();
                    var entry=mc.screen.getClass().getName().replace('.', '/')+".class";
                    var resource=mc.screen.getClass().getResource("/"+entry);
                    require(resource!=null&&resource.toURI().equals(production.findResource(entry).toUri()),"Screen not from production JAR");
                    try(var input=resource.openStream();var zip=new java.util.zip.ZipFile(production.getFilePath().toFile());var archived=zip.getInputStream(zip.getEntry(entry))){
                        String hash=PackagedRuntimeProof.hash(input.readAllBytes());require(hash.equals(PackagedRuntimeProof.hash(archived.readAllBytes())),"Screen bytes differ");obs.addProperty("screen_class_sha256",hash);
                    }
                    var imageName=NAMES[index]+".png";
                    try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory.resolve(imageName));}
                    obs.addProperty("capture",imageName);obs.addProperty("capture_sha256",PackagedRuntimeProof.hash(Files.readAllBytes(directory.resolve(imageName))));
                    cases.add(obs);mc.screen.onClose();enter(Stage.CLOSE);
                }
                case CLOSE -> {if(mc.screen!=null||ticks<20)return;index++;if(index==NAMES.length)finish(mc,null);else enter(Stage.OPEN);}
                default -> throw new IllegalStateException("Unexpected "+stage);
            }
        }catch(Throwable error){finish(mc,error);}
    }
    private static void boot(Minecraft mc) throws Exception {
        nonce=System.getProperty("iedctr.clientGameplayNonce","");
        require(nonce.matches("[a-f0-9]{32}")&&System.getProperty("iedctr.executedSourceSha","").matches("[a-f0-9]{40}"),"Missing identity");
        directory=mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory","")).toRealPath())&&Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),"Isolation marker mismatch");
        require(Files.readString(directory.resolve(".iedctr-gui-album-three-xvfb")).equals(nonce+"\n"+System.getenv("DISPLAY")),"Display not owned");
        require(mc.level==null&&mc.getSingleplayerServer()==null&&!Files.exists(directory.resolve("gui-album-three-proof.json")),"Existing world/proof");
        if(Files.exists(directory.resolve("saves")))try(var entries=Files.list(directory.resolve("saves"))){require(entries.findAny().isEmpty(),"Existing saves forbidden");}
        packaged=PackagedRuntimeProof.verify(mc);
        String name="iedctr-gui-album-three-"+nonce;mc.options.pauseOnLostFocus=false;enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
    }
    private static void setup(Minecraft mc){
        var p=player(mc);p.setGameMode(GameType.SURVIVAL);var level=p.serverLevel();
        for(int x=-2;x<=12;x++)for(int z=-1;z<=4;z++)level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),3);
        p.connection.teleport(.5,80,2.5,180,0);
        p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,16));
        p.getInventory().setItem(1,new ItemStack(Items.REDSTONE,8));
        p.getInventory().setItem(9,new ItemStack(Items.COBBLESTONE,32));
        var blocks=List.of(ModBlocks.FACTORY_HOPPER.get(),ModBlocks.METAL_CRAFTING_TABLE.get(),ModBlocks.SMALL_ELECTRICAL_FURNACE.get(),ModBlocks.SMALL_WASTE_INCINERATOR.get(),ModBlocks.LABELED_CRATE.get(),ModBlocks.FACTORY_DROPPER.get());
        for(int i=0;i<NAMES.length;i++){var pos=new BlockPos(i*2,80,0);level.setBlock(pos,blocks.get(i).defaultBlockState(),3);machines[i]=(MachineBlockEntity)level.getBlockEntity(pos);require(machines[i]!=null,"Missing machine "+i);}
        machines[0].setItem(0,new ItemStack(Items.IRON_INGOT,8));
        machines[5].setItem(0,new ItemStack(Items.IRON_INGOT,8));
    }
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);require(p!=null,"Player missing");return p;}
    private static void enter(Stage next){stage=next;ticks=0;inspection=null;}
    private static void finish(Minecraft mc,Throwable error){
        complete=true;
        try{if(directory!=null){var proof=new JsonObject();proof.addProperty("suite","gui-album-three-v1");proof.addProperty("status",error==null?"observed":"failed");proof.addProperty("nonce",nonce);proof.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));proof.add("packaged_runtime",packaged);proof.add("cases",cases);proof.addProperty("visual_acceptance",false);proof.addProperty("physical_os_input",false);if(error!=null)proof.addProperty("error",error.toString());Files.writeString(directory.resolve("gui-album-three-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");}}
        catch(Exception e){EngineersDecorReforged.LOGGER.error("Album proof error",e);}
        if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_GUI_ALBUM_THREE OBSERVED nonce={}",nonce);else EngineersDecorReforged.LOGGER.error("IEDCTR_GUI_ALBUM_THREE FAILED "+stage,error);mc.stop();
    }
    private static void require(boolean b,String message){PackagedRuntimeProof.require(b,message);}
}
