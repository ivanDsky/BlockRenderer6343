package blockrenderer6343.client.utils;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.IItemRenderer;

import com.gtnewhorizon.gtnhlib.util.ItemRenderUtil;

import blockrenderer6343.BlockRenderer6343;

/**
 * The multiblock structure "recipe" result: renders as the structure controller but is not it, so bookmarks and
 * autocraft don't consider the multiblock done once the controller is crafted. Display only - hidden from NEI. Its NBT
 * is just the serialized controller (id/Count/Damage) plus a "display" tag with name and lore.
 */
public class ItemMultiblockPreview extends Item implements IItemRenderer {

    public static final String REGISTRY_NAME = "multiblock_preview";
    public static final ItemMultiblockPreview INSTANCE = new ItemMultiblockPreview();

    private ItemMultiblockPreview() {
        setUnlocalizedName(BlockRenderer6343.MOD_ID + "." + REGISTRY_NAME);
        setCreativeTab(null);
    }

    public static ItemStack of(ItemStack controller, List<String> lore) {
        ItemStack stack = new ItemStack(INSTANCE);
        stack.setTagCompound(new NBTTagCompound());
        controller.writeToNBT(stack.getTagCompound());
        stack.setStackDisplayName(
                controller.getDisplayName() + " ("
                        + StatCollector.translateToLocal("blockrenderer6343.multiblock.structure")
                        + ")");
        NBTTagList loreTags = new NBTTagList();
        for (String line : lore) loreTags.appendTag(new NBTTagString(line));
        stack.getTagCompound().getCompoundTag("display").setTag("Lore", loreTags);
        return stack;
    }

    /** @return the controller stack this preview represents, or null */
    public static ItemStack getControllerStack(ItemStack stack) {
        return stack.hasTagCompound() ? ItemStack.loadItemStackFromNBT(stack.getTagCompound()) : null;
    }

    // renders exactly like the controller item, nothing else

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.INVENTORY;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        // forge's 3D inventory block path, but only when the controller itself renders as a 3D block
        if (helper != ItemRendererHelper.INVENTORY_BLOCK) return false;
        ItemStack controller = getControllerStack(item);
        Block block = controller == null ? null : Block.getBlockFromItem(controller.getItem());
        return block != null && RenderBlocks.renderItemIn3d(block.getRenderType());
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        ItemStack controller = getControllerStack(item);
        if (controller == null) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(
                controller.getItemSpriteNumber() == 0 ? TextureMap.locationBlocksTexture
                        : TextureMap.locationItemsTexture);
        Block block = Block.getBlockFromItem(controller.getItem());
        if (block != null && RenderBlocks.renderItemIn3d(block.getRenderType())) {
            ((RenderBlocks) data[0]).renderBlockAsItem(block, controller.getItemDamage(), 1.0F);
        } else {
            IIcon icon = controller.getIconIndex();
            if (icon != null) ItemRenderUtil.renderItem(type, icon);
        }
    }
}
