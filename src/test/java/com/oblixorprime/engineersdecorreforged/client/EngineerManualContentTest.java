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

}
