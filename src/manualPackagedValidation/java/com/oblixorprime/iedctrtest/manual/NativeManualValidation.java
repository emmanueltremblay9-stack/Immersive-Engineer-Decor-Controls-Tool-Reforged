package com.oblixorprime.iedctrtest.manual;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.SpecialManualElement;
import blusunrize.lib.manual.gui.ManualScreen;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Real IE manual pagination/rendering on an exact ordinary JAR; no OS-input claim. */
@EventBusSubscriber(modid=ManualValidationMod.MOD_ID, value=Dist.CLIENT)
public final class NativeManualValidation {
    private enum Stage { BOOT, WORLD, SETUP, CAPTURE, ADVANCE, BACKWARD, NEXT, DONE }
    private static Stage stage=Stage.BOOT;
    private static Path directory;
    private static String nonce, expectedBody;
    private static boolean isolated, complete, captureReady, screenRendered;
    private static long deadline;
    private static int ticks, group, startPage, endPage, captured, forward, backward;
    private static ManualInstance manual;
    private static ManualScreen screen;
    private static ManualEntry entry;
    private static CompletableFuture<?> pending;
    private static JsonObject runtime;
    private static final JsonArray PAGES=new JsonArray(), CAPTURES=new JsonArray(), GROUPS=new JsonArray();
    private static final StringBuilder body=new StringBuilder();
    private NativeManualValidation() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("iedctr.manualPackagedNative") || complete) return;
        Minecraft mc=Minecraft.getInstance();
        try {
            if(deadline==0)deadline=System.nanoTime()+600_000_000_000L;
            require(System.nanoTime()<deadline && ++ticks<3600,"Manual timeout: "+stage);
            if(stage==Stage.BOOT) { if(mc.screen instanceof TitleScreen && mc.getOverlay()==null)boot(mc); return; }
            if(stage==Stage.WORLD) {
                if(mc.level==null || mc.player==null || mc.screen!=null)return;
                require(mc.getSingleplayerServer()!=null && mc.player.connection.getConnection().isMemoryConnection(),"Not an isolated integrated world");
                pending=mc.getSingleplayerServer().submit(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    require(p!=null,"Missing integrated player");
                    p.setGameMode(GameType.CREATIVE);
                    p.connection.teleport(.5,80,.5,0,0);
                });
                enter(Stage.SETUP);return;
            }
            require(mc.level!=null && mc.player!=null && mc.getSingleplayerServer()!=null,"World lost");
            if(pending!=null) { if(!pending.isDone())return; pending.join();pending=null; }
            if(mc.getOverlay()!=null)return;
            switch(stage) {
                case SETUP -> {
                    if(ticks<100)return;
                    manual=ManualHelper.getManual();require(manual!=null,"IE manual missing");
                    // getGui performs IE's normal reload and recipe indexing before entry access.
                    screen=manual.getGui(false);require(screen!=null,"IE manual initialization failed");
                    openGroup(mc);
                }
                case CAPTURE -> {
                    require(mc.screen==screen && screen.getCurrentPage()==entry,"Unexpected manual screen/entry");
                    if(ticks>=25)captureReady=true;
                }
                case ADVANCE -> {
                    require(mc.screen==screen && screen.getCurrentPage()==entry,"Unexpected page transition");
                    if(screen.page+1<endPage) {
                        int previous=screen.page;
                        require(screen.mouseScrolled(0,0,0,-1),"Native next-page handler rejected");
                        require(screen.page==previous+1,"Native next-page skipped or repeated");
                        forward++;enter(Stage.CAPTURE);
                    } else {
                        require(captured==endPage-startPage,"Not every page captured");
                        require(compact(body.toString()).contains(expectedBody),"Rendered pages omit or reorder source text");
                        enter(Stage.BACKWARD);
                    }
                }
                case BACKWARD -> {
                    if(ticks<3)return;
                    if(screen.page>startPage) {
                        int previous=screen.page;
                        require(screen.mouseScrolled(0,0,0,1),"Native previous-page handler rejected");
                        require(screen.page==previous-1,"Native previous-page skipped or repeated");
                        backward++;ticks=0;
                    } else {
                        var row=new JsonObject();row.addProperty("entry",entry.getLocation().toString());
                        row.addProperty("start_page",startPage);row.addProperty("end_page_exclusive",endPage);
                        row.addProperty("captured_pages",captured);row.addProperty("body_complete",true);
                        row.addProperty("recipe_present",recipePresent());
                        row.addProperty("navigation_forward",forward);row.addProperty("navigation_backward",backward);
                        GROUPS.add(row);enter(Stage.NEXT);
                    }
                }
                case NEXT -> { if(++group<2)openGroup(mc);else enter(Stage.DONE); }
                case DONE -> finish(mc,null);
                default -> throw new IllegalStateException("Unexpected stage "+stage);
            }
        } catch(Throwable error) { finish(mc,error); }
    }

    @SubscribeEvent public static void beforeFrame(RenderFrameEvent.Pre event) { screenRendered=false; }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void rendered(ScreenEvent.Render.Post event) {
        if(isolated && !complete && stage==Stage.CAPTURE && event.getScreen()==screen
                && Minecraft.getInstance().screen==screen)screenRendered=true;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void afterFrame(RenderFrameEvent.Post event) {
        if(!isolated || complete || !captureReady || !screenRendered || stage!=Stage.CAPTURE)return;
        Minecraft mc=Minecraft.getInstance();
        try {
            require(mc.screen==screen && screen.getCurrentPage()==entry,"Screen changed before capture");
            String name=(group==0?"individual":"category")+"-page-"+screen.page+".png";
            Path imagePath=directory.resolve("manual-packaged-screenshots").resolve(name);
            Files.createDirectories(imagePath.getParent());
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                require(image.getWidth()==mc.getWindow().getWidth()&&image.getHeight()==mc.getWindow().getHeight(),"Screenshot dimensions differ");
                image.writeToFile(imagePath);
            }
            var shot=new JsonObject();
            shot.addProperty("path","manual-packaged-screenshots/"+name);
            shot.addProperty("sha256",PackagedRuntimeProof.hash(Files.readAllBytes(imagePath)));
            shot.addProperty("width",mc.getWindow().getWidth());shot.addProperty("height",mc.getWindow().getHeight());
            shot.addProperty("entry",entry.getLocation().toString());shot.addProperty("page",screen.page);
            shot.addProperty("phase","RenderFrameEvent.Post:LOWEST");
            CAPTURES.add(shot); // Preserve the actual rendered frame even when a bounds assertion fails.
            var row=pageProof(mc);
            row.add("capture",shot.deepCopy());PAGES.add(row);captured++;
            enter(Stage.ADVANCE);
        } catch(Throwable error) { finish(mc,error); }
    }

    private static void boot(Minecraft mc)throws Exception {
        nonce=System.getProperty("iedctr.clientGameplayNonce","");
        require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha","").matches("[a-f0-9]{40}"),"Missing source/nonce");
        directory=mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory","")).toRealPath())
                && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce),"Isolation marker mismatch");
        require(mc.level==null&&mc.getSingleplayerServer()==null&&!Files.exists(directory.resolve("client-manual-packaged-result.json"))
                &&!Files.exists(directory.resolve("manual-packaged-screenshots")),"Existing world/evidence forbidden");
        if(Files.exists(directory.resolve("saves")))try(var files=Files.list(directory.resolve("saves"))) {require(files.findAny().isEmpty(),"Existing saves forbidden");}
        int width=Integer.getInteger("iedctr.requestedWidth",0),height=Integer.getInteger("iedctr.requestedHeight",0),scale=Integer.getInteger("iedctr.requestedGuiScale",0);
        require(width==1280 && ((height==720&&scale==2)||(height==480&&scale==1)),"Unsupported profile");
        require(mc.getWindow().getWidth()==width&&mc.getWindow().getHeight()==height&&mc.getWindow().getGuiScale()==scale,"Requested viewport/scale clamped");
        runtime=PackagedRuntimeProof.verify(mc);
        String language=mc.getLanguageManager().getSelected();
        var id=ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,"manual/"+language+"/small_milking_machine.txt");
        String text;
        try(var input=mc.getResourceManager().getResourceOrThrow(id).open()) {text=new String(input.readAllBytes(),StandardCharsets.UTF_8);}
        text=text.substring(text.indexOf('\n')+1);text=text.substring(text.indexOf('\n')+1);
        expectedBody=compact(text.replaceAll("<[^>]+>"," "));
        require(expectedBody.length()>1000,"Incomplete expected source text");
        isolated=true;mc.options.pauseOnLostFocus=false;
        String name="iedctr-manual-pagination-"+nonce;enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
    }

    private static void openGroup(Minecraft mc) {
        String name=group==0?"small_milking_machine":"workshop_machines";
        entry=manual.getEntry(ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,name));
        require(entry!=null,"Missing manual entry: "+name);
        require(entry.getTreeNode()!=null,"Missing manual navigation node");
        screen.setCurrentNode(entry.getTreeNode());
        if(group==0) {startPage=0;endPage=entry.getPageCount();}
        else {
            require(entry.hasAnchor("small_milking_machine")&&entry.hasAnchor("small_tree_cutter"),"Category anchors missing");
            startPage=entry.getPageForAnchor("small_milking_machine");
            endPage=entry.getPageForAnchor("small_tree_cutter");
        }
        require(endPage>startPage && endPage-startPage<30,"Invalid Milking page interval");
        screen.page=startPage;mc.setScreen(screen);screen.fullInit();
        require(recipePresent(),"Milking recipe special missing on first page");
        captured=0;forward=0;backward=0;body.setLength(0);enter(Stage.CAPTURE);
    }

    private static boolean recipePresent() {
        var special=entry.getSpecialsByPage().get(startPage);
        return special!=null && special.getPixelsTaken()>0 && special.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT).contains("crafting");
    }

    @SuppressWarnings("unchecked")
    private static JsonObject pageProof(Minecraft mc)throws Exception {
        require(screen.page>=startPage&&screen.page<endPage,"Unexpected page index");
        List<?> rawPages=(List<?>)field(ManualEntry.class,"pages").get(entry);
        Object page=rawPages.get(screen.page);
        List<String> lines=(List<String>)field(page.getClass(),"renderText").get(page);
        SpecialManualElement special=(SpecialManualElement)field(page.getClass(),"special").get(page);
        require(lines!=null,"Native text was not initialized");
        var lineJson=new JsonArray();var widths=new JsonArray();
        int maxWidth=0;
        for(String line:lines) {int width=manual.fontRenderer().width(line);maxWidth=Math.max(maxWidth,width);lineJson.add(line);widths.add(width);body.append(line).append('\n');}
        int textHeight=lines.size()*manual.fontRenderer().lineHeight;
        int specialHeight=special.getPixelsTaken();
        require(maxWidth<=manual.pageWidth,"Text exceeds manual page width");
        require(textHeight+specialHeight<=manual.pageHeight+1,"Text/recipe exceed manual page height");
        float factor=field(ManualScreen.class,"scaleFactor").getFloat(screen);
        int left=field(ManualScreen.class,"guiLeft").getInt(screen),top=field(ManualScreen.class,"guiTop").getInt(screen);
        int bookWidth=field(ManualScreen.class,"xSize").getInt(screen),bookHeight=field(ManualScreen.class,"ySize").getInt(screen);
        double viewportW=mc.getWindow().getGuiScaledWidth(),viewportH=mc.getWindow().getGuiScaledHeight();
        require(factor>0 && left*factor>=0 && top*factor>=0
                &&(left+bookWidth)*factor<=viewportW+1&&(top+bookHeight)*factor<=viewportH+1,"Manual frame outside viewport: left="+left+", top="+top+", book="+bookWidth+"x"+bookHeight
                        +", factor="+factor+", viewport="+viewportW+"x"+viewportH
                        +", screen="+screen.width+"x"+screen.height+", guiScale="+mc.getWindow().getGuiScale());
        var row=new JsonObject();row.addProperty("entry",entry.getLocation().toString());row.addProperty("page",screen.page);
        row.addProperty("start_page",startPage);row.addProperty("end_page_exclusive",endPage);
        row.add("lines",lineJson);row.add("line_widths",widths);row.addProperty("page_width",manual.pageWidth);
        row.addProperty("page_height",manual.pageHeight);row.addProperty("text_height",textHeight);
        row.addProperty("special_height",specialHeight);row.addProperty("special_above",special.isAbove());
        row.addProperty("bounds_ok",true);row.addProperty("manual_scale_factor",factor);
        row.addProperty("frame_left",left);row.addProperty("frame_top",top);
        return row;
    }
    private static Field field(Class<?> type,String name)throws Exception {var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static String compact(String value) {return value.replaceAll("§.","").replaceAll("\\s+","");}
    private static void enter(Stage next) {stage=next;ticks=0;captureReady=false;screenRendered=false;}
    private static void require(boolean value,String message) {PackagedRuntimeProof.require(value,message);}
    private static void finish(Minecraft mc,Throwable error) {
        if(complete)return;complete=true;
        try {
            if(isolated) {
                var result=new JsonObject();result.addProperty("suite","native-manual-pagination-v1");
                result.addProperty("status",error==null?"PASS":"FAIL");result.addProperty("source_sha",System.getProperty("iedctr.executedSourceSha",""));
                result.addProperty("nonce",nonce);result.addProperty("language",mc.getLanguageManager().getSelected());
                result.addProperty("width",mc.getWindow().getWidth());result.addProperty("height",mc.getWindow().getHeight());
                result.addProperty("gui_scale",mc.getWindow().getGuiScale());
                result.add("runtime",runtime);result.add("pages",PAGES);result.add("captures",CAPTURES);result.add("groups",GROUPS);
                result.addProperty("input_boundary","IE mouseScrolled handler and direct entry selection, not OS/GLFW input");
                if(error!=null)result.addProperty("error",error.toString());
                Files.writeString(directory.resolve("client-manual-packaged-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
            }
        }catch(Exception writing) {writing.printStackTrace();}
        if(error!=null)error.printStackTrace();
        System.out.println("MANUAL_PAGINATION_"+(error==null?"PASS":"FAIL")+":"+nonce);
        if(isolated)mc.stop();
    }
}
