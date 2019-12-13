package com.maple.game.osee.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;

/**
 * 二维码相关工具
 *
 * @author Junlong
 */
public class QRCodeUtil {
    private static final String DEFAULT_IMAGE_FORMAT = "png"; // 二维码图片默认格式
    private static final int DEFAULT_WIDTH = 200; // 二维码默认宽
    private static final int DEFAULT_HEIGHT = 200; // 二维码默认高

    /**
     * 生成二维码图片数据
     *
     * @param content 二维码文本内容
     * @param width   二维码宽
     * @param height  二维码高
     */
    private static BufferedImage createQRCodeImage(String content, int width, int height) throws WriterException {
        HashMap<EncodeHintType, Object> hints = new HashMap<>();
        // 二维码纠错级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        // 二维码内容文本编码
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        // 二维码小块边距
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        int matrixWidth = bitMatrix.getWidth();
        int matrixHeight = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(matrixWidth, matrixHeight, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrixWidth; x++) {
            for (int y = 0; y < matrixHeight; y++) {
                // 填充图片颜色
                image.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return image;
    }

    /**
     * 生成Base64网络图片类型的二维码
     */
    public static String createQRCodeBase64(String content, int width, int height, String format, boolean head) throws WriterException, IOException {
        BufferedImage image = createQRCodeImage(content, width, height);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, outputStream);
        String base64String = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        if (head) {
            return String.format("data:image/%s;base64,%s", format, base64String);
        }
        return base64String;
    }

    /**
     * 生成Base64网络图片类型的二维码
     */
    public static String createQRCodeBase64(String content) throws WriterException, IOException {
        return createQRCodeBase64(content, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_IMAGE_FORMAT, false);
    }
}
