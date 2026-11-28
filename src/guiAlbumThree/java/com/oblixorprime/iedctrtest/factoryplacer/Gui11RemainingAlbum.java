package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.LabeledCrateEditScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.LabeledCrateLabelClientBridge;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.TreeCutterV3ReferenceGeometry;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;

/** Additional exact-JAR observations in the same disposable integrated world. */
final class Gui11RemainingAlbum {
    private record Panel(String id, MachineKind kind, Block block, String screen, int machineSlots) {}
    private static final List<Panel> PANELS = List.of(
        new Panel("04", MachineKind.FACTORY_PLACER, ModBlocks.FACTORY_PLACER.get(), "FactoryPlacerV2ReferenceScreen", 18),
        new Panel("05", MachineKind.SMALL_FREEZER, ModBlocks.SMALL_FREEZER.get(), "ThermalVerticalScreen", 3),
        new Panel("06", MachineKind.SMALL_MINERAL_SMELTER, ModBlocks.SMALL_MINERAL_SMELTER.get(), "ThermalVerticalScreen", 2),
        new Panel("09", MachineKind.SMALL_BLOCK_BREAKER, ModBlocks.SMALL_BLOCK_BREAKER.get(), "MachineScreen$SmallBlockBreakerScreen", 1),
        new Panel("10", MachineKind.SMALL_TREE_CUTTER, ModBlocks.SMALL_TREE_CUTTER.get(), "TreeCutterV3ReferenceScreen", 1),
        new Panel("12", MachineKind.SMALL_MILKING_MACHINE, ModBlocks.SMALL_MILKING_MACHINE.get(), "MachineScreen$SmallMilkingMachineScreen", 2),
        new Panel("13", MachineKind.SMALL_FLUID_FUNNEL, ModBlocks.SMALL_FLUID_FUNNEL.get(), "MachineScreen$SmallFluidFunnelScreen", 2),
        new Panel("14", MachineKind.PASSIVE_FLUID_ACCUMULATOR, ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get(), "MachineScreen$PassiveFluidAccumulatorScreen", 2),
        new Panel("15", MachineKind.FLUID_BARREL, ModBlocks.FLUID_BARREL.get(), "BarrelReferenceScreen", 2));
    private static final String CLIENT = "com.oblixorprime.engineersdecorreforged.client.";
    private static final String LABEL = "V5C CONFIRMED";
    private static final BlockPos CRATE = new BlockPos(8, 80, 0);
    private enum Stage { OPEN, WAIT, TREE_MISS, TREE_PICKUP, TREE_PLACE, CLOSE, EDITOR, EDITOR_ACK, DONE }
    private final Path directory;
    private final UUID playerId;
    private final NativeLibrary x11, xtest;
    private final Pointer display;
    final JsonArray cases = new JsonArray();
    private final JsonArray treeInputs = new JsonArray();
    private Stage stage = Stage.OPEN;
    private CompletableFuture<?> pending;
    private CompletableFuture<Boolean> serverEvidence;
    private int ticks, index, treeClicks;
    private boolean sent;
    private JsonObject editor;

    Gui11RemainingAlbum(Path directory, UUID playerId, NativeLibrary x11, NativeLibrary xtest, Pointer display) {
        this.directory = directory;
        this.playerId = playerId;
        this.x11 = x11;
        this.xtest = xtest;
        this.display = display;
    }

