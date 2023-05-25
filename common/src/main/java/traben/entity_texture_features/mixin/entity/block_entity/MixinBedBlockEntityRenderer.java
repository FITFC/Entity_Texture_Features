package traben.entity_texture_features.mixin.entity.block_entity;

import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BedBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import traben.entity_texture_features.mixin.accessor.SpriteContentsAccessor;
import traben.entity_texture_features.texture_handlers.ETFManager;
import traben.entity_texture_features.texture_handlers.ETFTexture;
import traben.entity_texture_features.entity_handlers.ETFBlockEntityWrapper;

import java.util.UUID;

import static traben.entity_texture_features.ETFClientCommon.ETFConfigData;

@Mixin(BedBlockEntityRenderer.class)
public abstract class MixinBedBlockEntityRenderer implements BlockEntityRenderer<BedBlockEntity> {

    private boolean isAnimatedTexture = false;
    private ETFTexture thisETFTexture = null;
    private ETFBlockEntityWrapper etf$bedStandInDummy = null;
    private Identifier etf$textureOfThis = null;
    private VertexConsumerProvider etf$vertexConsumerProviderOfThis = null;

    @ModifyArg(method = "renderPart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"),
            index = 1)
    private VertexConsumer etf$alterTexture(VertexConsumer vertices) {
        try {
            if (isAnimatedTexture || !ETFConfigData.enableCustomTextures || !ETFConfigData.enableCustomBlockEntities)
                return vertices;
            thisETFTexture = ETFManager.getInstance().getETFTexture(etf$textureOfThis, etf$bedStandInDummy, ETFManager.TextureSource.BLOCK_ENTITY, ETFConfigData.removePixelsUnderEmissiveBlockEntity);

            //System.out.println(thisETFTexture.toString()+thisETFTexture.thisIdentifier.toString());
            //return thisETFTexture.getTextureIdentifier();

            @SuppressWarnings("ConstantConditions") VertexConsumer alteredReturn = thisETFTexture == null ? null : etf$vertexConsumerProviderOfThis.getBuffer(RenderLayer.getEntityCutout(thisETFTexture.getTextureIdentifier(etf$bedStandInDummy)));
            return alteredReturn == null ? vertices : alteredReturn;
        } catch (Exception e) {
            return vertices;
        }
    }

    @Inject(method = "render(Lnet/minecraft/block/entity/BedBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BedBlockEntity;getWorld()Lnet/minecraft/world/World;",
                    shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void etf$getChestTexture(BedBlockEntity bedBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci, SpriteIdentifier spriteIdentifier) {
        try {
            isAnimatedTexture = ((SpriteContentsAccessor) spriteIdentifier.getSprite().getContents()).callGetFrameCount() != 1;
            if (!isAnimatedTexture) {
                //hopefully works in modded scenarios, assumes the mod dev uses the actual vanilla code process and texture pathing rules
                String nameSpace = spriteIdentifier.getTextureId().getNamespace();
                String texturePath = "textures/" + spriteIdentifier.getTextureId().getPath() + ".png";
                etf$textureOfThis = new Identifier(nameSpace, texturePath);
                etf$vertexConsumerProviderOfThis = vertexConsumerProvider;
                if (ETFConfigData.enableCustomTextures && ETFConfigData.enableCustomBlockEntities) {
                    World worldCheck = bedBlockEntity.getWorld();
                    if (worldCheck == null) worldCheck = MinecraftClient.getInstance().world;
                    if (worldCheck != null) {
                        etf$bedStandInDummy = new ETFBlockEntityWrapper(bedBlockEntity, UUID.nameUUIDFromBytes((bedBlockEntity.getPos().toString() + bedBlockEntity.getColor().toString()).getBytes()));
                    }
                    //System.out.println(etf$bedStandInDummy.toString());
                    //etf$bedStandInDummy.setPos(bedBlockEntity.getPos().getX(), bedBlockEntity.getPos().getY(), bedBlockEntity.getPos().getZ());
                    //chests don't have uuid so set UUID from something repeatable I chose from block pos
                    //etf$bedStandInDummy.setUuid();
                }
            }
        } catch (Exception ignored) {

        }
    }

    @Inject(method = "renderPart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V",
                    shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void etf$applyEmissiveBed(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ModelPart part, Direction direction, SpriteIdentifier sprite, int light, int overlay, boolean isFoot, CallbackInfo ci, VertexConsumer vertexConsumer) {
        //hopefully works in modded scenarios, assumes the mod dev uses the actual vanilla code process and texture pathing rules
        try {
            if (!isAnimatedTexture && ETFConfigData.enableEmissiveBlockEntities && (thisETFTexture != null)) {
                thisETFTexture.renderEmissive(matrices, vertexConsumers, part, ETFManager.EmissiveRenderModes.blockEntityMode());
            }
        } catch (Exception ignored) {

        }
    }
}


