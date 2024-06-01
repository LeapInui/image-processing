package leap;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Separator;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ImageProcessing extends Application {

    private Image originalImage;
    private ImageView imageView;
    private double[] gammaLUT = new double[256];
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("My Window");

        // Create an ImageView
        imageView = new ImageView();

        // Load the image from a file
        try {
            originalImage = new Image(new FileInputStream("imageprocessing\\src\\main\\resources\\leap\\sakura.jpg"));
            imageView.setImage(originalImage);
        } catch (FileNotFoundException e) {
            System.out.println(">>>The image could not be located in directory: " + System.getProperty("user.dir") + "<<<");
            System.exit(-1);
        }

        // Create a Slider for gamma correction
        Slider gammaSlider = new Slider(0.1, 3.0, 1.0);

        // Create a Label to display the current gamma value
        Label gammaLabel = new Label("Gamma: " + gammaSlider.getValue());

        // Add a listener to update the image with gamma correction when the slider is changed
        gammaSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double gammaValue = newValue.doubleValue();
            gammaLabel.setText("Gamma: " + gammaValue);

            setGameLUT(gammaValue);

            // Apply gamma correction to the image and update the ImageView
            Image correctedImage = applyGammaCorrection(originalImage, gammaValue);
            imageView.setImage(correctedImage);
        });

        Slider resizeSlider = new Slider(0.1, 2.0, 1.0);
        Label resizeLabel = new Label("Resize: " + resizeSlider.getValue());

        CheckBox nn = new CheckBox("Nearest Neighbour Interpolation");
        nn.setSelected(false);

        CheckBox cc = new CheckBox("Laplacian Cross Correlation");
        nn.setSelected(false);

        // Add a listener to update the image with resizing when the slider is changed or when checkbox is clicked
        ChangeListener<Object> listener = (observable, oldValue, newValue) -> {
            double resizeValue = resizeSlider.getValue();
            resizeLabel.setText("Resize: " + resizeValue);
        
            // Check if the change is the slider or the checkbox
            if (observable == resizeSlider.valueProperty() || observable == nn.selectedProperty()) {
                Image resizedImage = resize(originalImage, resizeValue, nn.isSelected());
                imageView.setImage(resizedImage);
            }
        };

        cc.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Image processedImage = applyCrossCorrelation(originalImage);
                imageView.setImage(processedImage);
            } else {
                imageView.setImage(originalImage);
            }
        });

        resizeSlider.valueProperty().addListener(listener);
        nn.selectedProperty().addListener(listener);

        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        Separator separator3 = new Separator();

        // Create a VBox to hold the components
        VBox vbox = new VBox(gammaSlider, gammaLabel, separator1, resizeSlider, resizeLabel, separator2, nn, cc, separator3, imageView);

        // Create a scene with the VBox
        Scene scene = new Scene(vbox, 400, 600);

        // Set the scene to the stage
        primaryStage.setScene(scene);

        // Show the stage
        primaryStage.show();
    }

    /*
     * 
     * Gamma Correction Function
     * 
     * 
     */
    private void setGameLUT(double gamma) {
        for (int i = 0; i < 256; i++) {
            gammaLUT[i] = Math.pow((double) i/255.0, 1.0 / gamma);
        }
    }

    private Image applyGammaCorrection(Image originalImage, double gamma) {
        int newWidth = (int) originalImage.getWidth();
        int newHeight = (int) originalImage.getHeight();
        
        // Create a new WritableImage
        javafx.scene.image.WritableImage gammaCorrectedImage = new javafx.scene.image.WritableImage(newWidth, newHeight);
        PixelWriter writeableimage = gammaCorrectedImage.getPixelWriter();

        Color colour;

        for (int j = 0; j < newHeight; j++) {
            for (int i = 0; i < newWidth; i++) {
                colour = originalImage.getPixelReader().getColor(i, j);
                colour = Color.color(gammaLUT[(int)(colour.getRed() * 255.0)], gammaLUT[(int)(colour.getGreen() * 255.0)], gammaLUT[(int)(colour.getBlue() * 255.0)]);
                writeableimage.setColor(i, j, colour);
            }
        }

        return gammaCorrectedImage;
    }


    /*
     * 
     * Interpolation Functions
     * 
     * 
     */
    private Color sample(double x, double y, PixelReader imageReader) {
        int ix = (int) x;
        int iy = (int) y;

        Color c0 = imageReader.getColor(ix, iy);
        Color c1 = imageReader.getColor(ix + 1, iy);
        Color c2 = imageReader.getColor(ix, iy + 1);
        Color c3 = imageReader.getColor(ix + 1, iy + 1);

        double dx = x - ix;
        double dy = y - iy;

        double red = bilinearCol(c0.getRed(), c1.getRed(), c2.getRed(), c3.getRed(), dx, dy);
        double green = bilinearCol(c0.getGreen(), c1.getGreen(), c2.getGreen(), c3.getGreen(), dx, dy);
        double blue = bilinearCol(c0.getBlue(), c1.getBlue(), c2.getBlue(), c3.getBlue(), dx, dy);

        return Color.color(red, green, blue);
    }

    private double bilinearCol(double c0, double c1, double c2, double c3, double dx, double dy) {
        double botHorizontal = lerp(c0, c1, dx);
        double topHorizontal = lerp(c2, c3, dx);
        double vertical = lerp(botHorizontal, topHorizontal, dy);

        return vertical;
    }

    private double lerp(double v1, double v2, double frac) {
        return v1 + (v2 - v1) * frac;
    }

    private Image resize(Image originalImage, double resizeScale, boolean nn) {
        int newWidth = (int) ((double)originalImage.getWidth() * resizeScale);
        int newHeight = (int) ((double)originalImage.getHeight() * resizeScale);
        
        // Create a new WritableImage
        javafx.scene.image.WritableImage resizedImage = new javafx.scene.image.WritableImage(newWidth, newHeight);
        PixelWriter writeableimage = resizedImage.getPixelWriter();

        Color colour;

        if (nn) {
            for (int j = 0; j < newHeight; j++) {
                for (int i = 0; i < newWidth; i++) {
                    double x = (double) originalImage.getWidth() * (double) i / (double) newWidth;
                    double y = (double) originalImage.getHeight() * (double) j / (double) newHeight;
                    int ix = (int) x;
                    int iy = (int) y;

                    colour = originalImage.getPixelReader().getColor(ix, iy);
                    writeableimage.setColor(i, j, colour);
                }
            }
        } else {
            for (int j = 0; j < newHeight; j++) {
                for (int i = 0; i < newWidth; i++) {
                    double owidth = originalImage.getWidth() - 1;
                    double oheight = originalImage.getHeight() - 1;
                    
                    if (newWidth > originalImage.getWidth() && newHeight > originalImage.getHeight()) {
                        owidth -= 1;
                        oheight -= 1;
                    } 

                    double x = owidth * (double) i / (double) newWidth;
                    double y = oheight * (double) j / (double) newHeight;

                    colour = sample(x, y, originalImage.getPixelReader());
                    writeableimage.setColor(i, j, colour);
                }
            }
        }
        

        return resizedImage;
    }


    /*
     * 
     * Cross Correlation Functions
     * 
     * 
     */

    private Image applyCrossCorrelation(Image originalImage) {
        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();

        // Create a new WritableImage
        javafx.scene.image.WritableImage processedImage = new javafx.scene.image.WritableImage(width, height);
        javafx.scene.image.PixelWriter writer = processedImage.getPixelWriter();
        javafx.scene.image.PixelReader reader = originalImage.getPixelReader();

        int[][] laplacianFilter = {
            {-4,-1, 0,-1,-4},
            {-1, 2, 3, 2,-1},
            { 0, 3, 4, 3, 0},
            {-1, 2, 3, 2,-1},
            {-4,-1, 0,-1,-4}
        };

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double[][][] resultBuffer = new double[width][height][3];

        // Apply Laplacian filter
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sumR = 0, sumG = 0, sumB = 0;
                for (int i = 0; i <= 4; i++) {
                    for (int j = 0; j <= 4; j++) {
                        if (x + i < width && y + j < height) {
                            Color color = reader.getColor(x + i, y + j);
                            sumR += color.getRed() * laplacianFilter[i][j];
                            sumG += color.getGreen() * laplacianFilter[i][j];
                            sumB += color.getBlue() * laplacianFilter[i][j];
                        }
                    }
                }

                resultBuffer[x][y][0] = sumR;
                resultBuffer[x][y][1] = sumG;
                resultBuffer[x][y][2] = sumB;
                
                min = Math.min(min, sumR);
                min = Math.min(min, sumG);
                min = Math.min(min, sumB);

                max = Math.max(max, sumR);
                max = Math.max(max, sumG);
                max = Math.max(max, sumB);
            }
        }
    
        // Normalisation
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double r = (resultBuffer[x][y][0] - min) / (max - min);
                double g = (resultBuffer[x][y][1] - min) / (max - min);
                double b = (resultBuffer[x][y][2] - min) / (max - min);

                writer.setColor(x, y, Color.color(r, g, b));
            }
        }
    
        return processedImage;
    }

}