    boolean tick(Minecraft mc) throws Exception {
        require(++ticks < 600, "V5C remaining album timeout at panel " + index + "/" + stage);
        if (pending != null) {
            if (!pending.isDone()) return false;
            pending.join();
            pending = null;
        }
        switch (stage) {
            case OPEN -> {
                if (index == PANELS.size()) {
                    pending = mc.getSingleplayerServer().submit(() -> player(mc).connection.teleport(8.5,80,2.5,180,0));
                    enter(Stage.EDITOR);
                    return false;
                }
                Panel panel = PANELS.get(index);
                BlockPos pos = position();
                pending = mc.getSingleplayerServer().submit(() -> {
                    var p = player(mc);
                    p.serverLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                    p.serverLevel().setBlock(pos, panel.block.defaultBlockState(), 3);
                    p.connection.teleport(pos.getX()+.5,80,pos.getZ()+2.5,180,0);
                    var machine = machine(mc, pos);
                    if (panel.kind == MachineKind.SMALL_TREE_CUTTER) {
                        machine.setItem(0, new ItemStack(Items.OAK_LOG, 4));
                        p.getInventory().setItem(9, ItemStack.EMPTY);
                    }
                    p.openMenu(machine);
                });
                enter(Stage.WAIT);
            }
            case WAIT -> {
                if (!(mc.screen instanceof AbstractContainerScreen<?> screen) ||
                    !(mc.player.containerMenu instanceof MachineMenu menu)) return false;
                if (ticks == 2 || ticks == 5) move(mc, ticks == 2 ? 12 : 8, ticks == 2 ? 12 : 8);
                if (ticks < 35) return false;
                Panel panel = PANELS.get(index);
                require(menu.kind()==panel.kind && menu.gui11V5CPresentation(), "Wrong V5C menu for "+panel.kind);
                require(menu.visibleMachineSlots()==panel.machineSlots && menu.slots.size()==panel.machineSlots+36,
                    "Wrong menu slot counts for "+panel.kind);
                int menuId = menu.containerId;
                if (!server(mc, () -> {
                    var current = player(mc).containerMenu;
                    return current instanceof MachineMenu m && m.containerId==menuId && m.kind()==panel.kind
                        && m.slots.size()==panel.machineSlots+36;
                })) return false;
                var row = observation(mc,panel.id,panel.kind.registryName(),panel.screen);
                row.addProperty("server_menu_acknowledged", true);
                row.addProperty("menu_id", menuId);
                row.addProperty("machine_slots", menu.visibleMachineSlots());
                row.addProperty("gui_scale",mc.getWindow().getGuiScale());
                row.addProperty("panel_left",screen.getGuiLeft());
                row.addProperty("panel_top",screen.getGuiTop());
                row.addProperty("panel_width",screen.getXSize());
                row.addProperty("panel_height",screen.getYSize());
                var slots = new JsonArray();
                for (var slot : menu.slots) {
                    var s = new JsonObject();
                    s.addProperty("index", slot.index);
                    s.addProperty("inventory_index", slot.getContainerSlot());
                    s.addProperty("x", slot.x);
                    s.addProperty("y", slot.y);
                    s.addProperty("active", slot.isActive());
                    s.addProperty("count", slot.getItem().getCount());
                    slots.add(s);
                }
                row.add("slots",slots);
                capture(mc,row,panel.kind.registryName());
                cases.add(row);
                if (panel.kind==MachineKind.SMALL_TREE_CUTTER) {
                    row.add("native_inventory",treeInputs);
                    enter(Stage.TREE_MISS);
                } else close(mc);
            }
            case TREE_MISS, TREE_PICKUP, TREE_PLACE -> tree(mc);
            case CLOSE -> {
                if (mc.screen==null && ticks>=20) {
                    index++;
                    enter(Stage.OPEN);
                }
            }
            case EDITOR -> {
                if (ticks==1) {
                    // Invoke the same registered client bridge as the Shift-use label route.
                    LabeledCrateLabelClientBridge.open(CRATE,List.of("V5C","Qualification"));
                    return false;
                }
                if (!(mc.screen instanceof LabeledCrateEditScreen screen)) return false;
                var fields=screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
                var buttons=screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                    .filter(widget -> !(widget instanceof EditBox)).toList();
                require(fields.size()==2 && buttons.size()==2,"Label editor loses two fields or buttons");
                if (ticks==3) fields.getFirst().setValue(LABEL);
                if (ticks==5 || ticks==8) move(mc,ticks==5?12:8,ticks==5?12:8);
                if (ticks<35) return false;
                editor=observation(mc,"18","labeled-crate-label-editor","LabeledCrateEditScreen");
                editor.addProperty("opening","registered label client bridge");
                editor.addProperty("typed_label",fields.getFirst().getValue());
                capture(mc,editor,"labeled-crate-label-editor");
                var confirm=buttons.getFirst();
                moveGui(mc,confirm.getX()+confirm.getWidth()/2.0,confirm.getY()+confirm.getHeight()/2.0);
                enter(Stage.EDITOR_ACK);
            }
            case EDITOR_ACK -> {
                if (ticks < 4) return false;
                if (!sent) {
                    require(mc.screen instanceof LabeledCrateEditScreen,"Editor closed before click");
                    click();
                    sent = true;
                    return false;
                }
                if (mc.screen!=null) return false;
                if (!server(mc, () -> machine(mc,CRATE).labelLines().getFirst().equals(LABEL))) return false;
                editor.addProperty("server_confirmed", true);
                cases.add(editor);
                enter(Stage.DONE);
            }
            case DONE -> {return true;}
        }
        return stage == Stage.DONE;
    }

