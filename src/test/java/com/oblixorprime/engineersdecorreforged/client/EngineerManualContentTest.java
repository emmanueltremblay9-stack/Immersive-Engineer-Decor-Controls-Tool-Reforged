package com.oblixorprime.engineersdecorreforged.client;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

/** Guards duplicated manual text; does not replace native pagination/visual review. */
public class EngineerManualContentTest {
    private static final Path ROOT = Path.of("src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/manual");
    private static final String[] MACHINES = {"small_freezer", "small_tree_cutter", "small_solar_panel",
            "fluid_barrel", "small_block_breaker", "small_mineral_smelter", "small_milking_machine", "small_fluid_funnel"};

    @Test
    public void updatedEntriesAndCategoryCopiesAgree() throws Exception {
        for (String lang : new String[]{"en_us", "fr_fr"}) {
            for (String machine : MACHINES) {
                String individual = Files.readString(ROOT.resolve(lang).resolve(machine + ".txt"));
                assertEquals(machine, 1, individual.split("<&recipe>", -1).length - 1);
                String body = individual.substring(individual.indexOf("<&recipe>") + "<&recipe>".length()).trim();
                String category = (machine.equals("fluid_barrel") || machine.equals("small_fluid_funnel")) ? "storage_transport" : "workshop_machines";
                String combined = Files.readString(ROOT.resolve(lang).resolve(category + ".txt"));
                String anchor = "<&" + machine + ">";
                assertEquals(machine, 1, combined.split(java.util.regex.Pattern.quote(anchor), -1).length - 1);
                int start = combined.indexOf(anchor) + anchor.length();
                int end = combined.indexOf("<np>", start);
                assertEquals(lang + "/" + machine, body, combined.substring(start, end < 0 ? combined.length() : end).trim());
                assertFalse(machine, body.contains("shop-floor utility block for crafting, storage"));
                assertFalse(machine, body.contains("collect drops separately"));
            }
        }
    }

    @Test
    public void allExistingCategoryCopiesMatchTheirIndividualEntries() throws Exception {
        for (String lang : new String[]{"en_us", "fr_fr"}) {
            try (var files = Files.list(ROOT.resolve(lang))) {
                for (Path page : files.filter(p -> p.toString().endsWith(".txt")).toList()) {
                    String text = Files.readString(page);
                    var anchors = java.util.regex.Pattern.compile("<&([^>]+)>").matcher(text);
                    while (anchors.find()) {
                        String id = anchors.group(1);
                        Path individual = ROOT.resolve(lang).resolve(id + ".txt");
                        if (id.equals("recipe") || !Files.exists(individual)) continue;
                        String single = Files.readString(individual);
                        if (!single.contains("<&recipe>") || single.contains("<np>")) continue;
                        int end = text.indexOf("<np>", anchors.end());
                        assertEquals(page + ": " + id,
                            single.substring(single.indexOf("<&recipe>") + 9).trim(),
                            text.substring(anchors.end(), end < 0 ? text.length() : end).trim());
                    }
                }
            }
        }
    }

    @Test
    public void importantIntegratedContractsRemainVisible() throws Exception {
        String freezer = Files.readString(ROOT.resolve("en_us/small_freezer.txt"));
        for (String term : new String[]{"2,000 mB", "1,000 mB", "180 ticks", "8 FE", "empty bucket", "comparator"})
            assertTrue(term, freezer.contains(term));
        String tree = Files.readString(ROOT.resolve("en_us/small_tree_cutter.txt"));
        for (String term : new String[]{"single output slot", "does not consume FE", "unversioned", "discards", "Back up"})
            assertTrue(term, tree.contains(term));
        String solar = Files.readString(ROOT.resolve("en_us/small_solar_panel.txt"));
        assertTrue(solar.contains("every 20 ticks"));
        assertTrue(solar.contains("not 256 FE per tick"));
    }
    @Test
    public void funnelAndDoorSensorDoNotPromiseObsoleteBehavior() throws Exception {
        String funnel = Files.readString(ROOT.resolve("en_us/small_fluid_funnel.txt"));
        for (String term : new String[]{"16 fluid-to-fluid steps", "Four successful", "not stored fluid", "3,000 mB", "not a valid"})
            assertTrue(term, funnel.contains(term));
        String sensor = Files.readString(ROOT.resolve("en_us/door_sensor_switch.txt"));
        assertTrue(sensor.contains("detects nearby players"));
        assertTrue(sensor.contains("every 40 ticks"));
        assertFalse(sensor.contains("Resets after its scheduled pulse"));
    }

