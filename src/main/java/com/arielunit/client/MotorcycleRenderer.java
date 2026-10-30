package com.arielunit.client;

import com.arielunit.ArielUnit;
import com.arielunit.MotorcycleEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Desenha a moto usando o modelo 3D do item "ariel_motorcycle". */
public class MotorcycleRenderer extends EntityRenderer<MotorcycleEntity> {
    /** Tamanho do desenho em relacao ao modelo 3D (mantem o desenho proximo do hitbox). */
    private static final float MODEL_SCALE = 0.85f;

    private final ItemRenderer itemRenderer;
    private final ItemStack stack = new ItemStack(ArielUnit.MOTORCYCLE_ITEM);

    public MotorcycleRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
        this.shadowRadius = 0.7f;
    }

    @Override
    public void render(MotorcycleEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0, 0.5 * MODEL_SCALE, 0.0);   // apoia as rodas no chao
        matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - yaw));
        this.itemRenderer.renderItem(stack, ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV,
                matrices, vertexConsumers, entity.getWorld(), entity.getId());
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(MotorcycleEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