    private void tree(Minecraft mc) throws Exception {
        require(mc.screen instanceof AbstractContainerScreen<?> && mc.player.containerMenu instanceof MachineMenu,
            "Tree Cutter closed during native inventory test");
        var screen=(AbstractContainerScreen<?>)mc.screen;
        var menu=(MachineMenu)mc.player.containerMenu;
        boolean miss=stage==Stage.TREE_MISS;
        boolean place=stage==Stage.TREE_PLACE;
        int target=place?1:0;
        var slot=menu.slots.get(target);
        var view=TreeCutterV3ReferenceGeometry.fit(screen.width,screen.height);
        double x=view.screenX(slot.x+(miss?-3:8));
        double y=view.screenY(slot.y+8);
        if (ticks==1) moveGui(mc,x,y);
        if (ticks<6) return;
        if (!sent) {
            var hovered=screen.getSlotUnderMouse();
            require(miss?hovered==null:hovered==slot,"Tree Cutter native hover differs for "+stage);
            if (!miss && !place) {
                var hover=new JsonObject();
                capture(mc,hover,"small_tree_cutter-output-hover");
                cases.get(cases.size()-1).getAsJsonObject().add("output_hover_capture",hover);
            }
            click();
            treeClicks++;
            sent=true;
            return;
        }
        int output=miss?4:0, carried=miss||place?0:4, playerSlot=place?4:0;
        if (ticks<12 || menu.slots.get(0).getItem().getCount()!=output ||
            menu.getCarried().getCount()!=carried || menu.slots.get(1).getItem().getCount()!=playerSlot) return;
        int menuId=menu.containerId;
        if (!server(mc, () -> {
            var serverMenu=player(mc).containerMenu;
            return serverMenu.containerId==menuId && serverMenu.slots.get(0).getItem().getCount()==output
                && serverMenu.getCarried().getCount()==carried && serverMenu.slots.get(1).getItem().getCount()==playerSlot;
        })) return;
        var witness=new JsonObject();
        witness.addProperty("step",stage.name());
        witness.addProperty("output_count",output);
        witness.addProperty("carried_count",carried);
        witness.addProperty("player_count",playerSlot);
        witness.addProperty("server_acknowledged",true);
        treeInputs.add(witness);
        if (miss) enter(Stage.TREE_PICKUP);
        else if (!place) enter(Stage.TREE_PLACE);
        else {
            require(treeClicks==3,"Expected exactly three Tree Cutter native clicks");
            close(mc);
        }
    }

