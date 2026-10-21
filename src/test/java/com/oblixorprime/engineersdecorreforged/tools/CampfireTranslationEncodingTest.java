package com.oblixorprime.engineersdecorreforged.tools;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
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
                String json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(input.readAllBytes())).toString();
                assertTrue(locale + " language", json.contains("\"language\": \"Fran\u00e7ais\""));
                String requirement = "Le repos exige l'une des huit cases voisines d'un feu de camp allum\u00e9.";
                String message = "Reposez-vous pr\u00e8s d'un feu de camp allum\u00e9, dans l'une des huit cases voisines.";
                assertTrue(locale + " help and JEI requirement", json.contains(requirement));
                assertEquals(locale + " exactly two requirement occurrences", 2,
                    json.split(java.util.regex.Pattern.quote(requirement), -1).length - 1);
                assertTrue(locale + " complete refusal message", json.contains(message));
                assertFalse(locale + " no Windows-1252 apostrophe control", json.contains("\u0092"));
                for (String line : json.split("\\n")) {
                    if (line.contains("sleeping_bag.help") || line.contains("jei.info.immersive_engineer_decor_controls_tool_reforged.sleeping_bag")
                            || line.contains("sleeping_bag.msg.campfire")) {
                        assertFalse(locale + " no replacement", line.contains("\uFFFD"));
                        assertFalse(locale + " no double-encoded UTF-8", line.contains("\u00C3") || line.contains("\u00C2") || line.contains("\u00E2"));
                    }
                }
            }
        }
    }
}