    @Test
    public void campfireRequirementAndCompactFrenchCategoryRemainDocumented() throws Exception {
        String bag = Files.readString(ROOT.resolve("en_us/sleeping_bag.txt"));
        for (String term : new String[]{"lit vanilla campfire", "eight neighboring", "diagonals count", "400-tick"})
            assertTrue(term, bag.contains(term));
        String language = Files.readString(ROOT.resolve("../lang/fr_fr.json"));
        assertFalse(language.contains("Équipements pour fluides et énergie"));
        for (String namespace : new String[]{"immersiveengineering", "immersive_engineer_decor_controls_tool_reforged"})
            assertTrue(language.contains("\"manual." + namespace + ".edr_fluid_power_utilities\": \"Fluides et énergie\""));
    }

    @Test
    public void historicalControlsDescribeTheirLiveBehavior() throws Exception {
        for (String lang : new String[]{"en_us", "fr_fr"}) {
            String timer = Files.readString(ROOT.resolve(lang + "/industrial_interval_timer.txt"));
            assertTrue(timer.contains("40"));
            String contact = Files.readString(ROOT.resolve(lang + "/industrial_foot_button.txt"));
            assertTrue(contact.contains("12"));
        }
        assertTrue(Files.readString(ROOT.resolve("en_us/charged_lapis_squeezer.txt")).contains("does not wear down"));
        assertTrue(Files.readString(ROOT.resolve("en_us/industrial_dimmer.txt")).contains("do not cycle"));
        assertTrue(Files.readString(ROOT.resolve("en_us/industrialswitch.txt")).contains("two-block-tall"));
        assertTrue(Files.readString(ROOT.resolve("en_us/glass_linear_entity_detector.txt")).contains("forward corridor"));
        assertTrue(Files.readString(ROOT.resolve("en_us/industrial_tube_gauge.txt")).contains("supporting block"));
    }

    @Test
    public void accessAndFluidGuidancePreservesIntegrationBoundaries() throws Exception {
        String stairs = Files.readString(ROOT.resolve("en_us/steel_catwalk_stairs.txt"));
        assertTrue(stairs.contains("high end"));
        assertTrue(stairs.contains("no crafting recipe"));
        String beam = Files.readString(ROOT.resolve("en_us/steel_double_t_support.txt"));
        assertTrue(beam.contains("A second attachment is rejected"));
        assertTrue(beam.contains("Treated Wood Beam"));
        String fluids = Files.readString(ROOT.resolve("en_us/overview.txt"));
        assertTrue(fluids.contains("not a machine tank"));
        assertTrue(fluids.contains("Milk is not"));
        assertTrue(fluids.contains("Lava remains hazardous"));
    }

    @Test
    public void storageAndMachineRisksStayDocumentedInBothLanguages() throws Exception {
        for (String lang : new String[]{"en_us", "fr_fr"}) {
            assertTrue(Files.readString(ROOT.resolve(lang + "/labeled_crate.txt")).contains("54"));
            assertTrue(Files.readString(ROOT.resolve(lang + "/factory_hopper.txt")).contains("1-32"));
            assertTrue(Files.readString(ROOT.resolve(lang + "/factory_placer.txt")).contains("18"));
            assertTrue(Files.readString(ROOT.resolve(lang + "/small_lab_furnace.txt")).contains("8 FE"));
            assertTrue(Files.readString(ROOT.resolve(lang + "/small_electrical_furnace.txt")).contains("64 FE"));
            assertTrue(Files.readString(ROOT.resolve(lang + "/passive_fluid_accumulator.txt")).contains("200 mB"));
        }
        assertTrue(Files.readString(ROOT.resolve("en_us/factory_placer.txt")).contains("several items in one cycle"));
        assertTrue(Files.readString(ROOT.resolve("en_us/small_waste_incinerator.txt")).contains("cannot be recovered"));
        assertTrue(Files.readString(ROOT.resolve("en_us/small_electrical_furnace.txt")).contains("operate with smelting set to OFF"));
    }

}
