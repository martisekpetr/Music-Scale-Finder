package chordAnalyzer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Customized JPanel for displaying piano keyboard with keys highlighted accordingly to currently selected scale. Redefines the <code>paintComponent</code> method
 * to paint a piano claviature from the file <code>piano.jpg</code> on the background and graphic representation of a given scale on the front. A scale is represented
 * by highlighting the correct keys on a claviature with color, using the <code>fillPolygon</code> method of a <code>Graphics</code> class. Coordinates of the polygon are
 * specific for each key on a piano, because they have different shapes. Used claviature has three octaves, so the painting of a polygon is repeated three times
 * with a proper shift.
 *  */
public class PianoPanel extends JPanel {

        private BufferedImage _image;
        private WeightedScale _scale;

    /**
     * Constructor. Loads image 'piano.jpg' which is painted on the panel and
     * sets dimensions of the panel to hold the image.
     *
     * @throws IOException
     */
        public PianoPanel() throws IOException {
            _image = ImageIO.read(new File("piano.jpg"));

            this.setPreferredSize(new Dimension(_image.getWidth(), _image.getHeight()));
            this.setMinimumSize(new Dimension(_image.getWidth(), _image.getHeight()));
        }

        /**
         * Updates the panel to display currently selected scale.
         * 
         * @param wscale currently selected scale
         */
        public void setScale(WeightedScale wscale) {
            _scale = wscale;
            this.repaint();
        }

        /**
     * Highlights specific tone in all his occurencies on a keyboard.
     *
     * @param t tone to be highlighted
     * @param g Graphics object to be drawn to
     */
        private void paintTone(Tone t, Graphics g) {
            //arrays for storing the points of a rectangle
            int[] intsx;
            int[] intsy;
            int pointCount;
            //coordinates for a label displaying current tone
            int[] labelXY;
            
            //for each tone we specify an area on first octave on a keyboard, which belongs to the tone
            switch (t) {
                case C:
                    intsx = new int[]{2, 2, 35, 35, 22, 22};
                    intsy = new int[]{2, _image.getHeight() - 2, _image.getHeight() - 2, 95, 95, 2};
                    pointCount = 6;
                    labelXY = new int[]{14, 140};
                    break;
                case Cis:
                    intsx = new int[]{24, 24, 46, 46};
                    intsy = new int[]{2, 93, 93, 2};
                    pointCount = 4;
                    labelXY = new int[]{25, 75};
                    break;
                case D:
                    intsx = new int[]{48, 48, 37, 37, 70, 70, 57, 57};
                    intsy = new int[]{2, 95, 95, _image.getHeight() - 2, _image.getHeight() - 2, 95, 95, 2};
                    pointCount = 8;
                    labelXY = new int[]{49, 140};
                    break;

                case Dis:
                    intsx = new int[]{59, 59, 81, 81};
                    intsy = new int[]{2, 93, 93, 2};
                    pointCount = 4;
                    labelXY = new int[]{60, 75};
                    break;
                case E:
                    intsx = new int[]{83, 83, 72, 72, 105, 105};
                    intsy = new int[]{2, 95, 95, _image.getHeight() - 2, _image.getHeight() - 2, 2};
                    pointCount = 6;
                    labelXY = new int[]{84, 140};
                    break;
                case F:
                    intsx = new int[]{107, 107, 140, 140, 127, 127};
                    intsy = new int[]{2, _image.getHeight() - 2, _image.getHeight() - 2, 95, 95, 2};
                    pointCount = 6;
                    labelXY = new int[]{119, 140};
                    break;
                case Fis:
                    intsx = new int[]{129, 129, 151, 151};
                    intsy = new int[]{2, 93, 93, 2};
                    pointCount = 4;
                    labelXY = new int[]{130, 75};
                    break;
                case G:
                    intsx = new int[]{153, 153, 142, 142, 175, 175, 162, 162};
                    intsy = new int[]{2, 95, 95, _image.getHeight() - 2, _image.getHeight() - 2, 95, 95, 2};
                    pointCount = 8;
                    labelXY = new int[]{154, 140};
                    break;
                case Gis:
                    intsx = new int[]{164, 164, 186, 186};
                    intsy = new int[]{2, 93, 93, 2};
                    pointCount = 4;
                    labelXY = new int[]{165, 75};
                    break;
                case A:
                    intsx = new int[]{188, 188, 177, 177, 210, 210, 197, 197};
                    intsy = new int[]{2, 95, 95, _image.getHeight() - 2, _image.getHeight() - 2, 95, 95, 2};
                    pointCount = 8;
                    labelXY = new int[]{189, 140};
                    break;
                case Bb:
                    intsx = new int[]{199, 199, 221, 221};
                    intsy = new int[]{2, 93, 93, 2};
                    pointCount = 4;
                    labelXY = new int[]{200, 75};
                    break;
                case B:
                    intsx = new int[]{223, 223, 212, 212, 245, 245};
                    intsy = new int[]{2, 95, 95, _image.getHeight() - 2, _image.getHeight() - 2, 2};
                    pointCount = 6;
                    labelXY = new int[]{224, 140};
                    break;

                default:
                    intsx = new int[]{};
                    intsy = new int[]{};
                    pointCount = 0;
                    labelXY = new int[]{0, 0};
                    break;

            }

            //highlights the tone in all three octaves that are displayed on the keyboard
            for (int k = 0; k < 3; k++) {
                g.setColor(new Color(255, 255, 100, 255));
                
                //root is distinguished with different color
                if (t.equals(_scale.getRoot())) {
                    g.setColor(new Color(250, 150, 50, 255));
                }
                //x-coordinates are shifted three times to span across the keyboard
                int[] tmpIntsx = new int[intsx.length];
                for (int i = 0; i < intsx.length; i++) {
                    tmpIntsx[i] = intsx[i] + k * ((_image.getWidth() - 2) / 3);
                }
                //highlighting a key and drawing a name of underlying tone
                g.fillPolygon(tmpIntsx, intsy, pointCount);
                g.setFont(new Font("arial", Font.BOLD, 15));
                g.setColor(Color.black);
                g.drawString(t.toString(), labelXY[0] + k * ((_image.getWidth() - 2) / 3), labelXY[1]);
            }
        }

        /**
         * Paints component itself, piano keyboard from image 'piano.jpg' and highlights all 
         * the tones from currently displayed scale.
         * 
         * @param g Graphics object to be drawn to.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(_image, 0, 0, null);

            if (_scale != null) {
                for (Tone t : _scale.getTones()) {
                    paintTone(t, g);
                }
            }
        }
    }

