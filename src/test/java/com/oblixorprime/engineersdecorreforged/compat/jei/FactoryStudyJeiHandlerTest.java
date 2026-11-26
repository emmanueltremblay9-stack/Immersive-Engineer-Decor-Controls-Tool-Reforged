package com.oblixorprime.engineersdecorreforged.compat.jei;

import com.oblixorprime.engineersdecorreforged.utility.FactoryStudyGeometry;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real projected Rect2i bounds; no mocked Minecraft screen or JEI runtime claim. */
public final class FactoryStudyJeiHandlerTest {
   @Test public void itemAreasRoundOutwardInAbsoluteScreenCoordinates() {
      try {
         Class.forName("mezz.jei.api.gui.handlers.IGuiContainerHandler");
      } catch (ClassNotFoundException absentOptionalRuntime) {
         Assume.assumeNoException("Optional JEI runtime is disabled", absentOptionalRuntime);
      }
      var item = new FactoryStudyGeometry.Rect(2, 4, 16, 16);
      area(item, new FactoryStudyGeometry.ViewTransform(1.0F, 10, 20), 12, 24, 16, 16);
      area(item, new FactoryStudyGeometry.ViewTransform(0.5F, 10.25F, 20.75F), 11, 22, 9, 9);
      area(item, new FactoryStudyGeometry.ViewTransform(0.25F, 10.125F, 20.375F), 10, 21, 5, 5);
      // A partly fractional native area must retain every pixel touched by the logical item.
      area(item, new FactoryStudyGeometry.ViewTransform(0.5F, 0, 0), 1, 2, 8, 8);
   }

   private static void area(FactoryStudyGeometry.Rect item, FactoryStudyGeometry.ViewTransform view,
                            int x, int y, int width, int height) {
      var actual = FactoryStudyJeiHandler.absoluteArea(view, item);
      assertEquals(x, actual.getX());
      assertEquals(y, actual.getY());
      assertEquals(width, actual.getWidth());
      assertEquals(height, actual.getHeight());
   }
}
