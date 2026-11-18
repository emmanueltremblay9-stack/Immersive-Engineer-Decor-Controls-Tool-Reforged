package com.oblixorprime.iedctrtest.heavy;

import com.google.gson.*;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTSupportBlock;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTTopology;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Native scene capture, not a claim of physical item placement or pixel CTM acceptance. */
@EventBusSubscriber(modid = HeavyValidationMod.MOD_ID, value = Dist.CLIENT)
public final class NativeHeavyValidation {
    private static final BlockPos POS = new BlockPos(0, 81, 0);
    private record Scene(String id, Direction axis, int roll, String kind, Direction neighbor) {}
    private static final List<Scene> SCENES = scenes();
    private static final JsonArray CAPTURES = new JsonArray();
    private static Path directory;
    private static String nonce;
    private static UUID playerId;
    private static JsonObject runtime;
    private static CompletableFuture<?> pending;
    private static int sceneIndex = -1, stableTicks, renderedFrames;
    private static long deadline;
    private static boolean booted, initialized, complete;
    private static String previousModelHash;
    private NativeHeavyValidation() {}

    private static List<Scene> scenes() {
        var list = new ArrayList<Scene>();
        for (Direction axis : Direction.values()) for (int roll=0; roll<4; roll++)
            list.add(new Scene("orientation_"+axis.getName()+"_"+roll, axis, roll, "isolated", null));
        for (Direction face : Direction.values())
            list.add(new Scene("anchor_"+face.getName(), Direction.UP, 0, "anchor", face));
        list.add(new Scene("axial_join", Direction.UP, 0, "axial", Direction.UP));
        list.add(new Scene("axial_removed", Direction.UP, 0, "remove", Direction.UP));
        list.add(new Scene("branch_join", Direction.UP, 0, "branch", Direction.EAST));
        list.add(new Scene("branch_removed", Direction.UP, 0, "remove", Direction.EAST));
        return List.copyOf(list);
    }