    private JsonObject observation(Minecraft mc,String id,String name,String screen) throws Exception {
        require(mc.screen.getClass().getName().equals(CLIENT+screen),"Wrong screen class for "+name);
        var mod=ModList.get().getModFileById(EngineersDecorReforged.MOD_ID).getFile();
        String entry=mc.screen.getClass().getName().replace('.','/')+".class";
        var url=mc.screen.getClass().getResource("/"+entry);
        require(url!=null && url.toURI().equals(mod.findResource(entry).toUri()),"Screen class is not from the loaded JAR");
        var texture=ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,"textures/gui/gui11_v5c/"+id+".png");
        var active=mc.getResourceManager().getResource(texture).orElseThrow();
        require(active.sourcePackId().equals("mod/"+EngineersDecorReforged.MOD_ID),"Overridden faceplate "+id);
        String asset="assets/"+texture.getNamespace()+"/"+texture.getPath();
        var result=new JsonObject();
        result.addProperty("case",name);
        result.addProperty("id",id);
        result.addProperty("screen",mc.screen.getClass().getName());
        result.addProperty("observed_utc",Instant.now().toString());
        try (var zip=new ZipFile(mod.getFilePath().toFile()); var code=url.openStream(); var pixels=active.open()) {
            String screenSha=PackagedRuntimeProof.hash(code.readAllBytes());
            String textureSha=PackagedRuntimeProof.hash(pixels.readAllBytes());
            try (var classEntry=zip.getInputStream(zip.getEntry(entry)); var pngEntry=zip.getInputStream(zip.getEntry(asset))) {
                require(screenSha.equals(PackagedRuntimeProof.hash(classEntry.readAllBytes())),"Screen bytes mismatch");
                require(textureSha.equals(PackagedRuntimeProof.hash(pngEntry.readAllBytes())),"PNG bytes mismatch");
            }
            result.addProperty("screen_class_sha256",screenSha);
            result.addProperty("texture_sha256",textureSha);
        }
        return result;
    }

    private void capture(Minecraft mc,JsonObject record,String name) throws Exception {
        Path path=directory.resolve(name+".png");
        try (var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {image.writeToFile(path);}
        record.addProperty("capture",path.getFileName().toString());
        record.addProperty("capture_sha256",PackagedRuntimeProof.hash(Files.readAllBytes(path)));
    }

    private boolean server(Minecraft mc, java.util.function.BooleanSupplier predicate) {
        if (serverEvidence==null) {
            serverEvidence=mc.getSingleplayerServer().submit(predicate::getAsBoolean);
            return false;
        }
        if (!serverEvidence.isDone()) return false;
        require(serverEvidence.join(),"Server evidence differs at panel "+index+"/"+stage);
        serverEvidence=null;
        return true;
    }

    private BlockPos position(){return new BlockPos((index%5)*3,80,7+(index/5)*3);}
    private ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);}
    private MachineBlockEntity machine(Minecraft mc,BlockPos pos) {
        return (MachineBlockEntity)player(mc).serverLevel().getBlockEntity(pos);
    }
    private void enter(Stage next){stage=next;ticks=0;sent=false;serverEvidence=null;}
    private void close(Minecraft mc){mc.screen.onClose();enter(Stage.CLOSE);}
    private static void require(boolean condition,String message){PackagedRuntimeProof.require(condition,message);}
    private void moveGui(Minecraft mc,double x,double y) {
        move(mc,(int)Math.round(x*mc.getWindow().getScreenWidth()/mc.screen.width),
            (int)Math.round(y*mc.getWindow().getScreenHeight()/mc.screen.height));
    }
    private void move(Minecraft mc,int x,int y) {
        int[] left={0},top={0};
        GLFW.glfwGetWindowPos(mc.getWindow().getWindow(),left,top);
        require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(new Object[]{display,-1,left[0]+x,top[0]+y,new NativeLong(0)})!=0,
            "Native mouse motion failed");
        x11.getFunction("XFlush").invokeInt(new Object[]{display});
    }
    private void click() {
        for (int pressed=1;pressed>=0;pressed--)
            require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{display,1,pressed,new NativeLong(0)})!=0,"Native click failed");
        x11.getFunction("XFlush").invokeInt(new Object[]{display});
    }
}
