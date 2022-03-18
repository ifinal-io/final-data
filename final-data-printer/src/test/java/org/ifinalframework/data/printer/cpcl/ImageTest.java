package org.ifinalframework.data.printer.cpcl;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author ilikly
 * @version 1.2.4
 **/
public class ImageTest {

    /**
     * F0F0 1111 0000 1111 0000
     * F0F0 1111 0000 1111 0000
     * F0F0 1111 0000 1111 0000
     * F0F0 1111 0000 1111 0000
     * 0F0F 0000 1111 0000 1111
     * 0F0F 0000 1111 0000 1111
     * 0F0F 0000 1111 0000 1111
     * 0F0F 0000 1111 0000 1111
     * F0F0 1111 0000 1111 0000
     * F0F0 1111 0000 1111 0000
     * F0F0 1111 0000 1111 0000
     * F0F0 1111 0000 1111 0000
     * 0F0F 0000 1111 0000 1111
     * 0F0F 0000 1111 0000 1111
     * 0F0F 0000 1111 0000 1111
     * 0F0F 0000 1111 0000 1111
     */
    @Test
    @SneakyThrows
    void testImage(){
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_BINARY);

        int imageHeight = image.getHeight();
        int imageWidth  = image.getWidth();

        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {

                int imod = i / 4 % 2;
                int jmod = j / 4 % 2;

                if((imod == 0 && jmod == 0) || (imod == 1 && jmod == 1)){
                    image.setRGB(j,i,Color.BLACK.getRGB());
                }else {
                    image.setRGB(j,i,Color.WHITE.getRGB());
                }
            }
        }
        File output = new File("gray.jpg");
        ImageIO.write(image, "jpg", output);

        int width = imageWidth / 8 * 8;
        int height = imageHeight;

        StringBuilder sb = new StringBuilder();

        int gray = new Color(127,127,127).getRGB();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if(image.getRGB(j,i) > gray){
                    sb.append(0);
                }else {
                    sb.append(1);
                }
            }
        }

        String imageBytes = sb.toString();
        System.out.println("length=" + imageBytes.length());
        System.out.println(imageBytes);

        StringBuilder imageHex = new StringBuilder();
        for (int i = 0; i < imageBytes.length(); i+=4) {
            int value = Integer.parseInt(imageBytes.substring(i, i + 4), 2);
            String hex = Integer.toHexString(value);
            imageHex.append(hex);
        }

        String hexString = imageHex.toString().toUpperCase();
        System.out.println(hexString);


    }

    public static void main(String[] args) throws IOException {
        BufferedImage bi = ImageIO.read(new URL("https://images0.cnblogs.com/blog/614265/201408/241707553465767.png"));
//        BufferedImage bi = ImageIO.read(new URL("https://img0.baidu.com/it/u=1973394674,3179522103&fm=26&fmt=auto"));
//        BufferedImage bi = ImageIO.read(new URL("https://dmall.com/resource/public/pc/img/logo.png"));
        // 获取当前图片的高,宽,ARGB
        int w = bi.getWidth();
        int h = bi.getHeight();


        BufferedImage bufferedImage=new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);//  构造一个类型为预定义图像类型之一的 BufferedImage，TYPE_BYTE_BINARY（表示一个不透明的以字节打包的 1、2 或 4 位图像。）

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                bufferedImage.setRGB(j,i,bi.getRGB(j,i));
            }
        }

        int col = (w - 1) / 8 + 1;


        byte[] bytes = new byte[col * h];

        int index = 0;

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < col; j++) {
                Color color = new Color(bufferedImage.getRGB(j,i));

                if(color.getRed() > 0){
                    bytes[index++] = 1;
                }else {
                    bytes[index++] = 0;
                }

            }
        }
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(b);
        }
        String imageBytes = sb.toString();
        System.out.println("length=" + imageBytes.length()+", " + imageBytes);




        System.out.println(sb.toString());


//        for (int i = 0; i < h; i++) {
//            StringBuilder stringBuilder = new StringBuilder();
//
//            for (int j = 0; j < w; j++) {
//                int rgb = bufferedImage.getRGB(j, i);
//                if(new Color(rgb).getRed() > 127){
//                    stringBuilder.append("F");
//                }else {
//                    stringBuilder.append("0");
//                }
//            }
//
//
//            System.out.println(stringBuilder.toString());
//        }


        ImageIO.write(bufferedImage, "jpg", new File("new123.jpg"));
    }

}
