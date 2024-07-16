package traben.entity_texture_features.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.entity_texture_features.ETF;
import traben.entity_texture_features.config.ETFConfigWarning;
import traben.entity_texture_features.config.ETFConfigWarnings;
import traben.entity_texture_features.features.ETFManager;
import traben.entity_texture_features.features.ETFRenderContext;
import traben.entity_texture_features.features.texture_handlers.ETFTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.io.InputStream;
import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.MutableComponent;
#if MC > MC_20_2
import net.minecraft.network.chat.contents.PlainTextContents;
#else
import net.minecraft.network.chat.contents.LiteralContents;
#endif
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;


public abstract class ETFUtils2 {

    public static @NotNull ResourceLocation res(String fullPath){
        #if MC >= MC_21
        return ResourceLocation.parse(fullPath);
        #else 
        return new ResourceLocation(fullPath);
        #endif
    }

    public static @NotNull ResourceLocation res(String namespace, String path){
        #if MC >= MC_21
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        #else 
        return new ResourceLocation(namespace, path);
        #endif
    }

    public static ResourceLocation getETFVariantNotNullForInjector(ResourceLocation identifier) {
        //do not modify texture
        if (identifier == null
                || ETFRenderContext.getCurrentEntity() == null
                || !ETFRenderContext.isAllowedToRenderLayerTextureModify())
            return identifier;

        //get etf modified texture
        ETFTexture etfTexture = ETFManager.getInstance().getETFTextureVariant(identifier, ETFRenderContext.getCurrentEntity());
        if (ETFRenderContext.isAllowedToPatch()) {
            etfTexture.assertPatchedTextures();
        }
        ResourceLocation modified = etfTexture.getTextureIdentifier(ETFRenderContext.getCurrentEntity());

        //check not null just to be safe, it shouldn't be however
        //noinspection ConstantValue
        return modified == null ? identifier : modified;
    }

    public static boolean renderEmissive(ETFTexture texture, MultiBufferSource provider, RenderMethodForOverlay renderer) {
        if (!ETF.config().getConfig().canDoEmissiveTextures()) return false;
        ResourceLocation emissive = texture.getEmissiveIdentifierOfCurrentState();
        if (emissive != null) {
            boolean wasAllowed = ETFRenderContext.isAllowedToRenderLayerTextureModify();
            ETFRenderContext.preventRenderLayerTextureModify();

            VertexConsumer emissiveConsumer = provider.getBuffer(
                    ETFRenderContext.canRenderInBrightMode() ?
                            RenderType.beaconBeam(emissive, true) :
                            ETFRenderContext.shouldEmissiveUseCullingLayer() ?
                                    RenderType.entityTranslucentCull(emissive) :
                                    RenderType.entityTranslucent(emissive));

            if (wasAllowed) ETFRenderContext.allowRenderLayerTextureModify();

            ETFRenderContext.startSpecialRenderOverlayPhase();
            renderer.render(emissiveConsumer, ETF.EMISSIVE_FEATURE_LIGHT_VALUE);
            ETFRenderContext.endSpecialRenderOverlayPhase();
            return true;
        }
        return false;
    }

