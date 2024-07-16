package traben.entity_texture_features.mixin.mods.iris;
#if MC == MC_20_2 || MC < MC_20_1
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(Minecraft.class)
public class MixinFullyBufferedMultiBufferSource {}
#else
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.entity_texture_features.features.ETFRenderContext;


/**
 * this is a copy of {@link net.minecraft.client.renderer.MultiBufferSource.BufferSource} but for iris's
 * custom entity {@link MultiBufferSource}
 * <p>
 * this should have no negative impact on iris's render process, other than of course adding more code that needs to run
 */
@Pseudo
@Mixin(FullyBufferedMultiBufferSource.class)
public class MixinFullyBufferedMultiBufferSource {


    @ModifyVariable(
            method = "getBuffer",
            at = @At(value = "HEAD"),
            index = 1, argsOnly = true)
    private RenderType etf$modifyRenderLayer(RenderType value) {
        RenderType newLayer = ETFRenderContext.modifyRenderLayerIfRequired(value);
        return newLayer == null ? value : newLayer;
    }


    @Inject(
            method = "getBuffer",
            at = @At(value = "RETURN"))
    private void etf$injectIntoGetBufferReturn(RenderType renderLayer, CallbackInfoReturnable<VertexConsumer> cir) {
        ETFRenderContext.insertETFDataIntoVertexConsumer(
                (MultiBufferSource) this,
                renderLayer,
                cir.getReturnValue());
    }

}
#endif