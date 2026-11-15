package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.*;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.LabeledCrateEditScreen;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Observes real rendered screens and real packet acknowledgements; no visual pass assertion. */
@EventBusSubscriber(modid=FactoryPlacerValidationMod.MOD_ID, value=Dist.CLIENT)
public final class ModernUIDiagnostic {
    private static final BlockPos POS=new BlockPos(0,80,0), CHEST=new BlockPos(2,80,0), FURNACE=new BlockPos(4,80,0);
    private static final List<String> ORIGINAL=List.of("Original first","Original second");
    private static final List<String> EDITED=List.of("Modern UI diagnostic","Text remains editable");
    private enum Stage {BOOT,WORLD,OPEN,WAIT,ACK,CLOSE,DONE}
    private static Stage stage=Stage.BOOT;
    private static boolean complete;
    private static long deadline;
    private static int ticks,index;
    private static UUID playerId;
    private static Path directory;
    private static String nonce;
    private static JsonObject packaged;
    private static final JsonArray cases=new JsonArray();
    private static MachineBlockEntity machine, furnace;
    private static CompletableFuture<?> pending;
    private static CompletableFuture<Boolean> inspection;
    private static final String[] NAMES={"editor","machine-container","vanilla-chest"};
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("iedctr.modernUIDiagnostic")||complete)return;
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
                    if(index==0){
                        if(!(mc.level.getBlockEntity(POS) instanceof MachineBlockEntity))return;
                        mc.setScreen(new LabeledCrateEditScreen(POS,ORIGINAL));
                    }else pending=mc.getSingleplayerServer().submit(()->{
                        if(index==1){furnace.setItem(0,new ItemStack(Items.DIRT,1));player(mc).openMenu(furnace);}
                        else {var chest=(ChestBlockEntity)player(mc).serverLevel().getBlockEntity(CHEST);chest.setItem(0,new ItemStack(Items.DIRT,1));player(mc).openMenu(chest);}
                    });
                    enter(Stage.WAIT);
                }
                case WAIT -> {
                    if(ticks<30||mc.screen==null)return;
                    if(index==0){
                        require(mc.screen instanceof LabeledCrateEditScreen,"Wrong editor");
                        var fields=mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
                        require(fields.size()==2,"Missing real editor fields");
                        for(int i=0;i<2;i++)fields.get(i).setValue(EDITED.get(i));
                        // Capture on a subsequent rendered frame, not immediately after modifying fields.
                        if(ticks<35)return;
                    }else if(!(mc.screen instanceof AbstractContainerScreen<?>))return;
                    var obs=inspectModernUI();obs.addProperty("screen",mc.screen.getClass().getName());obs.addProperty("case",NAMES[index]);
                    if(index<2){
                        var production=ModList.get().getModFileById(EngineersDecorReforged.MOD_ID).getFile();
                        var entry=mc.screen.getClass().getName().replace('.', '/')+".class";
                        var resource=mc.screen.getClass().getResource("/"+entry);
                        require(resource!=null&&resource.toURI().equals(production.findResource(entry).toUri()),"Screen not from production JAR");
                        try(var input=resource.openStream();var zip=new java.util.zip.ZipFile(production.getFilePath().toFile());var archived=zip.getInputStream(zip.getEntry(entry))){
                            String hash=PackagedRuntimeProof.hash(input.readAllBytes());require(hash.equals(PackagedRuntimeProof.hash(archived.readAllBytes())),"Screen bytes differ");obs.addProperty("screen_class_sha256",hash);
                        }
                    }
                    var imageName=NAMES[index]+".png";
                    try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory.resolve(imageName));}
                    obs.addProperty("capture",imageName);obs.addProperty("capture_sha256",PackagedRuntimeProof.hash(Files.readAllBytes(directory.resolve(imageName))));
                    cases.add(obs);
                    if(index==0)require(mc.screen.keyPressed(257,0,0),"Editor Enter rejected");
                    else {
                        var screen=(AbstractContainerScreen<?>)mc.screen;var slot=mc.player.containerMenu.getSlot(0);
                        require(slot.getItem().is(Items.DIRT)&&slot.getItem().getCount()==1,"Seeded slot not synchronized");
                        require(screen.mouseClicked(screen.getGuiLeft()+slot.x+8,screen.getGuiTop()+slot.y+8,0),"Container click rejected");
                    }
                    enter(Stage.ACK);
                }
                case ACK -> {
                    if(inspection==null){inspection=mc.getSingleplayerServer().submit(()->index==0?machine.labelLines().equals(EDITED):player(mc).containerMenu.getCarried().is(Items.DIRT)&&player(mc).containerMenu.getCarried().getCount()==1&&player(mc).containerMenu.getSlot(0).getItem().isEmpty());return;}
                    if(!inspection.isDone())return;
                    boolean ok=inspection.join();inspection=null;if(!ok)return;
                    if(index!=0&&(!mc.player.containerMenu.getCarried().is(Items.DIRT)||mc.player.containerMenu.getCarried().getCount()!=1))return;
                    cases.get(index).getAsJsonObject().addProperty("server_acknowledged_interaction",true);
                    if(mc.screen!=null)mc.screen.onClose();enter(Stage.CLOSE);
                }
                case CLOSE -> {if(mc.screen!=null||ticks<20)return;index++;if(index==NAMES.length){enter(Stage.DONE);finish(mc,null);}else enter(Stage.OPEN);}
                default -> throw new IllegalStateException("Unexpected "+stage);
            }
        }catch(Throwable error){finish(mc,error);}
    }
    private static void boot(Minecraft mc) throws Exception {
        nonce = System.getProperty("iedctr.clientGameplayNonce", "");
        require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"), "Missing run identity");
        directory = mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
            && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Isolation marker mismatch");
        require(mc.level == null && mc.getSingleplayerServer() == null && !Files.exists(directory.resolve("modernui-proof.json"))
            && !Files.exists(directory.resolve("editor.png")), "Refusing existing world/proof");
        if (Files.exists(directory.resolve("saves"))) try (var entries = Files.list(directory.resolve("saves"))) {
            require(entries.findAny().isEmpty(), "Existing saves forbidden");
        }
        packaged = PackagedRuntimeProof.verify(mc);
        inspectModernUI();
        String name = "iedctr-modernui-" + nonce;
        mc.options.pauseOnLostFocus = false; enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(0L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }
    private static void setup(Minecraft mc){
        var p=player(mc);p.setGameMode(GameType.SURVIVAL);var level=p.serverLevel();
        for(int x=-2;x<=4;x++)for(int z=-1;z<=4;z++)level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),3);
        p.connection.teleport(.5,80,2.5,180,0);
        level.setBlock(POS,ModBlocks.LABELED_CRATE.get().defaultBlockState(),3);
        machine=(MachineBlockEntity)level.getBlockEntity(POS);require(machine!=null,"Missing crate");
        machine.setLabeledCrateLabel(ORIGINAL.get(0),ORIGINAL.get(1));level.setBlock(CHEST,Blocks.CHEST.defaultBlockState(),3);
        level.setBlock(FURNACE,ModBlocks.SMALL_ELECTRICAL_FURNACE.get().defaultBlockState(),3);
        furnace=(MachineBlockEntity)level.getBlockEntity(FURNACE);require(furnace!=null,"Missing electrical furnace");
    }
    private static JsonObject inspectModernUI() throws Exception {
        var mod=ModList.get().getModFileById("modernui");require(mod!=null,"ModernUI absent");
        var jar=mod.getFile().getFilePath().toRealPath();
        require(jar.equals(Path.of(System.getProperty("iedctr.modernUIJar")).toRealPath()),"Wrong ModernUI artifact");
        var digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-512").digest(Files.readAllBytes(jar)));
        require(digest.equals(System.getProperty("iedctr.modernUIJarSha512")),"ModernUI hash differs");
        var config=Class.forName("icyllis.modernui.mc.neoforge.ConfigImpl").getField("CLIENT").get(null);
        var value=config.getClass().getField("mAdditionalBlurEffect").get(config);
        boolean setting=(Boolean)value.getClass().getMethod("get").invoke(value);
        boolean expected=Boolean.parseBoolean(System.getProperty("iedctr.expectedAdditionalBlur"));
        require(setting==expected,"Loaded config differs");
        var type=Class.forName("icyllis.modernui.mc.BlurHandler");
        require(type.getResource("BlurHandler.class").toURI().equals(mod.getFile().findResource("icyllis/modernui/mc/BlurHandler.class").toUri()),"Blur class not from pinned jar");
        boolean effective=type.getField("sBlurForVanillaScreens").getBoolean(null);
        require(effective==expected&&type.getField("sBlurEffect").getBoolean(null),"Effective blur settings differ");
        var result=new JsonObject();result.addProperty("modernui_jar_sha512",digest);result.addProperty("additional_blur_config",setting);result.addProperty("additional_blur_effective",effective);
        var instance=type.getField("INSTANCE").get(null);
        for(String key:List.of("mBlurring","mBlurEffectLoaded","mBlurEffect")){var f=type.getDeclaredField(key);f.setAccessible(true);var v=f.get(instance);result.addProperty(key,v instanceof Boolean?(Boolean)v:v!=null);}
        if(stage==Stage.WAIT&&expected)require(result.get("mBlurring").getAsBoolean()&&result.get("mBlurEffect").getAsBoolean(),"Additional blur not armed/loaded at capture");
        result.addProperty("blur_process_call_instrumented",false);return result;
    }
    private static ServerPlayer player(Minecraft mc){var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);require(p!=null,"Player missing");return p;}
    private static void enter(Stage next){stage=next;ticks=0;inspection=null;}
    private static void finish(Minecraft mc,Throwable error){
        complete=true;
        try{if(directory!=null){var proof=new JsonObject();proof.addProperty("suite","modernui-native-diagnostic-v1");proof.addProperty("status",error==null?"observed":"failed");proof.addProperty("nonce",nonce);proof.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));proof.add("packaged_runtime",packaged);proof.add("cases",cases);proof.addProperty("visual_acceptance",false);proof.addProperty("physical_os_input",false);if(error!=null)proof.addProperty("error",error.toString());Files.writeString(directory.resolve("modernui-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");}}
        catch(Exception e){EngineersDecorReforged.LOGGER.error("ModernUI proof error",e);}
        if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_MODERNUI OBSERVED nonce={}",nonce);else EngineersDecorReforged.LOGGER.error("IEDCTR_MODERNUI FAILED "+stage,error);mc.stop();
    }
    private static void require(boolean b,String message){PackagedRuntimeProof.require(b,message);}
}
