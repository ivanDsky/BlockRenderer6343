package blockrenderer6343.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import com.gtnewhorizon.gtnhlib.util.ItemRenderUtil;

import blockrenderer6343.ItemMultiblockPreview;

/** renders the preview item exactly like its controller item, nothing else */
public class MultiblockPreviewRenderer implements IItemRenderer {

    public static final MultiblockPreviewRenderer INSTANCE = new MultiblockPreviewRenderer();

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.INVENTORY;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        // forge's 3D inventory block path, but only when the controller itself renders as a 3D block
        if (helper != ItemRendererHelper.INVENTORY_BLOCK) return false;
        ItemStack controller = ItemMultiblockPreview.getControllerStack(item);
        Block block = controller == null ? null : Block.getBlockFromItem(controller.getItem());
        return block != null && RenderBlocks.renderItemIn3d(block.getRenderType());
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        ItemStack controller = ItemMultiblockPreview.getControllerStack(item);
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
