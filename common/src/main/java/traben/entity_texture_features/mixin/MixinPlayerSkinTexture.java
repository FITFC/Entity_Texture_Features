package traben.entity_texture_features.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.HttpTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.entity_texture_features.ETF;
import traben.entity_texture_features.config.ETFConfig;
import traben.entity_texture_features.features.player.ETFPlayerTexture;


@Mixin(HttpTexture.class)
public abstract class MixinPlayerSkinTexture {


    @Shadow
    private static void setNoAlpha(final NativeImage image, final int x, final int y, final int width, final int height) {
    }

    @Inject(method = "setNoAlpha", cancellable = true, at = @At("HEAD"))
    private static void etf$cancelling(final NativeImage image, final int x1, final int y1, final int x2, final int y2, final CallbackInfo ci) {
        if (ETF.config().getConfig() != null) {
            var mode = ETF.config().getConfig().skinTransparencyMode;

            if (mode == ETFConfig.SkinTransparencyMode.ETF_SKINS_ONLY && ETFPlayerTexture.remappingETFSkin) {
                ci.cancel();
            } else if (mode == ETFConfig.SkinTransparencyMode.ALL) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "processLegacySkin",
            cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/HttpTexture;setNoAlpha(Lcom/mojang/blaze3d/platform/NativeImage;IIII)V"
                    , shift = At.Shift.BEFORE, ordinal = 0))
    private void etf$differentAlpha(final NativeImage image, final CallbackInfoReturnable<NativeImage> cir) {
        if (ETF.config().getConfig() != null && ETF.config().getConfig().skinTransparencyInExtraPixels) {
            //limit the alpha regions to the uv specifically mapped to the vanilla model only

            //head
            setNoAlpha(image, 8, 0, 24, 8);
            setNoAlpha(image, 0, 8, 32, 16);

            //needed for the notch hack
            // only relevant for legacy skins without transparency in the upper head layer at all
//            if (i==32) {
//                doNotchTransparencyHack(image, 32, 0, 64, 32);
//            }

            //og body
            setNoAlpha(image, 4, 16, 12, 20);
            setNoAlpha(image, 20, 16, 36, 20);
            setNoAlpha(image, 44, 16, 52, 20);
            //main
            setNoAlpha(image, 0, 20, 64, 32);

            //alt limbs
            setNoAlpha(image, 20, 48, 28, 52);
            setNoAlpha(image, 36, 48, 44, 52);
            //main
            setNoAlpha(image, 16, 52, 48, 64);

            cir.setReturnValue(image);
        }
    }
}


