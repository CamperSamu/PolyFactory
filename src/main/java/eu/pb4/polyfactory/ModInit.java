package eu.pb4.polyfactory;

import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithGearBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.compat.power_networks.PowerNetworksInit;
import eu.pb4.polyfactory.loottable.FactoryLootTables;
import eu.pb4.polyfactory.models.ConveyorModel;
import eu.pb4.polyfactory.models.GenericParts;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.DebugData;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModInit implements ModInitializer {
	public static final String ID = "polyfactory";
	public static final Logger LOGGER = LoggerFactory.getLogger("PolyFactory");
    public static final boolean DEV = FabricLoader.getInstance().isDevelopmentEnvironment();
    @SuppressWarnings("PointlessBooleanExpression")
	public static final boolean DYNAMIC_ASSETS = true && DEV;
	//public static final boolean ENABLE_GEARS_ON_AXLE = false;

    public static Identifier id(String path) {
		return new Identifier(ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.warn("=================================================");
		LOGGER.warn("PolyFactory is still in pre-alpha state!");
		LOGGER.warn("Don't expect any stability or playability");
		LOGGER.warn("until official release!");
		LOGGER.warn("=================================================");

		FactoryBlocks.register();
		FactoryBlockEntities.register();
		FactoryItems.register();
		FactoryNodes.register();
		FactoryRecipeTypes.register();
		FactoryRecipeSerializers.register();
		AxleBlock.Model.ITEM_MODEL.getItem();
		WindmillBlock.Model.MODEL.getItem();
		AxleWithGearBlock.Model.ITEM_MODEL_GEAR.getItem();
		FactoryLootTables.register();
		FactoryCommands.register();
		GenericParts.SMALL_GEAR.isEmpty();
		DebugData.register();

		ConveyorModel.registerAssetsEvents();
		VirtualDestroyStage.setup();
		UiResourceCreator.setup();
		GuiTextures.register();
		PolydexCompat.register();
		PolymerResourcePackUtils.addModAssets(ID);
		PolymerResourcePackUtils.markAsRequired();

		if (FabricLoader.getInstance().isModLoaded("power_networks")) {
			PowerNetworksInit.register();
		}
	}
}
