package traben.entity_texture_features.neoforge;


#if MC >= MC_20_6
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
#else
import net.neoforged.neoforge.client.ConfigScreenHandler;
#endif

import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

import net.neoforged.neoforgespi.language.IModInfo;
import traben.entity_texture_features.ETF;

import java.util.List;

@Mod("entity_texture_features")
public class ETFClientNeoForge {

    public ETFClientNeoForge() {

        // Submit our event bus to let architectury register our content on the right time
        //EventBuses.registerModEventBus(ExampleMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        if (FMLEnvironment.dist.isClient()) {
            try {
                ModLoadingContext.get().registerExtensionPoint(
                        #if MC >= MC_20_6
                        IConfigScreenFactory.class,
                        ()-> ETF::getConfigScreen);
                        #else
                        ConfigScreenHandler.ConfigScreenFactory.class,
                        ()-> new ConfigScreenHandler.ConfigScreenFactory(ETF::getConfigScreen));
                        #endif
                       // () -> new ConfigScreenHandler.ConfigScreenFactory(ETF::getConfigScreen));
            } catch (NoClassDefFoundError e) {
                System.out.println("[Entity Texture Features]: Mod config broken, download latest forge version");
            }
            ETF.start();

        } else {
            throw new UnsupportedOperationException("Attempting to load a clientside only mod on the server, refusing");
        }
    }


}