    @SubscribeEvent public static void rendered(RenderLevelStageEvent event) {
        if (Boolean.getBoolean("iedctr.heavyPackagedNative") && !complete && initialized
                && event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) renderedFrames++;
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("iedctr.heavyPackagedNative") || complete) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (deadline == 0) deadline = System.nanoTime()+900_000_000_000L;
            require(System.nanoTime()<deadline, "Heavy scene timeout at "+sceneIndex);
            if (!booted) {
                if (mc.screen instanceof TitleScreen && mc.getOverlay()==null) boot(mc);
                return;
            }
            if (mc.level==null || mc.player==null || mc.screen!=null || mc.getSingleplayerServer()==null) return;
            require(mc.player.connection.getConnection().isMemoryConnection(), "Only integrated world allowed");
            if (pending!=null) {
                if (!pending.isDone()) return;
                pending.join(); pending=null; stableTicks=0; renderedFrames=0;
            }
            if (!initialized) {
                initialized=true; playerId=mc.player.getUUID(); next(mc); return;
            }
            Scene scene=SCENES.get(sceneIndex);
            look(mc);
            if (!clientMatches(mc,scene)) { stableTicks=0; renderedFrames=0; return; }
            // No manual renderer invalidation: the production neighbor update must reach the client.
            if (++stableTicks<40 || renderedFrames<40) return;
            capture(mc,scene);
            if (sceneIndex+1==SCENES.size()) { runtime=PackagedRuntimeProof.verify(mc); finish(mc,null); }
            else next(mc);
        } catch (Throwable error) { finish(mc,error); }
    }

    private static void boot(Minecraft mc) throws Exception {
        runtime=PackagedRuntimeProof.verify(mc);
        nonce=System.getProperty("iedctr.clientGameplayNonce", "");
        require(nonce.matches("[a-f0-9]{32}") && System.getProperty("iedctr.executedSourceSha", "").matches("[a-f0-9]{40}"), "Identity absent");
        directory=mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath()), "Game directory differs");
        require(Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Nonce differs");
        require(Files.readString(directory.resolve(".iedctr-heavy-xvfb")).equals(nonce+"\n"+System.getenv("DISPLAY")), "Display not owned");
        require(mc.level==null && mc.getSingleplayerServer()==null && !Files.exists(directory.resolve("heavy-captures")), "Existing world/captures forbidden");
        if (Files.exists(directory.resolve("saves"))) try(var entries=Files.list(directory.resolve("saves"))) { require(entries.findAny().isEmpty(), "Existing saves forbidden"); }
        Files.createDirectory(directory.resolve("heavy-captures"));
        mc.options.pauseOnLostFocus=false; mc.options.hideGui=true; mc.options.fov().set(45);
        String name="iedctr-heavy-"+nonce; booted=true;
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name,GameType.SPECTATOR,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
            new WorldOptions(0L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
    }

    private static BlockState state(Scene scene) {
        return ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get().defaultBlockState()
            .setValue(HeavyDoubleTSupportBlock.FACING,scene.axis()).setValue(HeavyDoubleTSupportBlock.ROLL,scene.roll());
    }
    private static ServerPlayer player(Minecraft mc) {
        var p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId); require(p!=null,"Player vanished"); return p;
    }
    private static void next(Minecraft mc) {
        sceneIndex++; Scene scene=SCENES.get(sceneIndex); stableTicks=0; renderedFrames=0;
        pending=mc.getSingleplayerServer().submit(()->{
            var p=player(mc); var level=p.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,mc.getSingleplayerServer());
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,mc.getSingleplayerServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,mc.getSingleplayerServer());
            level.setDayTime(6000); level.setWeatherParameters(6000,0,false,false);
            p.setGameMode(GameType.SPECTATOR); p.getInventory().clearContent();
            // View anchors from the support side, not from behind their opaque backing block.
            // Eye height is explicit so UP's view stays above the fixture floor.
            Vec3 eye=scene.kind().equals("anchor") ? anchorEye(scene.neighbor()) : new Vec3(3.8,83.62,5.5);
            p.connection.teleport(eye.x,eye.y-p.getEyeHeight(),eye.z,0,0);
            if (scene.kind().equals("remove")) {
                require(level.getBlockState(POS).equals(state(scene)),"Removal must preserve the existing center state");
                level.setBlock(POS.relative(scene.neighbor()),Blocks.AIR.defaultBlockState(),3);
            } else {
                for (int x=-3;x<=3;x++) for (int y=-3;y<=3;y++) for (int z=-3;z<=3;z++)
                    level.setBlock(POS.offset(x,y,z),y==-3?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                level.setBlock(POS,state(scene),3);
                if (scene.kind().equals("anchor")) level.setBlock(POS.relative(scene.neighbor()),Blocks.STONE.defaultBlockState(),3);
                if (scene.kind().equals("axial")) level.setBlock(POS.relative(scene.neighbor()),state(scene),3);
                if (scene.kind().equals("branch")) level.setBlock(POS.relative(scene.neighbor()),state(scene).setValue(HeavyDoubleTSupportBlock.FACING,Direction.EAST),3);
            }
        });
    }
    private static Vec3 anchorEye(Direction face) {
        Vec3 center=Vec3.atCenterOf(POS);
        return switch(face.getAxis()) {
            case X -> center.add(-face.getStepX()*3.5,2.0,4.0);
            case Y -> center.add(3.0,-face.getStepY()*1.75,4.0);
            case Z -> center.add(4.0,2.0,-face.getStepZ()*3.5);
        };
    }
    private static boolean clientMatches(Minecraft mc,Scene scene) {
        if (!mc.level.getBlockState(POS).equals(state(scene))) return false;
        for (Direction face : Direction.values()) {
            var actual=mc.level.getBlockState(POS.relative(face));
            if (face==scene.neighbor() && scene.kind().equals("anchor")) { if(!actual.is(Blocks.STONE))return false; }
            else if (face==scene.neighbor() && (scene.kind().equals("axial")||scene.kind().equals("branch"))) {
                var expected=scene.kind().equals("axial")?state(scene):state(scene).setValue(HeavyDoubleTSupportBlock.FACING,Direction.EAST);
                if(!actual.equals(expected))return false;
            } else if(!actual.isAir()) return false;
        }
        return true;
    }
    private static void look(Minecraft mc) {
        Vec3 delta=Vec3.atCenterOf(POS).subtract(mc.player.getEyePosition());
        mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
        mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
    }
    private static void capture(Minecraft mc,Scene scene) throws Exception {
        var blockState=mc.level.getBlockState(POS);
        var topology=HeavyDoubleTTopology.resolve(mc.level,POS,blockState);
        int bit=scene.neighbor()==null?0:1<<scene.neighbor().ordinal();
        require(topology.anchor()==(scene.kind().equals("anchor")?bit:0),"Client anchor topology differs");
        require(topology.axial()==(scene.kind().equals("axial")?bit:0),"Client axial topology differs");
        require(topology.junction()==(scene.kind().equals("branch")?bit:0),"Client branch topology differs");
        var model=mc.getBlockRenderer().getBlockModel(blockState);
        require(model.getClass().getName().contains("HeavyDoubleTConnectedModels"),"Connected model wrapper missing");
        ModelData data=model.getModelData(mc.level,POS,blockState,ModelData.EMPTY);
        var quads=new ArrayList<BakedQuad>();
        quads.addAll(model.getQuads(blockState,null,RandomSource.create(0),data,null));
        for(Direction face:Direction.values())quads.addAll(model.getQuads(blockState,face,RandomSource.create(0),data,null));
        require(!quads.isEmpty(),"No baked Heavy geometry");
        var bytes=new java.io.ByteArrayOutputStream();
        for(var quad:quads) {
            require(!quad.getSprite().contents().name().getPath().equals("missingno"),"Missing sprite");
            int[] vertices=quad.getVertices();require(vertices.length%4==0,"Malformed quad");
            int stride=vertices.length/4;require(stride>=6,"Missing vertex UVs");
            for(int v=0;v<4;v++)for(int offset:new int[]{0,1,2,4,5})
                require(Float.isFinite(Float.intBitsToFloat(vertices[v*stride+offset])),"Nonfinite baked position/UV");
            for(int value:vertices)bytes.writeBytes(ByteBuffer.allocate(4).putInt(value).array());
        }
        String modelHash=PackagedRuntimeProof.hash(bytes.toByteArray());
        if(scene.kind().equals("remove"))require(previousModelHash!=null&&!modelHash.equals(previousModelHash),"Client baked geometry did not change after neighbor removal");
        previousModelHash=modelHash;
        Path image=directory.resolve("heavy-captures").resolve(scene.id()+".png");
        try(var frame=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { frame.writeToFile(image); }
        var row=new JsonObject();row.addProperty("case",scene.id());row.addProperty("axis",scene.axis().getName());row.addProperty("roll",scene.roll());
        row.addProperty("kind",scene.kind());row.addProperty("capture", "heavy-captures/"+scene.id()+".png");
        row.addProperty("sha256",PackagedRuntimeProof.hash(Files.readAllBytes(image)));row.addProperty("quad_count",quads.size());row.addProperty("baked_quads_sha256",modelHash);
        row.addProperty("client_synced",true);row.addProperty("stable_ticks",stableTicks);row.addProperty("rendered_frames",renderedFrames);
        row.addProperty("axial_mask",topology.axial());row.addProperty("junction_mask",topology.junction());row.addProperty("anchor_mask",topology.anchor());
        row.addProperty("collision_boxes",blockState.getCollisionShape(mc.level,POS).toAabbs().size());
        row.addProperty("camera_x",mc.player.getX());row.addProperty("camera_y",mc.player.getY());row.addProperty("camera_z",mc.player.getZ());
        CAPTURES.add(row);
    }
    private static void finish(Minecraft mc,Throwable error) {
        complete=true;
        try {
            if(directory!=null) {
                var p=new JsonObject();p.addProperty("suite","native-heavy-scenes-v1");p.addProperty("status",error==null?"passed_bounded_capture":"failed");
                p.addProperty("nonce",nonce);p.addProperty("executed_source_sha",System.getProperty("iedctr.executedSourceSha"));
                p.addProperty("selected_language",mc.getLanguageManager().getSelected());
                p.addProperty("width",mc.getWindow().getWidth());p.addProperty("height",mc.getWindow().getHeight());
                p.addProperty("actual_integrated_world",mc.getSingleplayerServer()!=null);p.addProperty("visual_acceptance",false);p.addProperty("pixel_ctm_verified",false);
                p.addProperty("physical_item_placement",false);p.addProperty("scene_setup","server setBlock; no runtime/art edits");
                p.addProperty("mesh_refresh_acceptance",false);p.addProperty("mesh_limit","Client world, fresh baked model queries and >=40 rendered frames observed; screenshots need human comparison, no assertion of chunk-mesh identity");
                p.add("packaged_runtime",runtime);p.add("captures",CAPTURES);if(error!=null)p.addProperty("error",error.toString());
                Files.writeString(directory.resolve("heavy-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(p)+"\n");
            }
        } catch(Exception writeError) { EngineersDecorReforged.LOGGER.error("Heavy proof write failed",writeError); }
        if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_HEAVY PASSED nonce={}",nonce);
        else EngineersDecorReforged.LOGGER.error("IEDCTR_HEAVY FAILED scene="+sceneIndex,error);
        mc.stop();
    }
    private static void require(boolean condition,String message) { if(!condition)throw new IllegalStateException(message); }
}
