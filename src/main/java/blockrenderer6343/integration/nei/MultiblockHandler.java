package blockrenderer6343.integration.nei;

import static blockrenderer6343.integration.nei.GuiMultiblockHandler.SLOT_SIZE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import blockrenderer6343.ItemMultiblockPreview;
import blockrenderer6343.client.utils.BRButton;
import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.client.utils.ConstructableData;
import blockrenderer6343.client.utils.GuiSlider;
import codechicken.nei.LayoutManager;
import codechicken.nei.PositionedStack;
import codechicken.nei.RecipeSearchField;
import codechicken.nei.SearchField;
import codechicken.nei.SearchTokenParser;
import codechicken.nei.api.API;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton.UpdateRecipeButtonsEvent;
import codechicken.nei.recipe.RecipeCatalysts;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import it.unimi.dsi.fastutil.objects.ObjectSet;

@EventBusSubscriber
public abstract class MultiblockHandler extends TemplateRecipeHandler {

    public static final int CANDIDATE_SLOTS_X = 150;
    public static final int CANDIDATE_SLOTS_Y = 20;
    public static final int CANDIDATE_IN_COlUMN = 6;
    private static ItemStack lastStack;
    protected RecipeCacher recipeCacher = new RecipeCacher();
    protected GuiMultiblockHandler guiHandler;
    protected IConstructable[] currentMultiblocks;
    protected int oldRecipe;
    protected static RecipeSearchField recipeSearchField;

    static {
        // this is a hack to let us hijack the "in recipe search" field while in the preview
        API.addSearchProvider(
                new SearchField.SearchParserProvider(
                        '䌡',
                        "inpreview",
                        EnumChatFormatting.RESET,
                        a -> new InPreviewFilter()) {

                    @Override
                    public SearchTokenParser.SearchMode getSearchMode() {
                        return SearchTokenParser.SearchMode.ALWAYS;
                    }
                });
        try {
            recipeSearchField = (RecipeSearchField) ReflectionHelper.findField(GuiRecipe.class, "searchField")
                    .get(null);
        } catch (Exception ignored) {}
    }

    public MultiblockHandler(GuiMultiblockHandler guiHandler) {
        this.guiHandler = guiHandler;
        guiHandler.setOnIngredientChanged(this::resetPositionedIngredients);
        guiHandler.setOnCandidateChanged(this::setResults);
    }

    public abstract @NotNull ItemStack getConstructableStack(IConstructable multiblock);

