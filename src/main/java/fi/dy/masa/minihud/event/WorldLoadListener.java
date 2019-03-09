package fi.dy.masa.minihud.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.LiteModMiniHud;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.renderer.OverlayRenderer;
import fi.dy.masa.minihud.renderer.RenderContainer;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import javax.annotation.Nullable;
import java.io.File;

public class WorldLoadListener implements IWorldLoadListener
{
    private boolean hasCachedSeed;
    private long cachedSeed;
    private boolean renderersRead;

    @Override
    public void onWorldLoadPre(@Nullable WorldClient world, Minecraft mc)
    {
        WorldClient worldOld = Minecraft.getMinecraft().world;

        // Save the settings before the integrated server gets shut down
        if (worldOld != null)
        {
            this.writeDataPerDimension();

            // Quitting to main menu
            if (world == null)
            {
                this.writeDataGlobal();
            }

            this.hasCachedSeed = world != null && Configs.Generic.DONT_RESET_SEED_ON_DIMENSION_CHANGE.getBooleanValue();

            if (this.hasCachedSeed)
            {
                this.cachedSeed = world.getSeed();
            }
        }
        else
        {
            // Logging in to a world, load the global data
            if (world != null)
            {
                this.readStoredDataGlobal();
                OverlayRenderer.resetRenderTimeout();
            }

            this.hasCachedSeed = false;
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable WorldClient world, Minecraft mc)
    {
        ShapeManager.INSTANCE.clear();

        // Clear the cached data
        DataStorage.getInstance().onWorldLoad();

        if (world != null)
        {
            this.readStoredDataPerDimension();

            if (this.hasCachedSeed)
            {
                DataStorage.getInstance().setWorldSeed(this.cachedSeed);
                this.hasCachedSeed = false;
            }
        }
    }

    private void writeDataPerDimension()
    {
        File file = getCurrentStorageFile(false);
        JsonObject root = new JsonObject();

        root.add("shapes", ShapeManager.INSTANCE.toJson());
        root.add("data_storage", DataStorage.getInstance().toJson());

        JsonUtils.writeJsonToFile(root, file);
    }

    private void writeDataGlobal()
    {
        File file = getCurrentStorageFile(true);
        JsonObject root = new JsonObject();

        root.add("renderers", RenderContainer.INSTANCE.toJson());

        JsonUtils.writeJsonToFile(root, file);
    }

    private void readStoredDataPerDimension()
    {
        // Per-dimension file
        File file = getCurrentStorageFile(false);
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();

            if (JsonUtils.hasObject(root, "shapes"))
            {
                ShapeManager.INSTANCE.fromJson(JsonUtils.getNestedObject(root, "shapes", false));
            }

            if (JsonUtils.hasObject(root, "data_storage"))
            {
                DataStorage.getInstance().fromJson(JsonUtils.getNestedObject(root, "data_storage", false));
            }
        }
    }

    private void readStoredDataGlobal()
    {
        // Global file
        File file = getCurrentStorageFile(true);
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();

            if (this.renderersRead == false && JsonUtils.hasObject(root, "renderers"))
            {
                RenderContainer.INSTANCE.fromJson(JsonUtils.getNestedObject(root, "renderers", false));
            }
        }
    }

    public static File getCurrentConfigDirectory()
    {
        return new File(FileUtils.getConfigDirectory(), Reference.MOD_ID);
    }

    private static File getCurrentStorageFile(boolean globalData)
    {
        File dir = getCurrentConfigDirectory();

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            LiteModMiniHud.logger.warn("Failed to create the config directory '{}'", dir.getAbsolutePath());
        }

        return new File(dir, getStorageFileName(globalData));
    }

    private static String getStorageFileName(boolean globalData)
    {
        Minecraft mc = Minecraft.getMinecraft();
        String name = StringUtils.getWorldOrServerName();

        if (name != null)
        {
            if (globalData)
            {
                return name + ".json";
            }
            else
            {
                return name + "_dim" + WorldUtils.getDimensionId(mc.world) + ".json";
            }
        }

        return Reference.MOD_ID + "_default.json";
    }
}