    public static boolean renderEnchanted(ETFTexture texture, MultiBufferSource provider, int light, RenderMethodForOverlay renderer) {

        //attempt enchanted render
        ResourceLocation enchanted = texture.getEnchantIdentifierOfCurrentState();
        if (enchanted != null) {
            boolean wasAllowed = ETFRenderContext.isAllowedToRenderLayerTextureModify();
            ETFRenderContext.preventRenderLayerTextureModify();
            VertexConsumer enchantedVertex = ItemRenderer.getArmorFoilBuffer(provider, RenderType.armorCutoutNoCull(enchanted), #if MC < MC_21 false, #endif true);
            if (wasAllowed) ETFRenderContext.allowRenderLayerTextureModify();

            ETFRenderContext.startSpecialRenderOverlayPhase();
            renderer.render(enchantedVertex, light);
            ETFRenderContext.endSpecialRenderOverlayPhase();
            return true;
        }
        return false;
    }

    @Nullable
    public static ResourceLocation addVariantNumberSuffix(@NotNull ResourceLocation identifier, int variant) {
        var changed = ETFUtils2.res(addVariantNumberSuffix(identifier.toString(), variant));
        return identifier.equals(changed) ? null : changed;
    }

    @NotNull
    public static String addVariantNumberSuffix(String identifierString, int variant) {
        String file;
        if (identifierString.endsWith(".png")) {
            file = "png";
        } else {
            String[] split = identifierString.split("\\.");
            file = split[split.length - 1];
        }
        if (variant < 2)
            return identifierString;
        if (identifierString.matches("\\D+\\d+\\." + file)) {
            return identifierString.replace("." + file, "." + variant + "." + file);
        }
        return identifierString.replace("." + file, variant + "." + file);
    }

    @Nullable
    public static ResourceLocation replaceIdentifier(ResourceLocation id, String regex, String replace) {
        if (id == null) return null;
        ResourceLocation forReturn;
        try {
            forReturn = ETFUtils2.res(id.getNamespace(), id.getPath().replaceFirst(regex, replace));
        } catch (ResourceLocationException idFail) {
            ETFUtils2.logError(ETF.getTextFromTranslation("config.entity_texture_features.illegal_path_recommendation").getString() + "\n" + idFail);
            forReturn = null;
        } catch (Exception e) {
            forReturn = null;
        }
        return forReturn;
    }

    @Nullable
    public static String returnNameOfHighestPackFromTheseMultiple(String[] packNameList) {
        ArrayList<String> packNames = new ArrayList<>(Arrays.asList(packNameList));
        //loop through and remove the one from the lowest pack of the first 2 entries
        //this iterates over the whole array
        while (packNames.size() >= 2) {
            if (ETFManager.getInstance().KNOWN_RESOURCEPACK_ORDER.indexOf(packNames.get(0)) >= ETFManager.getInstance().KNOWN_RESOURCEPACK_ORDER.indexOf(packNames.get(1))) {
                packNames.remove(1);
            } else {
                packNames.remove(0);
            }
        }
        //here the array is down to 1 entry which should be the one in the highest pack
        return packNames.get(0);
    }

    @Nullable
    public static String returnNameOfHighestPackFromTheseTwo(String pack1, String pack2) {
        if (pack1 == null) return null;

        if (pack1.equals(pack2)) {
            return pack1;
        }
        if (ETFManager.getInstance().KNOWN_RESOURCEPACK_ORDER.indexOf(pack1)
                >= ETFManager.getInstance().KNOWN_RESOURCEPACK_ORDER.indexOf(pack2)) {
            return pack1;
        } else {
            return pack2;
        }
    }

//    @SuppressWarnings("BooleanMethodIsAlwaysInverted") //makes more logical sense
//    public static boolean isNativeImageEmpty(@NotNull NativeImage image) {
//        boolean foundNonEmptyPixel = false;
//        for (int x = 0; x < image.getWidth(); x++) {
//            for (int y = 0; y < image.getHeight(); y++) {
//                if (image.getColor(x, y) != 0) {
//                    foundNonEmptyPixel = true;
//                    break;
//                }
//            }
//            if (foundNonEmptyPixel) break;
//        }
//        return !foundNonEmptyPixel;
//    }

    @Nullable
    public static Properties readAndReturnPropertiesElseNull(ResourceLocation path) {
        Properties props = new Properties();
        try {
            @SuppressWarnings("OptionalGetWithoutIsPresent") //try catch is intended
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(path).get();
            try {
                InputStream in = resource.open();
                props.load(in);
                in.close();
                return props;
            } catch (Exception e) {
                //resource.close();
                return null;
            }
        } catch (Exception e) {
            return null;
        }

    }

    @Nullable
    public static List<Properties> readAndReturnAllLayeredPropertiesElseNull(ResourceLocation path) {
        List<Properties> props = new ArrayList<>();
        try {
            var resources = Minecraft.getInstance().getResourceManager().getResourceStack(path);
            for (Resource resource : resources) {
                if (resource == null) continue;
                Properties prop = new Properties();
                try {
                    InputStream in = resource.open();
                    prop.load(in);
                    in.close();
                    if (!prop.isEmpty()) {
                        props.add(prop);
                    }
                } catch (Exception ignored) {}
            }
            if (!props.isEmpty()) {
                return props;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static NativeImage getNativeImageElseNull(@Nullable ResourceLocation identifier) {
//        if (identifier != null) {
//            NativeImage image = ETFManager.getInstance().KNOWN_NATIVE_IMAGES.get(identifier);
//            if (image != null) {
//                return image;
//            }
//        }
        NativeImage img;
        try {
            //try catch is intended
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(identifier);
            if (resource.isPresent()) {
                InputStream in = resource.get().open();
                try {
                    img = NativeImage.read(in);
                    in.close();
//                    ETFManager.getInstance().KNOWN_NATIVE_IMAGES.put(identifier, img);
                    return img;
                } catch (Exception e) {
                    //resource.close();
                    in.close();
                    return null;
                }
            } else {
                AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(identifier);
                if (texture instanceof DynamicTexture nativeImageBackedTexture) {
                    var image2 = nativeImageBackedTexture.getPixels();
                    if (image2 == null) return null;
                    NativeImage image3 = new NativeImage(image2.getWidth(), image2.getHeight(), false);
                    image3.copyFrom(image2);
                    return image3;
                }
                return null;
            }
        } catch (Exception e) {
            return null;
        }

    }

    //improvements to logging by @Maximum#8760
    public static void logMessage(String obj) {
        logMessage(obj, false);
    }

    public static void logMessage(String obj, boolean inChat) {
        if (!obj.endsWith(".")) obj = obj + ".";
        if (inChat) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(MutableComponent.create(
                        new #if MC > MC_20_2 PlainTextContents.LiteralContents #else LiteralContents #endif
                                ("§a[INFO]§r [Entity Texture Features]: " + obj))/*.formatted(Formatting.GRAY, Formatting.ITALIC)*/ , false);
            } else {
                ETF.LOGGER.info(obj);
            }
        } else {
            ETF.LOGGER.info(obj);
        }
    }

    //improvements to logging by @Maximum#8760
    public static void logWarn(String obj) {
        logWarn(obj, false);
    }

    public static void logWarn(String obj, boolean inChat) {
        if (!obj.endsWith(".")) obj = obj + ".";
        if (inChat) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(MutableComponent.create(
                        new #if MC > MC_20_2 PlainTextContents.LiteralContents #else LiteralContents #endif
                                ("§e[WARN]§r [Entity Texture Features]: " + obj)).withStyle(ChatFormatting.YELLOW), false);
            } else {
                ETF.LOGGER.warn(obj);
            }
        } else {
            ETF.LOGGER.warn(obj);
        }
    }

