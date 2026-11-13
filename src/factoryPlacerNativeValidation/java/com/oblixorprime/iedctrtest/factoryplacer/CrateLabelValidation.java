package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.LabeledCrateEditScreen;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Real packaged screen/widgets and packets, not physical desktop input or art acceptance. */
@EventBusSubscriber(modid = FactoryPlacerValidationMod.MOD_ID, value = Dist.CLIENT)
public final class CrateLabelValidation {
    private static final BlockPos POS = new BlockPos(0, 80, 0);
    private static final List<String> ORIGINAL = List.of("Original first", "Original second");
    private static final List<String> EDITED = List.of("Cuivre été", "Réserve Ω");
    private static final List<String> EMPTY = List.of("", "Étain Ω");
    private enum Stage { BOOT, WORLD, OPEN, UNSAVED, CAPTURE, SAVED, CANCELLED, DONE }
    private static Stage stage = Stage.BOOT;
    private static boolean complete;
    private static long deadline;
    private static int ticks, cancelChecks;
    private static UUID playerId;
    private static Path directory;
    private static String nonce;
    private static JsonObject packaged;
    private static final JsonArray cases = new JsonArray();
    private static MachineBlockEntity machine;
    private static LabeledCrateEditScreen screen;
    private static CompletableFuture<?> pending;
    private static CompletableFuture<List<String>> inspection;
    private CrateLabelValidation() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("iedctr.crateLabelPackagedNative") || complete) return;
        var mc = Minecraft.getInstance();
        try {
            if (deadline == 0) deadline = System.nanoTime() + 900_000_000_000L;
            require(System.nanoTime() < deadline && ++ticks < 1800, "Crate native timeout at " + stage);
            if (stage == Stage.BOOT) {
                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) boot(mc);
                return;
            }
            if (stage == Stage.WORLD) {
                if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
                require(mc.getSingleplayerServer() != null && mc.player.connection.getConnection().isMemoryConnection(), "Only isolated integrated world permitted");
                playerId = mc.player.getUUID();
                pending = mc.getSingleplayerServer().submit(() -> setup(mc));
                enter(Stage.OPEN); return;
            }
            require(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null, "World disconnected");
            if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
            if (mc.getOverlay() != null) return;
            switch (stage) {
                case OPEN -> {
                    if (!(mc.level.getBlockEntity(POS) instanceof MachineBlockEntity client) || !client.labelLines().equals(ORIGINAL)) return;
                    screen = new LabeledCrateEditScreen(POS, ORIGINAL); mc.setScreen(screen);
                    expect(ORIGINAL); edit(EDITED); resize(mc, 800, 400); expect(EDITED);
                    resize(mc, 640, 360); expect(EDITED); cases.add("both_lines_survive_repeated_resize");
                    edit(EMPTY); resize(mc, 800, 400); expect(EMPTY);
                    resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()); expect(EMPTY);
                    cases.add("empty_and_unicode_survive_resize"); enter(Stage.UNSAVED);
                }
                case UNSAVED -> {
                    if (!serverEquals(mc, ORIGINAL)) return;
                    require(mc.screen == screen, "Resize closed editor"); cases.add("resize_does_not_save"); enter(Stage.CAPTURE);
                }
                case CAPTURE -> {
                    if (ticks < 5) return;
                    expect(EMPTY);
                    try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(directory.resolve("crate-label-resized.png")); }
                    require(screen.keyPressed(257, 0, 0), "Enter was not consumed");
                    require(mc.screen == null, "Save did not close editor"); enter(Stage.SAVED);
                }
                case SAVED -> {
                    if (!serverEquals(mc, EMPTY)) return;
                    cases.add("enter_saves_server_authoritative_text");
                    screen = new LabeledCrateEditScreen(POS, EMPTY); mc.setScreen(screen); expect(EMPTY);
                    edit(List.of("Do not save", "Annuler")); resize(mc, 640, 360);
                    expect(List.of("Do not save", "Annuler"));
                    var buttons = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
                    require(buttons.size() == 2, "Expected Done and Cancel buttons");
                    var cancel = buttons.get(1);
                    require(screen.mouseClicked(cancel.getX() + 2, cancel.getY() + 2, 0), "Cancel click rejected");
                    require(mc.screen == null, "Cancel did not close editor"); enter(Stage.CANCELLED);
                }
                case CANCELLED -> {
                    if (ticks < 10 || !serverEquals(mc, EMPTY)) return;
                    if (++cancelChecks < 3) return;
                    cases.add("cancel_does_not_save_server_text");
                    screen = new LabeledCrateEditScreen(POS, List.of("New first", "New second")); mc.setScreen(screen);
                    expect(List.of("New first", "New second")); cases.add("new_screen_uses_its_own_initial_values");
                    screen.onClose(); enter(Stage.DONE); finish(mc, null);
                }
                default -> throw new IllegalStateException("Unexpected native stage " + stage);
            }
        } catch (Throwable error) { finish(mc, error); }
    }
    private static void boot(Minecraft mc) throws Exception {
        nonce = System.getProperty("iedctr.clientGameplayNonce", "");
        require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"), "Missing run identity");
        directory = mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
            && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Isolation marker mismatch");
        require(mc.level == null && mc.getSingleplayerServer() == null && !Files.exists(directory.resolve("crate-label-proof.json"))
            && !Files.exists(directory.resolve("crate-label-resized.png")), "Refusing existing world/proof");
        if (Files.exists(directory.resolve("saves"))) try (var entries = Files.list(directory.resolve("saves"))) {
            require(entries.findAny().isEmpty(), "Existing saves forbidden");
        }
        packaged = PackagedRuntimeProof.verify(mc);
        String name = "iedctr-crate-label-" + nonce;
        mc.options.pauseOnLostFocus = false; enter(Stage.WORLD);
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(0L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }
    private static void setup(Minecraft mc) {
        var p = player(mc); p.setGameMode(GameType.SURVIVAL); var level = p.serverLevel();
        for (int x=-2; x<=2; x++) for (int z=-1; z<=4; z++) level.setBlock(new BlockPos(x,79,z), Blocks.STONE.defaultBlockState(), 3);
        p.connection.teleport(.5,80,2.5,180,0);
        level.setBlock(POS, ModBlocks.LABELED_CRATE.get().defaultBlockState(), 3);
        require(level.getBlockEntity(POS) instanceof MachineBlockEntity, "Missing crate");
        machine = (MachineBlockEntity)level.getBlockEntity(POS); machine.setLabeledCrateLabel(ORIGINAL.get(0), ORIGINAL.get(1));
    }
    private static List<EditBox> fields() {
        var fields = screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
        require(fields.size()==2, "Expected two real EditBox widgets"); return fields;
    }
    private static void edit(List<String> values) { var fields=fields(); for(int i=0;i<2;i++) fields.get(i).setValue(values.get(i)); }
    private static void expect(List<String> expected) {
        require(fields().stream().map(EditBox::getValue).toList().equals(expected), "Native field values differ: " + expected);
    }
    private static void resize(Minecraft mc, int width, int height) { screen.resize(mc,width,height); require(screen.width==width && screen.height==height,"Resize dimensions not applied"); }
    private static boolean serverEquals(Minecraft mc, List<String> expected) {
        if (inspection==null) { inspection=mc.getSingleplayerServer().submit(()->List.copyOf(machine.labelLines())); return false; }
        if (!inspection.isDone()) return false;
        var actual=inspection.join(); inspection=null; return actual.equals(expected);
    }
    private static ServerPlayer player(Minecraft mc) { var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId); require(p!=null,"Player vanished"); return p; }
    private static void enter(Stage next) { stage=next; ticks=0; inspection=null; }
    private static void finish(Minecraft mc, Throwable error) {
        complete=true;
        try {
            if(directory!=null) {
                var proof=new JsonObject(); proof.addProperty("suite","native-crate-label-resize-v1");
                proof.addProperty("status",error==null?"passed":"failed"); proof.addProperty("nonce",nonce);
                proof.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha")); proof.add("packaged_runtime",packaged);
                proof.add("cases",cases); proof.addProperty("physical_os_input",false); proof.addProperty("visual_acceptance",false);
                proof.addProperty("resize_method","Screen.resize with real Minecraft and EditBox; no GLFW resize");
                proof.addProperty("selected_language",mc.getLanguageManager().getSelected());
                proof.addProperty("width",mc.getWindow().getWidth()); proof.addProperty("height",mc.getWindow().getHeight());
                if(error==null) proof.addProperty("capture_sha256",PackagedRuntimeProof.hash(Files.readAllBytes(directory.resolve("crate-label-resized.png"))));
                else proof.addProperty("error",error.toString());
                Files.writeString(directory.resolve("crate-label-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
            }
        } catch(Exception writeError) { EngineersDecorReforged.LOGGER.error("Crate label proof write failed",writeError); }
        if(error==null) EngineersDecorReforged.LOGGER.info("IEDCTR_CRATE_LABEL PASSED nonce={}",nonce);
        else EngineersDecorReforged.LOGGER.error("IEDCTR_CRATE_LABEL FAILED at "+stage,error);
        mc.stop();
    }
    private static void require(boolean condition,String message) { PackagedRuntimeProof.require(condition,message); }
}
