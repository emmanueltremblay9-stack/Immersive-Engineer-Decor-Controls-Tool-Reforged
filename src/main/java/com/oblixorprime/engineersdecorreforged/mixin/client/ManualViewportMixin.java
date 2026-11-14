package com.oblixorprime.engineersdecorreforged.mixin.client;

import blusunrize.lib.manual.gui.ManualScreen;
import com.oblixorprime.engineersdecorreforged.client.ManualViewportFit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** IE 12.4.2: fit only this addon's entry before IE lays out its controls. */
@Mixin(value=ManualScreen.class, remap=false)
abstract class ManualViewportMixin extends Screen {
    @Shadow private float scaleFactor;
    @Shadow private int xSize;
    @Shadow private int ySize;
    @Unique private boolean iedctr$adjusted;
    protected ManualViewportMixin(Component title) { super(title); }

    @Inject(method="init()V", at=@At("HEAD"), require=1)
    private void iedctr$restoreDimensions(CallbackInfo ci) {
        if (iedctr$adjusted) {
            var window=Minecraft.getInstance().getWindow();
            width=window.getGuiScaledWidth();
            height=window.getGuiScaledHeight();
            iedctr$adjusted=false;
        }
    }

    @Inject(method="init()V", at=@At(value="INVOKE", target="Lblusunrize/lib/manual/ManualInstance;openManual()V"), require=1)
    private void iedctr$fitOwnedEntry(CallbackInfo ci) {
        var entry=((ManualScreen)(Object)this).getCurrentPage();
        if (entry==null) return;
        var window=Minecraft.getInstance().getWindow();
        double original=scaleFactor*window.getGuiScale();
        double fitted=ManualViewportFit.fit(entry.getLocation().getNamespace(), original,
                window.getWidth(), window.getHeight(), xSize, ySize);
        if (fitted>=original) return;
        width=(int)Math.ceil(window.getWidth()/fitted);
        height=(int)Math.ceil(window.getHeight()/fitted);
        scaleFactor=(float)(fitted/window.getGuiScale());
        iedctr$adjusted=true;
    }
}
