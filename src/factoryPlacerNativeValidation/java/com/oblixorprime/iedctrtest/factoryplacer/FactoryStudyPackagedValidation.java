package com.oblixorprime.iedctrtest.factoryplacer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.network.MachineActionPayload;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyGeometry;
import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyGeometry.Control;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.neoforged.fml.ModList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Separate opt-in driver: normal registered screens, normal inventory/actions packets, fresh world. */
@EventBusSubscriber(modid = FactoryPlacerValidationMod.MOD_ID, value = Dist.CLIENT)
public final class FactoryStudyPackagedValidation {
    private interface Work { boolean run(Minecraft mc) throws Exception; }
    private record Step(String name, Work work) { }
    private static final List<Step> steps = new ArrayList<>();
    private static final JsonArray observations = new JsonArray(), captures = new JsonArray(), geometry = new JsonArray(), tooltips = new JsonArray(), controls = new JsonArray();
    private static boolean booted, worldReady, complete, tooltipRecording, tooltipInFrame;
    private static int stepIndex, stepTicks, totalTicks, tooltipFrames, opened, filledSlotCount;
    private static long deadline;
    private static Path directory;
    private static String nonce;
    private static String languageProfile;
    private static UUID playerId;
    private static volatile MachineBlockEntity machine;
    private static MachineKind currentKind;
    private static BlockPos currentPos;
    private static JsonObject packaged, tooltipCandidate, tooltipObserved;
    private static double pointerX, pointerY;
    private static NativeLibrary x11, xtest;
    private static Pointer xDisplay;
    private static final JsonArray jeiChecks = new JsonArray();
    private FactoryStudyPackagedValidation() { }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("iedctr.factoryStudyNative") || complete) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (deadline == 0) deadline = System.nanoTime() + 1_200_000_000_000L;
            require(System.nanoTime() < deadline, "Twenty-minute native validation timeout");
            totalTicks++;
            if (!booted) {
                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) boot(mc);
                return;
            }
            if (!worldReady) {
                if (mc.level == null || mc.player == null || mc.gameMode == null || mc.screen != null) return;
                require(mc.getSingleplayerServer() != null && mc.player.connection.getConnection().isMemoryConnection(), "Integrated test world required");
                playerId = mc.player.getUUID(); worldReady = true; buildPlan();
            }
            if (mc.getOverlay() != null) return;
            require(mc.level != null && mc.player != null && mc.getSingleplayerServer() != null, "Test world disconnected");
            if (stepIndex == steps.size()) { finish(mc, null); return; }
            Step step = steps.get(stepIndex);
            require(++stepTicks < 1200, "Step timed out: " + step.name());
            if (step.work().run(mc)) {
                JsonObject row = new JsonObject(); row.addProperty("test", step.name()); row.addProperty("verdict", "PASS");
                row.addProperty("client_ticks", stepTicks); row.addProperty("client_world_tick", mc.level.getGameTime()); observations.add(row);
                EngineersDecorReforged.LOGGER.info("IEDCTR_FACTORY_STUDY step={} PASS", step.name());
                stepIndex++; stepTicks = 0;
            }
        } catch (Throwable error) { finish(mc, error); }
    }

    private static void boot(Minecraft mc) throws Exception {
        nonce = System.getProperty("iedctr.clientGameplayNonce", "");
        languageProfile = System.getProperty("iedctr.factoryStudyLanguage", "all");
        require(languageProfile.equals("all") || languageProfile.equals("en_us") || languageProfile.equals("fr_fr"), "Unsupported language profile");
        if (!languageProfile.equals("all")) require(mc.getLanguageManager().getSelected().equals(languageProfile), "Cold-start language differs from requested profile");
        require(nonce.matches("[a-f0-9]{32}"), "Missing nonce");
        directory = mc.gameDirectory.toPath().toRealPath();
        require(directory.equals(Path.of(System.getProperty("iedctr.clientGameplayDirectory", "")).toRealPath())
            && Files.readString(directory.resolve(".iedctr-client-gameplay-nonce")).equals(nonce), "Fresh-directory marker mismatch");
        require(mc.level == null && mc.getSingleplayerServer() == null && !Files.exists(directory.resolve("factory-study-proof.json")), "Existing run forbidden");
        if (Files.exists(directory.resolve("saves"))) try (var saves = Files.list(directory.resolve("saves"))) { require(saves.findAny().isEmpty(), "Existing world forbidden"); }
        require(System.getProperty(MachineMenu.DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY) == null, "Unrelated presentation override forbidden");
        require(Runtime.getRuntime().maxMemory() >= 3L*1024*1024*1024, "Native client requires an explicit >=3GiB heap");
        packaged = FactoryStudyRuntimeProof.verify(mc);
        mc.options.pauseOnLostFocus = false;
        String name = "factory-study-" + nonce;
        booted = true;
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
            new WorldOptions(0L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }

    private static void buildPlan() {
        int n = 0;
        for (MachineKind kind : new MachineKind[]{MachineKind.FACTORY_HOPPER, MachineKind.FACTORY_DROPPER, MachineKind.SMALL_LAB_FURNACE}) {
            BlockPos pos = new BlockPos(n++ * 8, 80, 0); String prefix = kind.registryName() + "/";
            action(prefix + "select", mc -> { currentKind = kind; currentPos = pos; });
            server(prefix + "fresh_fixture", p -> setup(p, kind, pos));
            waitFor(prefix + "world_block_synchronized", mc -> mc.screen == null && mc.level.getBlockState(pos).is(block(kind))
                && mc.player.getEyePosition().distanceTo(new Vec3(pos.getX()+.5,80.5,1)) < 4);
            action(prefix + "normal_registered_block_use", mc -> {
                require(mc.player.getMainHandItem().isEmpty(), "Opening requires empty hand");
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(pos.getX()+.5,80.5,1), Direction.SOUTH, pos, false));
            });
            waitFor(prefix + "registered_screen_open", mc -> mc.screen instanceof FactoryStudyScreen && menu(mc).kind() == kind);
            action(prefix + "topology", mc -> { checkGeometry(mc, kind, "empty"); opened++; });
            waitServer(prefix + "server_menu_and_empty_items", p -> p.containerMenu instanceof MachineMenu m && m.kind() == kind && m.slots.stream().allMatch(s -> s.getItem().isEmpty()));
            scale(prefix, 2);
            action(prefix + "clean_pointer", FactoryStudyPackagedValidation::clearPointer);
            delay(prefix + "empty_render_settle", 6);
            actionThrow(prefix + "empty_capture", mc -> capture(mc, kind.registryName()+"-empty", null));
            if (kind != MachineKind.SMALL_LAB_FURNACE) {
                for (Control control : FactoryStudyGeometry.controls(kind)) addControlTest(kind, control);
                addKeyboardTest(kind);
                addServerGuards(kind);
            }
            server(prefix + "seed_display_fixture", p -> seedFilled(p, kind));
            waitFor(prefix + "all_visible_slots_filled", mc -> menu(mc).slots.stream().allMatch(s -> !s.getItem().isEmpty()));
            waitServer(prefix + "filled_slots_server_confirmed", p -> p.containerMenu instanceof MachineMenu m && m.slots.stream().allMatch(s -> !s.getItem().isEmpty()));
            action(prefix + "filled_geometry", mc -> { checkGeometry(mc, kind, "filled"); filledSlotCount += menu(mc).slots.size(); clearPointer(mc); });
            delay(prefix + "filled_render_settle", 6);
            actionThrow(prefix + "filled_capture", mc -> capture(mc, kind.registryName()+"-filled", null));
            addInventoryTests(kind);
            for (String language : languageProfile.equals("all") ? new String[]{"en_us", "fr_fr"} : new String[]{languageProfile}) {
                language(prefix, language);
                for (int scale : new int[]{1, 2, 4}) {
                    scale(prefix + language + "/", scale);
                    action(prefix + language + "/scale" + scale + "/geometry", mc -> checkGeometry(mc, kind, language+"-scale"+scale));
                    addTooltip(kind, language, scale, kind == MachineKind.SMALL_LAB_FURNACE ? "FE" : FactoryStudyGeometry.controls(kind).getFirst().name(),
                        scale == 2 && language.equals("en_us") || scale == 4 && language.equals("fr_fr"));
                }
            }
            if (kind == MachineKind.SMALL_LAB_FURNACE) {
                scale(prefix + "active/", 2);
                server(prefix + "real_fuel_and_energy", p -> {
                    machine.setItem(1, new ItemStack(Items.COAL, 1));
                    require(machine.energyStorage(null).receiveEnergy(16000, false) == 16000, "Energy capability did not receive16000FE");
                    p.containerMenu.broadcastChanges();
                });
                waitFor(prefix + "real_burn_progress_energy_sync", mc -> menu(mc).field(0)>0 && menu(mc).field(2)>0 && menu(mc).energyStored()==16000);
                waitServer(prefix + "real_burn_progress_server_confirmed", p -> p.containerMenu instanceof MachineMenu m && m.field(0)>0 && m.field(2)>0 && m.energyStored()==16000);
                action(prefix + "active_clean_pointer", FactoryStudyPackagedValidation::clearPointer); delay(prefix + "active_render_settle", 4);
                actionThrow(prefix + "active_capture", mc -> capture(mc, "small_lab_furnace-active", null));
                addJeiTests();
            }
            action(prefix + "close_normal_screen", mc -> screen(mc).onClose());
            waitFor(prefix + "closed", mc -> mc.screen == null && !(mc.player.containerMenu instanceof MachineMenu));
        }
    }

    private static void addControlTest(MachineKind kind, Control control) {
        String prefix = kind.registryName()+"/control_"+control.name()+"/";
        int[] expected = new int[3], before = new int[2];
        action(prefix+"click", mc -> {
            MachineMenu m=menu(mc); int logic=m.field(kind==MachineKind.FACTORY_HOPPER?2:5);
            expected[0]=field(kind,control); before[0]=expected[0]<0?0:m.field(expected[0]); before[1]=m.field(2);
            switch(control) {
                case RANGE -> expected[1]=3;
                case VELOCITY, PERIOD -> expected[1]=75;
                case COUNT -> expected[1]=24;
                case DIRECTION -> { expected[1]=50; expected[2]=50; }
                case REDSTONE -> expected[1]=kind==MachineKind.FACTORY_HOPPER
                    ? ((logic&2) | switch(logic&5){case 0->1;case 1->4;default->0;})
                    : ((logic&~34) | switch(logic&34){case 0->32;case 32->2;default->0;}) & 51;
                case TRIGGER -> expected[1]=(logic ^ (kind==MachineKind.FACTORY_HOPPER?2:16)) & (kind==MachineKind.FACTORY_HOPPER?7:51);
                case FILTER_GATE -> expected[1]=(logic^1)&51;
                case MANUAL -> expected[1]=1;
            }
            require(control==Control.MANUAL || before[0]!=expected[1] || control==Control.DIRECTION && before[1]!=expected[2], "Control fixture must change its value");
            clickControl(mc, control, .75, .25);
        });
        waitFor(prefix+"client_ack", mc -> menu(mc).field(expected[0])==expected[1] && (control!=Control.DIRECTION || menu(mc).field(2)==expected[2]));
        waitServer(prefix+"server_ack", p -> p.containerMenu instanceof MachineMenu m && m.field(expected[0])==expected[1] && (control!=Control.DIRECTION || m.field(2)==expected[2]));
        action(prefix+"record", mc -> {
            JsonObject row=new JsonObject();row.addProperty("machine",kind.registryName());row.addProperty("control",control.name());row.addProperty("field",expected[0]);row.addProperty("before",before[0]);row.addProperty("after",menu(mc).field(expected[0]));
            if(control==Control.DIRECTION)row.addProperty("after_y",menu(mc).field(2));row.addProperty("client_and_server_confirmed",true);row.addProperty("input","screen.mouseClicked/mouseReleased -> normal action packet");controls.add(row);
        });
        if(control==Control.MANUAL) {
            waitFor(prefix+"client_signal_returns_low", mc -> menu(mc).field(expected[0])==0);
            waitServer(prefix+"server_signal_returns_low", p -> p.containerMenu instanceof MachineMenu m && m.field(expected[0])==0);
        }
        delay(prefix+"settle",3);
    }

    private static void addKeyboardTest(MachineKind kind) {
        String pre=kind.registryName()+"/keyboard/";int field=0,max=kind==MachineKind.FACTORY_HOPPER?4:100;
        action(pre+"tab_then_end",mc->{var s=screen(mc);s.setFocused(null);mc.setLastInputType(net.minecraft.client.InputType.KEYBOARD_TAB);boolean tabReturn=s.keyPressed(GLFW.GLFW_KEY_TAB,0,0);require(s.getFocused()!=null&&s.getFocused().isFocused(),"Tab did not focus a control");JsonObject tabObservation=new JsonObject();tabObservation.addProperty("test",pre+"tab_observation");tabObservation.addProperty("key_pressed_return",tabReturn);tabObservation.addProperty("focus_non_null",s.getFocused()!=null);tabObservation.addProperty("focused_element_is_focused",s.getFocused().isFocused());tabObservation.addProperty("verdict","PASS");observations.add(tabObservation);require(s.keyPressed(GLFW.GLFW_KEY_END,0,0),"End not consumed");});
        waitFor(pre+"client_end_ack",mc->menu(mc).field(field)==max);
        waitServer(pre+"server_end_ack",p->p.containerMenu instanceof MachineMenu m&&m.field(field)==max);
        action(pre+"wheel_decrement",mc->{var r=FactoryStudyGeometry.track(kind,FactoryStudyGeometry.controls(kind).getFirst());var v=screen(mc).validationViewTransform();require(screen(mc).mouseScrolled(v.screenX(r.x()+r.width()/2.),v.screenY(r.y()+r.height()/2.),0,-1),"Wheel not consumed");});
        waitFor(pre+"client_wheel_ack",mc->menu(mc).field(field)==max-1);
        waitServer(pre+"server_wheel_ack",p->p.containerMenu instanceof MachineMenu m&&m.field(field)==max-1);
    }

    private static void addServerGuards(MachineKind kind) {
        String pre=kind.registryName()+"/guards/";int[] before={0};int action=kind==MachineKind.FACTORY_HOPPER?1:10;
        action(pre+"send_stale_container_packet",mc->{before[0]=menu(mc).field(0);PacketDistributor.sendToServer(new MachineActionPayload(menu(mc).containerId+1000,action,0,0));});
        delay(pre+"stale_packet_settle",12);
        waitFor(pre+"stale_packet_client_unchanged",mc->menu(mc).field(0)==before[0]);
        server(pre+"stale_spectator_wrong_action_server_guards",p->{
            require(p.containerMenu instanceof MachineMenu,"Missing server menu");MachineMenu m=(MachineMenu)p.containerMenu;require(m.field(0)==before[0],"Stale ID changed state");
            p.setGameMode(GameType.SPECTATOR);m.handleAction(p,action,0,0);require(m.field(0)==before[0],"Spectator action accepted");p.setGameMode(GameType.SURVIVAL);
            p.containerMenu=p.inventoryMenu;try{m.handleAction(p,action,0,0);require(m.field(0)==before[0],"Stale menu object accepted");}finally{p.containerMenu=m;}
            m.handleAction(p,kind==MachineKind.FACTORY_HOPPER?10:1,0,0);require(m.field(0)==before[0],"Wrong-machine action accepted");
        });
    }

    private static void addInventoryTests(MachineKind kind) {
        String pre=kind.registryName()+"/inventory/";int index=FactoryStudyGeometry.machineSlotCount(kind);int[] heldBefore={0};
        action(pre+"pickup_via_screen",mc->clickSlot(mc,index));
        waitFor(pre+"pickup_client",mc->menu(mc).getSlot(index).getItem().isEmpty()&&ore(menu(mc).getCarried()));
        waitServer(pre+"pickup_server",p->p.containerMenu instanceof MachineMenu m&&m.getSlot(index).getItem().isEmpty()&&ore(m.getCarried()));
        if(kind!=MachineKind.SMALL_LAB_FURNACE){
            action(pre+"held_stack_control_attempt",mc->{heldBefore[0]=menu(mc).field(0);clickControl(mc,FactoryStudyGeometry.controls(kind).getFirst(),1,0);});
            delay(pre+"held_stack_settle",10);
            waitFor(pre+"held_stack_client_blocked",mc->ore(menu(mc).getCarried())&&menu(mc).field(0)==heldBefore[0]);
            waitServer(pre+"held_stack_server_unchanged",p->p.containerMenu instanceof MachineMenu m&&ore(m.getCarried())&&m.field(0)==heldBefore[0]);
        }
        delay(pre+"avoid_double_click",8);
        action(pre+"return_via_screen",mc->clickSlot(mc,index));
        waitFor(pre+"return_client",mc->menu(mc).getCarried().isEmpty()&&ore(menu(mc).getSlot(index).getItem()));
        waitServer(pre+"return_server",p->p.containerMenu instanceof MachineMenu m&&m.getCarried().isEmpty()&&ore(m.getSlot(index).getItem()));
        server(pre+"quickmove_fixture",p->{machine.clearContent();p.getInventory().clearContent();p.getInventory().setItem(9,new ItemStack(Items.IRON_ORE,7));p.containerMenu.broadcastChanges();});
        waitFor(pre+"quickmove_fixture_synced",mc->ore(menu(mc).getSlot(index).getItem())&&menu(mc).getSlot(0).getItem().isEmpty());
        action(pre+"normal_quickmove_packet",mc->mc.gameMode.handleInventoryMouseClick(menu(mc).containerId,index,0,ClickType.QUICK_MOVE,mc.player));
        waitFor(pre+"quickmove_client",mc->menu(mc).getSlot(index).getItem().isEmpty()&&ore(menu(mc).getSlot(0).getItem()));
        waitServer(pre+"quickmove_server_conservation",p->p.containerMenu instanceof MachineMenu m&&m.getSlot(index).getItem().isEmpty()&&ore(machine.getItem(0))&&totalItems(p)==7);
    }

    private static void addTooltip(MachineKind kind,String lang,int scale,String target,boolean save) {
        String pre=kind.registryName()+"/tooltip/"+lang+"/scale"+scale+"/";
        action(pre+"position",mc->{
            var s=screen(mc);s.setFocused(null);var r=kind==MachineKind.SMALL_LAB_FURNACE?FactoryStudyGeometry.energyBounds():FactoryStudyGeometry.controlBounds(kind,Control.valueOf(target));
            var v=s.validationViewTransform();pointerX=v.screenX(r.x()+r.width()/2.);pointerY=v.screenY(r.y()+r.height()/2.);
            tooltipObserved=null;tooltipCandidate=null;tooltipFrames=0;tooltipRecording=true;movePointer(mc,pointerX,pointerY);
        });
        waitFor(pre+"pointer_observed",mc->{if(pointerObserved(mc))return true;movePointer(mc,pointerX,pointerY);return false;});
        waitFor(pre+"actual_rendered_tooltip",mc->tooltipFrames>=2&&tooltipObserved!=null);
        actionThrow(pre+"bounded_localized_assertion",mc->{
            require(mc.getLanguageManager().getSelected().equals(lang),"Selected language differs");
            require(tooltipObserved.get("in_bounds").getAsBoolean(),"Tooltip clipped: "+tooltipObserved);
            require(tooltipObserved.get("width").getAsInt()<=FactoryStudyGeometry.tooltipWidth(screen(mc).width),"Tooltip exceeds wrapping bound");
            String text=tooltipObserved.get("text").getAsString();require(text.length()>5&&!text.contains("gui.immersive_engineer"),"Untranslated tooltip");
            String expectedPhrase = lang.equals("fr_fr") ? (kind==MachineKind.FACTORY_HOPPER?"Réglage de la portée de collecte":kind==MachineKind.FACTORY_DROPPER?"Réglage de la vitesse de largage":"Énergie stockée") : (kind==MachineKind.FACTORY_HOPPER?"Collection range setting":kind==MachineKind.FACTORY_DROPPER?"Ejection speed setting":"Stored energy");
            require(text.replaceAll("\\s+", " ").contains(expectedPhrase), "Rendered tooltip is not in the required language: "+text);
            JsonObject row=tooltipObserved.deepCopy();row.addProperty("machine",kind.registryName());row.addProperty("language",lang);row.addProperty("gui_scale",scale);row.addProperty("target",target);tooltips.add(row);
            if(save)capture(mc,kind.registryName()+"-tooltip-"+lang+"-scale"+scale,row);
            tooltipRecording=false;
        });
    }


    private static void addJeiTests() {
        String pre="small_lab_furnace/jei/";
        action(pre+"required_runtime",mc->require(ModList.get().isLoaded("jei"),"This final native profile requires JEI"));
        action(pre+"shortcut_pointer",mc->{
            tooltipRecording=false;screen(mc).setFocused(null);var r=FactoryStudyGeometry.progress();var v=screen(mc).validationViewTransform();
            pointerX=v.screenX(r.x()+r.width()/2.);pointerY=v.screenY(r.y()+r.height()/2.);movePointer(mc,pointerX,pointerY);
        });
        waitFor(pre+"shortcut_pointer_observed",FactoryStudyPackagedValidation::pointerObserved);
        action(pre+"shortcut_normal_glfw_click",mc->nativeButton(1));
        waitFor(pre+"shortcut_recipes_open",FactoryStudyPackagedValidation::jeiOpen);
        delay(pre+"shortcut_render_settle",6);
        actionThrow(pre+"shortcut_selected_smelting",mc->{
            JsonObject detail=readJei(mc,"progress_shortcut",null,null);require(detail.get("selected_recipe_type").getAsString().equals("minecraft:smelting"),"Shortcut selected wrong recipe type");
            jeiChecks.add(detail);capture(mc,"small_lab_furnace-jei-smelting",detail);mc.screen.onClose();
        });
        waitFor(pre+"shortcut_returns_to_factory",mc->mc.screen instanceof FactoryStudyScreen);
        server(pre+"ingredient_shortcut_fixture",p->{p.getInventory().setItem(9,new ItemStack(Items.IRON_INGOT));p.getInventory().setItem(10,new ItemStack(Items.IRON_ORE));p.containerMenu.broadcastChanges();});
        waitFor(pre+"ingredient_fixture_synchronized",mc->menu(mc).getSlot(11).getItem().is(Items.IRON_INGOT)&&menu(mc).getSlot(12).getItem().is(Items.IRON_ORE));
        for(boolean recipes:new boolean[]{true,false}) {
            String name=recipes?"recipes_R":"uses_U";int slot=recipes?11:12;
            action(pre+name+"_pointer",mc->{var a=FactoryStudyGeometry.anchor(MachineKind.SMALL_LAB_FURNACE,slot);var v=screen(mc).validationViewTransform();pointerX=v.screenX(a.x()+8);pointerY=v.screenY(a.y()+8);movePointer(mc,pointerX,pointerY);});
            waitFor(pre+name+"_pointer_observed",FactoryStudyPackagedValidation::pointerObserved);
            action(pre+name+"_normal_glfw_key",mc->nativeKey(recipes?'r':'u'));
            waitFor(pre+name+"_open",FactoryStudyPackagedValidation::jeiOpen);
            delay(pre+name+"_render_settle",6);
            actionThrow(pre+name+"_focus_readback",mc->{JsonObject detail=readJei(mc,name,recipes?RecipeIngredientRole.OUTPUT:RecipeIngredientRole.INPUT,recipes?Items.IRON_INGOT:Items.IRON_ORE);jeiChecks.add(detail);capture(mc,"small_lab_furnace-jei-"+name,detail);mc.screen.onClose();});
            waitFor(pre+name+"_returns_to_factory",mc->mc.screen instanceof FactoryStudyScreen);
        }
        waitServer(pre+"same_server_menu_still_open",p->p.containerMenu instanceof MachineMenu m&&m.kind()==MachineKind.SMALL_LAB_FURNACE);
    }
    private static boolean jeiOpen(Minecraft mc){return mc.screen!=null&&mc.screen.getClass().getName().equals("mezz.jei.gui.recipes.RecipesGui");}
    private static boolean pointerObserved(Minecraft mc){var s=screen(mc);return Math.abs(mc.mouseHandler.xpos()*s.width/mc.getWindow().getScreenWidth()-pointerX)<1.5&&Math.abs(mc.mouseHandler.ypos()*s.height/mc.getWindow().getScreenHeight()-pointerY)<1.5;}
    private static JsonObject readJei(Minecraft mc,String name,RecipeIngredientRole expectedRole,net.minecraft.world.item.Item expectedItem)throws Exception{
        require(jeiOpen(mc),"JEI recipes screen missing");
        var logicField=mc.screen.getClass().getDeclaredField("logic");logicField.setAccessible(true);Object logic=logicField.get(mc.screen);
        Object category=Class.forName("mezz.jei.gui.recipes.IRecipeGuiLogic").getMethod("getSelectedRecipeCategory").invoke(logic);
        JsonObject row=new JsonObject();row.addProperty("test",name);row.addProperty("screen_class",mc.screen.getClass().getName());row.addProperty("selected_recipe_type",((IRecipeCategory<?>)category).getRecipeType().getUid().toString());
        if(expectedRole!=null){
            var stateField=logic.getClass().getDeclaredField("state");stateField.setAccessible(true);Object state=stateField.get(logic);
            var focuses=(IFocusGroup)Class.forName("mezz.jei.gui.recipes.lookups.ILookupState").getMethod("getFocuses").invoke(state);
            require(focuses.getItemStackFocuses(expectedRole).anyMatch(f->f.getTypedValue().getIngredient().is(expectedItem)),"JEI R/U focus does not match the hovered item and role");
            row.addProperty("focus_role",expectedRole.name());row.addProperty("focus_item",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(expectedItem).toString());row.addProperty("focus_matches_hovered_item",true);
        }
        row.addProperty("input","XTEST synthetic events through GLFW and normal JEI event interception");return row;
    }
    private static void openX(){if(xDisplay==null){x11=NativeLibrary.getInstance("X11");xtest=NativeLibrary.getInstance("Xtst");xDisplay=x11.getFunction("XOpenDisplay").invokePointer(new Object[]{null});require(xDisplay!=null,"Test display unavailable");}}
    private static void nativeButton(int button){openX();require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{xDisplay,button,1,new NativeLong(0)})!=0,"Synthetic mouse press failed");require(xtest.getFunction("XTestFakeButtonEvent").invokeInt(new Object[]{xDisplay,button,0,new NativeLong(0)})!=0,"Synthetic mouse release failed");x11.getFunction("XFlush").invokeInt(new Object[]{xDisplay});}
    private static void nativeKey(char symbol){openX();int key=x11.getFunction("XKeysymToKeycode").invokeInt(new Object[]{xDisplay,new NativeLong(symbol)})&255;require(key>0,"No test keycode");require(xtest.getFunction("XTestFakeKeyEvent").invokeInt(new Object[]{xDisplay,key,1,new NativeLong(0)})!=0,"Synthetic key press failed");require(xtest.getFunction("XTestFakeKeyEvent").invokeInt(new Object[]{xDisplay,key,0,new NativeLong(0)})!=0,"Synthetic key release failed");x11.getFunction("XFlush").invokeInt(new Object[]{xDisplay});}

    private static void language(String pre,String language) {
        steps.add(new Step(pre+"language_"+language,new Work(){CompletableFuture<?> pending;public boolean run(Minecraft mc){
            if(mc.getLanguageManager().getSelected().equals(language)&&pending==null)return true;
            if(pending==null){require(languageProfile.equals("all"),"Cold-start profile must not reload resources");tooltipRecording=false;mc.getLanguageManager().setSelected(language);mc.options.languageCode=language;pending=mc.reloadResourcePacks();return false;}
            if(!pending.isDone())return false;pending.join();return true;
        }}));
    }
    private static void scale(String pre,int value){
        action(pre+"set_scale_"+value,mc->{tooltipRecording=false;mc.options.guiScale().set(value);mc.resizeDisplay();});
        delay(pre+"scale_"+value+"_settle",6);
        action(pre+"scale_"+value+"_actual",mc->{require(mc.getWindow().getGuiScale()==value,"Requested GUI scale not applied");require(mc.getWindow().getWidth()==1920&&mc.getWindow().getHeight()==1080,"Expected1920×1080 framebuffer");});
    }

    private static void setup(ServerPlayer p,MachineKind kind,BlockPos pos){
        p.closeContainer();p.setGameMode(GameType.SURVIVAL);var level=p.serverLevel();
        for(int x=pos.getX()-2;x<=pos.getX()+2;x++)for(int z=-2;z<=5;z++)level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,block(kind).defaultBlockState().setValue(kind==MachineKind.SMALL_LAB_FURNACE?MachineBlocks.HORIZONTAL_FACING:MachineBlocks.FACING,Direction.NORTH),3);
        require(level.getBlockEntity(pos)instanceof MachineBlockEntity,"Machine missing");machine=(MachineBlockEntity)level.getBlockEntity(pos);
        require(machine.isEmpty(),"Fixture must start empty");p.getInventory().clearContent();p.connection.teleport(pos.getX()+.5,80,3.5,180,24);
    }
    private static void seedFilled(ServerPlayer p,MachineKind kind){
        MachineMenu m=(MachineMenu)p.containerMenu;
        if(kind!=MachineKind.SMALL_LAB_FURNACE){machine.dataAccessForTests().set(kind==MachineKind.FACTORY_HOPPER?2:5,kind==MachineKind.FACTORY_HOPPER?0:2);machine.dataAccessForTests().set(kind==MachineKind.FACTORY_HOPPER?5:11,0);}
        // Display coverage fixture deliberately seeds PAPER even in processing/output slots.
        // This tests rendering only; the separate ore/fuel tests use valid gameplay inputs.
        for(int i=0;i<m.slots.size();i++)m.getSlot(i).set(new ItemStack(Items.PAPER,1+i%32));
        p.getInventory().setItem(9,new ItemStack(Items.IRON_ORE,7));m.broadcastChanges();
    }
    private static Block block(MachineKind k){return switch(k){case FACTORY_HOPPER->ModBlocks.FACTORY_HOPPER.get();case FACTORY_DROPPER->ModBlocks.FACTORY_DROPPER.get();case SMALL_LAB_FURNACE->ModBlocks.SMALL_LAB_FURNACE.get();default->throw new IllegalArgumentException();};}
    private static int field(MachineKind k,Control c){return switch(c){case RANGE,VELOCITY->0;case COUNT->k==MachineKind.FACTORY_HOPPER?1:4;case PERIOD->k==MachineKind.FACTORY_HOPPER?3:6;case DIRECTION->1;case MANUAL->k==MachineKind.FACTORY_HOPPER?5:11;default->k==MachineKind.FACTORY_HOPPER?2:5;};}
    private static MachineMenu menu(Minecraft mc){require(mc.player.containerMenu instanceof MachineMenu,"No client machine menu");return(MachineMenu)mc.player.containerMenu;}
    private static FactoryStudyScreen screen(Minecraft mc){require(mc.screen instanceof FactoryStudyScreen,"No default FactoryStudyScreen");return(FactoryStudyScreen)mc.screen;}
    private static ServerPlayer player(Minecraft mc){ServerPlayer p=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);require(p!=null,"Server player missing");return p;}
    private static boolean ore(ItemStack s){return s.is(Items.IRON_ORE)&&s.getCount()==7;}
    private static int totalItems(ServerPlayer p){int sum=p.containerMenu.getCarried().getCount();for(int i=0;i<machine.getContainerSize();i++)sum+=machine.getItem(i).getCount();for(int i=0;i<36;i++)sum+=p.getInventory().getItem(i).getCount();return sum;}

    private static void checkGeometry(Minecraft mc,MachineKind kind,String state){
        var s=screen(mc);var m=menu(mc);var v=s.validationViewTransform();int count=FactoryStudyGeometry.machineSlotCount(kind);
        require(m.factoryStudyPresentation()&&m.slots.size()==count+36&&m.visibleMachineSlots()==count,"Menu topology differs");
        require(s.validationControlCount()==(kind==MachineKind.FACTORY_HOPPER?6:kind==MachineKind.FACTORY_DROPPER?8:0),"Control count differs");
        int panelWidth = FactoryStudyGeometry.canvasWidth(kind);
        int panelHeight = FactoryStudyGeometry.canvasHeight(kind);
        require(v.originX() >= 5.9 && v.originY() >= 5.9
            && v.screenX(panelWidth) <= s.width - 5.9 && v.screenY(panelHeight) <= s.height - 5.9,
            "Canvas exceeds viewport margin: " + kind + " " + panelWidth + "x" + panelHeight);
        for(var a:FactoryStudyGeometry.anchors(kind)){
            var slot=m.getSlot(a.menuIndex());require(slot.x==a.x()&&slot.y==a.y(),"Anchor differs");
            int backing=a.menuIndex()<count?a.menuIndex():a.menuIndex()<count+27?a.menuIndex()-count+9:a.menuIndex()-count-27;
            require(slot.getSlotIndex()==backing,"Slot backing ID differs");
            double x=v.screenX(a.x()+8),y=v.screenY(a.y()+8);require(s.validationSlotHoveredAt(a.menuIndex(),x,y),"Center misses slot");
            require(!s.validationSlotHoveredAt(a.menuIndex(),v.screenX(a.hoverLeft()-.01),y)&&!s.validationSlotHoveredAt(a.menuIndex(),v.screenX(a.hoverRight()+.01),y)
                &&!s.validationSlotHoveredAt(a.menuIndex(),x,v.screenY(a.hoverTop()-.01))&&!s.validationSlotHoveredAt(a.menuIndex(),x,v.screenY(a.hoverBottom()+.01)),"Hover escapes source interior");
            for(var b:FactoryStudyGeometry.anchors(kind))if(b.menuIndex()!=a.menuIndex())require(!s.validationSlotHoveredAt(b.menuIndex(),x,y),"Center overlaps another slot");
        }
        JsonObject row=new JsonObject();row.addProperty("machine",kind.registryName());row.addProperty("state",state);row.addProperty("slots",m.slots.size());row.addProperty("gui_scale",mc.getWindow().getGuiScale());row.addProperty("canvas_fit_scale",v.scale());row.addProperty("logical_width",s.width);row.addProperty("logical_height",s.height);row.addProperty("all_centers_and_boundaries_pass",true);geometry.add(row);
    }
    private static void clickSlot(Minecraft mc,int index){var s=screen(mc);var a=FactoryStudyGeometry.anchor(currentKind,index);var v=s.validationViewTransform();double x=v.screenX(a.x()+8),y=v.screenY(a.y()+8);s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);}
    private static void clickControl(Minecraft mc,Control c,double fx,double fy){var s=screen(mc);var r=FactoryStudyGeometry.track(currentKind,c);var v=s.validationViewTransform();double x=v.screenX(r.x()+(r.width()-1)*fx),y=v.screenY(r.y()+(r.height()-1)*fy);s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);}
    private static void clearPointer(Minecraft mc){tooltipRecording=false;screen(mc).setFocused(null);movePointer(mc,2,2);}
    private static void movePointer(Minecraft mc,double x,double y){
        long window=mc.getWindow().getWindow();if(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)!=GLFW.GLFW_TRUE){GLFW.glfwFocusWindow(window);return;}
        int[] wx={0},wy={0};GLFW.glfwGetWindowPos(window,wx,wy);int px=wx[0]+(int)Math.round(x*mc.getWindow().getScreenWidth()/screen(mc).width),py=wy[0]+(int)Math.round(y*mc.getWindow().getScreenHeight()/screen(mc).height);
        openX();require(xtest.getFunction("XTestFakeMotionEvent").invokeInt(new Object[]{xDisplay,-1,px,py,new NativeLong(0)})!=0,"XTEST pointer move failed");x11.getFunction("XFlush").invokeInt(new Object[]{xDisplay});
    }

    @SubscribeEvent public static void before(ScreenEvent.Render.Pre event){tooltipInFrame=false;}
    @SubscribeEvent public static void tooltip(RenderTooltipEvent.Color event){
        Minecraft mc=Minecraft.getInstance();if(!Boolean.getBoolean("iedctr.factoryStudyNative")||complete||!tooltipRecording||!(mc.screen instanceof FactoryStudyScreen s))return;
        double px=mc.mouseHandler.xpos()*s.width/mc.getWindow().getScreenWidth(),py=mc.mouseHandler.ypos()*s.height/mc.getWindow().getScreenHeight();if(Math.abs(px-pointerX)>1.5||Math.abs(py-pointerY)>1.5)return;
        try{
            StringBuilder text=new StringBuilder();int width=0,height=event.getComponents().size()==1?-2:0;
            for(var c:event.getComponents()){
                FormattedCharSequence seq=null;for(var f:c.getClass().getDeclaredFields())if(FormattedCharSequence.class.isAssignableFrom(f.getType())){f.setAccessible(true);seq=(FormattedCharSequence)f.get(c);break;}
                require(seq!=null,"Tooltip text is not observable");if(!text.isEmpty())text.append('\n');seq.accept((i,style,cp)->{text.appendCodePoint(cp);return true;});width=Math.max(width,c.getWidth(event.getFont()));height+=c.getHeight();
            }
            JsonObject row=new JsonObject();row.addProperty("text",text.toString());row.addProperty("x",event.getX());row.addProperty("y",event.getY());row.addProperty("width",width);row.addProperty("height",height);
            row.addProperty("in_bounds",event.getX()-4>=0&&event.getY()-4>=0&&event.getX()+width+4<=s.width&&event.getY()+height+4<=s.height);tooltipCandidate=row;tooltipInFrame=true;
        }catch(Throwable e){finish(mc,e);}
    }
    @SubscribeEvent public static void after(ScreenEvent.Render.Post event){if(tooltipInFrame&&tooltipRecording&&event.getScreen()==Minecraft.getInstance().screen){tooltipObserved=tooltipCandidate.deepCopy();tooltipFrames++;}tooltipInFrame=false;}
    private static void capture(Minecraft mc,String name,JsonObject detail)throws Exception{
        Path file=directory.resolve(name+".png");require(!Files.exists(file),"Capture already exists");try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(file);}
        JsonObject row=new JsonObject();row.addProperty("file",file.getFileName().toString());row.addProperty("sha256",FactoryControlRuntimeProof.hash(Files.readAllBytes(file)));row.addProperty("machine",currentKind.registryName());row.addProperty("gui_scale",mc.getWindow().getGuiScale());row.addProperty("width",mc.getWindow().getWidth());row.addProperty("height",mc.getWindow().getHeight());if(detail!=null)row.add("tooltip",detail.deepCopy());captures.add(row);
    }
    private static void action(String name,Consumer<Minecraft> fn){steps.add(new Step(name,mc->{fn.accept(mc);return true;}));}
    private static void actionThrow(String name,ThrowingConsumer fn){steps.add(new Step(name,mc->{fn.accept(mc);return true;}));}
    private interface ThrowingConsumer{void accept(Minecraft mc)throws Exception;}
    private static void waitFor(String name,Predicate<Minecraft> p){steps.add(new Step(name,p::test));}
    private static void delay(String name,int ticks){steps.add(new Step(name,mc->stepTicks>=ticks));}
    private static void server(String name,Consumer<ServerPlayer> fn){steps.add(new Step(name,new Work(){CompletableFuture<?> future;public boolean run(Minecraft mc){if(future==null){future=mc.getSingleplayerServer().submit(()->fn.accept(player(mc)));return false;}if(!future.isDone())return false;future.join();return true;}}));}
    private static void waitServer(String name,Predicate<ServerPlayer> fn){steps.add(new Step(name,new Work(){CompletableFuture<Boolean> future;public boolean run(Minecraft mc){if(future==null){future=mc.getSingleplayerServer().submit(()->fn.test(player(mc)));return false;}if(!future.isDone())return false;boolean result=future.join();future=null;return result;}}));}
    private static void finish(Minecraft mc,Throwable error){
        complete=true;
        if(xDisplay!=null){x11.getFunction("XCloseDisplay").invokeInt(new Object[]{xDisplay});xDisplay=null;}
        try{if(directory!=null){
            if(error!=null&&worldReady&&mc.screen!=null)try{capture(mc,"failure-observation",null);}catch(Exception ignored){}
            JsonObject proof=new JsonObject();proof.addProperty("suite","factory-study-packaged-integration-v1");proof.addProperty("status",error==null?"PASS":"FAIL");proof.addProperty("nonce",nonce);
            proof.addProperty("candidate_source_sha",System.getProperty("iedctr.executedSourceSha"));proof.addProperty("working_tree_manifest_sha256",System.getProperty("iedctr.workingTreeManifestSha256"));
            proof.add("packaged_runtime",packaged);proof.addProperty("actual_integrated_world",worldReady);proof.addProperty("opened_registered_screens",opened);proof.addProperty("filled_visible_slots",filledSlotCount);
            proof.addProperty("input_method","normal Minecraft block-use, screen handlers, inventory/action packets; XTEST events through GLFW; independent server observations; not hardware input");
            proof.addProperty("language_profile",languageProfile);proof.addProperty("full_resource_reload",languageProfile.equals("all")?"ATTEMPTED_LANGUAGE_SWITCHES":"NOT_PERFORMED_COLD_START_PROFILE");
            proof.addProperty("display_fixture","PAPER seeded directly into all visible slots only for visual coverage; separate valid7IRON_ORE inventory and realCOAL/FE tests");
            proof.addProperty("native_visual_acceptance",false);proof.addProperty("multiplayer_dedicated_server_test","NOT_PERFORMED");proof.addProperty("screen_reader_listening_test","NOT_PERFORMED");
            proof.addProperty("jvm_max_memory_bytes",Runtime.getRuntime().maxMemory());proof.add("jei_checks",jeiChecks);
            proof.addProperty("completed_steps",stepIndex);proof.addProperty("total_planned_steps",steps.size());proof.addProperty("client_ticks",totalTicks);proof.add("observations",observations);proof.add("geometry",geometry);proof.add("controls",controls);proof.add("tooltips",tooltips);proof.add("captures",captures);
            if(error!=null){proof.addProperty("error",error.toString());proof.addProperty("failed_step",stepIndex<steps.size()?steps.get(stepIndex).name():"boot");JsonObject debug=new JsonObject();debug.addProperty("screen",mc.screen==null?"none":mc.screen.getClass().getName());debug.addProperty("expected_pointer_x",pointerX);debug.addProperty("expected_pointer_y",pointerY);debug.addProperty("tooltip_frames",tooltipFrames);debug.addProperty("tooltip_color_seen",tooltipCandidate!=null);if(mc.screen!=null){debug.addProperty("observed_pointer_x",mc.mouseHandler.xpos()*mc.screen.width/mc.getWindow().getScreenWidth());debug.addProperty("observed_pointer_y",mc.mouseHandler.ypos()*mc.screen.height/mc.getWindow().getScreenHeight());}proof.add("failure_diagnostics",debug);}
            Files.writeString(directory.resolve("factory-study-proof.json"),new GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
        }}catch(Exception e){EngineersDecorReforged.LOGGER.error("Factory study proof write failed",e);}
        if(error==null)EngineersDecorReforged.LOGGER.info("IEDCTR_FACTORY_STUDY PASS nonce={}",nonce);else EngineersDecorReforged.LOGGER.error("IEDCTR_FACTORY_STUDY FAIL",error);mc.stop();
    }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
}