    //improvements to logging by @Maximum#8760
    public static void logError(String obj) {
        logError(obj, false);
    }

    public static void logError(String obj, boolean inChat) {
        if (!obj.endsWith(".")) obj = obj + ".";
        if (inChat) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(MutableComponent.create(
                        new #if MC > MC_20_2 PlainTextContents.LiteralContents #else LiteralContents #endif
                                ("§4[ERROR]§r [Entity Texture Features]: " + obj)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            } else {
                ETF.LOGGER.error(obj);
            }
        } else {
            ETF.LOGGER.error(obj);
        }
    }

    public static NativeImage emptyNativeImage() {
        return emptyNativeImage(64, 64);
    }

    public static NativeImage emptyNativeImage(int Width, int Height) {
        NativeImage empty = new NativeImage(Width, Height, false);
        empty.fillRect(0, 0, Width, Height, 0);
        return empty;
    }

    public static boolean registerNativeImageToIdentifier(NativeImage image, ResourceLocation identifier) {
        if (image == null || identifier == null) {
            logError("registering native image failed: " + image + ", " + identifier);
            return false;
        }
        try {
            NativeImage closableImage = new NativeImage(image.getWidth(), image.getHeight(), true);
            closableImage.copyFrom(image);

            Minecraft.getInstance().getTextureManager().release(identifier);

            DynamicTexture closableBackedTexture = new DynamicTexture(closableImage);
            Minecraft.getInstance().getTextureManager().register(identifier, closableBackedTexture);

            return true;
        } catch (Exception e) {
            logError("registering native image failed: " + e);
            return false;
        }

    }

    public static void checkModCompatibility() {
        for (ETFConfigWarning warning :
                ETFConfigWarnings.getRegisteredWarnings()) {
            warning.testWarningAndApplyFixIfEnabled();
        }
    }


    public interface RenderMethodForOverlay {
        void render(VertexConsumer consumer, int light);
    }

}