    protected abstract @NotNull ObjectSet<IConstructable> tryLoadingMultiblocks(ItemStack candidate);

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        loadRecipes(result);
        super.loadCraftingRecipes(result);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        loadRecipes(ingredient);
        super.loadUsageRecipes(ingredient);
    }

    private void loadRecipes(ItemStack stack) {
        currentMultiblocks = null;
        oldRecipe = -1;
        // the preview item means its stored controller
        if (stack.getItem() == ItemMultiblockPreview.INSTANCE) stack = ItemMultiblockPreview.getControllerStack(stack);
        if (stack == null) return;
        ObjectSet<IConstructable> multiblocks = tryLoadingMultiblocks(stack);
        if (multiblocks.isEmpty()) return;
        lastStack = stack;
        currentMultiblocks = multiblocks.toArray(new IConstructable[0]);
    }

    @Override
    public void drawBackground(int recipe) {
        super.drawBackground(recipe);
        if (currentMultiblocks == null) return;
        if (oldRecipe != recipe) {
            oldRecipe = recipe;
            IConstructable multi = currentMultiblocks[recipe];
            guiHandler.loadMultiblock(
                    multi,
                    getConstructableStack(multi),
                    ConstructableData.getTierData(multi).setTierFromStack(lastStack));
        }

        guiHandler.recalculateSearch(recipeSearchField.text());
        guiHandler.drawMultiblock(recipe);
    }

    public static @Nullable MultiblockHandler getHandlerFromGui(GuiScreen gui) {
        // noinspection rawtypes
        if (!(gui instanceof GuiRecipe recipe)) return null;
        if (recipe.getHandler() instanceof MultiblockHandler handler) {
            return handler;
        }
        return null;
    }

    public static @Nullable GuiMultiblockHandler getCurrentGuiHandler() {
        MultiblockHandler handler = getHandlerFromGui(Minecraft.getMinecraft().currentScreen);
        if (handler != null) {
            return handler.getGuiHandler();
        }
        return null;
    }

    @Override
    public String getOverlayIdentifier() {
        return getClass().getName();
    }

    @Override
    public int numRecipes() {
        return currentMultiblocks == null ? 0 : currentMultiblocks.length;
    }

    @Override
    public String getGuiTexture() {
        return "blockrenderer6343:textures/void.png";
    }

    @Override
    public String getRecipeName() {
        return StringUtils.abbreviate(guiHandler.getMultiblockName(), 25);
    }

    public String getFullRecipeName() {
        return guiHandler.getMultiblockName();
    }

    @Override
    public String getRecipeTabName() {
        return StatCollector.translateToLocal("blockrenderer6343.multiblock.structure");
    }

    protected @NotNull GuiMultiblockHandler getGuiHandler() {
        return guiHandler;
    }

    @Override
    public List<PositionedStack> getIngredientStacks(int recipe) {
        // ingredients are whatever the structure view currently renders (respects the layer slider),
        // reversed so the recipe starts with controllers; only the preview item is visible, the rest is
        // parked off-screen (below y=9999) for favorites/bookmarks
        if (GuiMultiblockHandler.renderer == null) return Collections.emptyList();
        List<ItemStack> stacks = BRUtil.getIngredients(GuiMultiblockHandler.renderer);
        List<PositionedStack> ingredients = new ArrayList<>(stacks.size());
        for (int i = stacks.size() - 1; i >= 0; i--) {
            if (BRNEIConfig.getConfigValue(BRNEIConfig.FILTER_HATCH) && BRUtil.hatchFilter.test(stacks.get(i)))
                continue;
            ingredients.add(new PositionedStack(stacks.get(i), 0, 9999 + ingredients.size(), false));
        }
        return ingredients;
    }

    @Override
    public PositionedStack getResultStack(int recipe) {
        // dedicated preview item: renders as the controller but means building the whole structure, so NEI
        // bookmarks/autocraft don't consider the multiblock done once the controller itself is crafted.
        // parked off-screen like the ingredients: it must exist for favorites/in-recipe search, not be seen
        return new PositionedStack(
                ItemMultiblockPreview.of(getConstructableStack(currentMultiblocks[recipe]), buildLoreLines(recipe)),
                0,
                9999,
                false);
    }

    /** @return the current slider state as lore lines, e.g. "Tier: 2", "Glass: 3 (Tempered Glass)" */
    private List<String> buildLoreLines(int recipe) {
        List<String> lore = new ArrayList<>();
        ConstructableData data = ConstructableData.getTierData(currentMultiblocks[recipe]);
        for (BRButton button : guiHandler.allButtons) {
            if (!(button instanceof GuiSlider slider)) continue;
            ItemStack channelItem = data.getChannelItems().get(StringUtils.uncapitalize(slider.name));
            lore.add(
                    EnumChatFormatting.GRAY + slider.name
                            + ": "
                            + slider.getValueText()
                            + (channelItem != null ? " (" + channelItem.getDisplayName() + ")" : ""));
        }
        return lore;
    }

    @Override
    public List<PositionedStack> getOtherStacks(int recipe) {
        return arecipes.isEmpty() ? Collections.emptyList() : arecipes.get(0).getOtherStacks();
    }

    public void resetPositionedIngredients(List<ItemStack> ingredients) {
        RecipeCatalysts.putRecipeCatalysts(getOverlayIdentifier(), ingredients);
    }

    public void setResults(List<List<ItemStack>> results) {
        arecipes.clear();
        recipeCacher.setResults(results);
        arecipes.add(recipeCacher);
    }

    public class RecipeCacher extends CachedRecipe {

        private final List<PositionedStack> positionedResults = new ArrayList<>();

        public void setResults(List<List<ItemStack>> results) {
            positionedResults.clear();
            int columnCount = results.size() / CANDIDATE_IN_COlUMN + 1;
            int realCandidateInColumn = results.size() % columnCount == 0 ? results.size() / columnCount
                    : results.size() / columnCount + 1;
            for (int i = 0; i < results.size(); i++) {
                PositionedStack result = new PositionedStack(
                        results.get(i),
                        CANDIDATE_SLOTS_X - (columnCount - 1) * SLOT_SIZE + (i / realCandidateInColumn) * SLOT_SIZE,
                        CANDIDATE_SLOTS_Y + (i % realCandidateInColumn) * SLOT_SIZE);
                result.generatePermutations();
                positionedResults.add(result);
            }
        }

        @Override
        public PositionedStack getResult() {
            return null;
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            return getCycledIngredients(cycleticks / 20, positionedResults);
        }
    }

    public static class InPreviewFilter implements ItemFilter {

        @Override
        public boolean matches(ItemStack item) {
            GuiMultiblockHandler handler = getCurrentGuiHandler();
            if (handler != null && !LayoutManager.searchField.focused()) {
                return recipeSearchField.isVisible() && recipeSearchField.focused()
                        && !recipeSearchField.text().isEmpty();
            }
            return false;
        }
    }

    @SubscribeEvent
    @SuppressWarnings({ "unused", "rawtypes" })
    public static void onPostOverlay(UpdateRecipeButtonsEvent.Post event) {
        if (event.gui instanceof GuiRecipe recipe && recipe.getHandler() instanceof MultiblockHandler) {
            // NEI's overlay button can't fill a multiblock anywhere; keep the favorite button working
            event.buttonList.removeIf(GuiOverlayButton.class::isInstance);
        }
    }
}
