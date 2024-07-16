package traben.entity_texture_features.mixin.mods.iris;
#if MC == MC_20_2 || MC < MC_20_1
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(Minecraft.class)
public class MixinInnerWrappedRenderType {}
#else
import net.irisshaders.iris.layer.InnerWrappedRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import traben.entity_texture_features.utils.ETFRenderLayerWithTexture;

import java.util.Optional;


/**
 * Required in case Iris wraps an instance of {@link ETFRenderLayerWithTexture}
 * <p>
 * This is assumed to be required, whereas I know {@link MixinOuterWrappedRenderType} is required.
 */
@Pseudo
@Mixin(value = InnerWrappedRenderType.class)
public abstract class MixinInnerWrappedRenderType implements ETFRenderLayerWithTexture {


    @Shadow
    public abstract RenderType unwrap();

    @Override
    public Optional<ResourceLocation> etf$getId() {
        if (unwrap() instanceof ETFRenderLayerWithTexture etf)
            return etf.etf$getId();
        return Optional.empty();
    }
}
#endif
