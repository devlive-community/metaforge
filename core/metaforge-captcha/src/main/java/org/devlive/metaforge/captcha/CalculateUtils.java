package org.devlive.metaforge.captcha;

import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.captcha.entity.ResultEntity;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
public class CalculateUtils
{
    // Image width
    private static final int WIDTH = 200;
    // Image height
    private static final int HEIGHT = 40;

    private CalculateUtils()
    {}

    /**
     * Generates a CaptchaResultEntity by creating and manipulating an image.
     *
     * @return The generated CaptchaResultEntity containing the image.
     */
    public static ResultEntity generate()
    {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics(); // Create a graphics class
        graphics.setColor(Color.WHITE); // The background color should be light
        graphics.fillRect(0, 0, WIDTH, HEIGHT); // Painted background
        ResultEntity entity = CaptchaUtils.generateCalculateImage();
        String[] result = entity.getExpression();
        int length = result.length; // The number of verification codes generated
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 22); // Bold sans-serif keeps every operator legible
        graphics.setFont(font); // Set font
        for (int i = 0; i < length; i++) {
            String string = String.valueOf(result[i]);
            // Dark randomized foreground stays readable on the light background
            graphics.setColor(CaptchaUtils.getForegroundColor());
            graphics.drawString(string, 30 * i + 30, 27);
        }
        graphics.dispose(); // Image takes effect
        String base64 = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
            ImageIO.write(image, "png", os);
            base64 = Base64.getEncoder().encodeToString(os.toByteArray());
        }
        catch (Exception exception) {
            log.error("Failed to generate image", exception);
        }
        entity.setImage(base64);
        return entity;
    }
}
