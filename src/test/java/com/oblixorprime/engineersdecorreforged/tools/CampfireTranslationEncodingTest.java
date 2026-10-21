package com.oblixorprime.engineersdecorreforged.tools;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Guard the newly introduced French campfire wording, not unrelated legacy translations. */
public class CampfireTranslationEncodingTest {
    @Test
    public void frenchCampfireWordingUsesValidUtf8AndReadableApostrophes() throws Exception {
        for (String locale : new String[] {"fr_ca", "fr_fr"}) {
            String path = "/assets/immersive_engineer_decor_controls_tool_reforged/lang/" + locale + ".json";
            try (InputStream input = getClass().getResourceAsStream(path)) {
                assertNotNull(path, input);
                String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                String requirement = "Le repos exige l'une des huit cases voisines d'un feu de camp allum\u00e9.";
                String message = "Reposez-vous pr\u00e8s d'un feu de camp allum\u00e9, dans l'une des huit cases voisines.";
                assertTrue(locale + " help and JEI requirement", json.contains(requirement));
                assertEquals(locale + " exactly two requirement occurrences", 2,
                    json.split(java.util.regex.Pattern.quote(requirement), -1).length - 1);
                assertTrue(locale + " complete refusal message", json.contains(message));
                assertFalse(locale + " no Windows-1252 apostrophe control", json.contains("\u0092"));
            }
        }
    }
}
