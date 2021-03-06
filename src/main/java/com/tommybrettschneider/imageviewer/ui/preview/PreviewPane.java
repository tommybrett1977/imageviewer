package com.tommybrettschneider.imageviewer.ui.preview;

import com.tommybrettschneider.imageviewer.util.PopupListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import static com.tommybrettschneider.imageviewer.ui.preview.ImageDisplayMode.*;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

public class PreviewPane extends JPanel implements IPreviewPane {

    private static final Logger LOGGER = Logger.getLogger(PreviewPane.class.getName()); 
    
    private static final String CLASSPATH_IMG_INPROGRESS = "/wait.gif";
    private static final String PROPERTY_DISPLAYMODE = "displayMode";
    
    private final IImageCache imageCache;
    private ImageIcon icon;
    private JScrollPane scrollpane;
    private JLabel label;
    private ImageDisplayMode displayMode = AUTORESIZE;
    private JPopupMenu contextMenu;
    private ExecutorService executorService;
    private Runnable refreshImageJob = new RefreshImageJob();

    public static PreviewPane getPreviewPane() {
        return PreviewPane.getPreviewPane(AUTORESIZE, true);
    }
    
    public static PreviewPane getPreviewPane(ImageDisplayMode imageDisplayMode, boolean enableDnd) {
        PreviewPane previewPane = new PreviewPane();
        previewPane.setDisplayMode(AUTORESIZE);
        previewPane.add(previewPane.getScrollpane(), BorderLayout.CENTER);
        previewPane.setBackground(Color.WHITE);
        previewPane.setDnDEnabled(enableDnd);
        return previewPane;
    }
    
    /**
     * Constructor.
     */
    public PreviewPane() {
        super(new BorderLayout(0, 0));
        setOpaque(true);
        imageCache = new ImageCache();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                LOGGER.info("previewpane resized");
                final BufferedImage currentImg = imageCache.getCurrentImage();
                if (getDisplayMode().equals(AUTORESIZE) && currentImg != null) {
                    final BufferedImage autoresizedImg = imageCache.proportionalScale(currentImg, AUTORESIZE);
                    if (autoresizedImg == null
                            || ((autoresizedImg.getHeight() > getHeight() || autoresizedImg.getWidth() != getWidth())
                            && (autoresizedImg.getWidth() > getWidth() || autoresizedImg.getHeight() != getHeight()))) {
                        imageCache.resetAutoscaleImage();
                        executorService.execute(refreshImageJob);
                    } else {
                        // use cached image...
                    }
                }
            }
        });
        addPropertyChangeListener((PropertyChangeEvent e) -> {
            if (e.getPropertyName().equals(PROPERTY_DISPLAYMODE) && imageCache.getCurrentImage() != null) {
                executorService.execute(refreshImageJob);
            }
        });
    }

    /**
     * Load source image from stream and display.
     *
     * @param is
     * @throws java.lang.Exception
     */
    @Override
    public void setSource(final InputStream is) throws Exception {
        imageCache.getImage(is);
        executorService.execute(refreshImageJob);
    }

    /**
     * Load source image from file and display.
     *
     * @param file
     * @throws java.lang.Exception
     */
    @Override
    public void setSource(final File file) throws Exception {
        setSource(file.toURI());
    }

    /**
     * Load source image from url and display.
     *
     * @param uri
     * @throws java.lang.Exception
     */
    @Override
    public void setSource(final URI uri) throws Exception {
        if (!imageCache.isCached(uri)) {
            imageCache.getImage(uri, new IIOReadProgressListener() {
                @Override
                public void imageComplete(ImageReader source) {
                }

                @Override
                public void imageProgress(ImageReader source, final float percentageDone) {
//                    SwingUtilities.invokeLater(new Runnable() {
//                       public void run() {
//                           progress.setValue((int)percentageDone);
//                           progress.validate();
//                           progress.revalidate();
//                           progress.invalidate();
//                           progress.repaint();
//                           
//                       } 
//                    });
                    //System.out.println(percentageDone);
                }

                @Override
                public void imageStarted(ImageReader source, int imageIndex) {
                }

                @Override
                public void readAborted(ImageReader source) {
                }

                @Override
                public void sequenceComplete(ImageReader source) {
                }

                @Override
                public void sequenceStarted(ImageReader source, int minIndex) {
                }

                @Override
                public void thumbnailComplete(ImageReader source) {
                }

                @Override
                public void thumbnailProgress(ImageReader source, float percentageDone) {
                }

                @Override
                public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
                }
            });
            executorService.execute(refreshImageJob);
        }
    }

    /* (non-Javadoc)
     * @see de.tb.ui.component.core.IPreviewPane#setDisplayMode(int)
     */
    @Override
    public void setDisplayMode(final ImageDisplayMode displayMode) {
        if (!this.displayMode.equals(displayMode)) {
            firePropertyChange(PROPERTY_DISPLAYMODE, this.displayMode, displayMode);
            this.displayMode = displayMode;

            if (getDisplayMode().equals(AUTORESIZE)) {
                scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            } else {
                scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollpane.setRequestFocusEnabled(true);
            }
        }
    }

    /**
     * Returns the context menu.
     *
     * @return JPopupMenu the context menu
     */
    protected JPopupMenu getContextMenu() {
        if (contextMenu == null) {
            contextMenu = new JPopupMenu();
            final ButtonGroup btnGroup = new ButtonGroup();
            for (ImageDisplayMode imageDisplayMode : ImageDisplayMode.values()) {
                JMenuItem item = new JCheckBoxMenuItem(new ScaleImageAction(imageDisplayMode));
                btnGroup.add(item);
                contextMenu.add(item);
            }
        }
        return contextMenu;
    }

    /**
     * @return Returns the displayMode.
     */
    @Override
    public ImageDisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Setter to enable or disable drag and drop capabilities for this
     * component.
     *
     * @param enableDnD
     */
    @Override
    public void setDnDEnabled(boolean enableDnD) {
        if (getTransferHandler() == null && enableDnD) {
            setTransferHandler(new PreviewableTransferHandler(this));
        } else if (getTransferHandler() != null && !enableDnD) {
            setTransferHandler(null);
        }
    }

    /**
     * Returns true if drag and drop capabilities are enabled, otherwise false.
     *
     * @return
     */
    @Override
    public boolean isDnDEnabled() {
        return getTransferHandler() != null;
    }

    /**
     *
     */
    private class ScaleImageAction extends AbstractAction {

        private final ImageDisplayMode displayMode;

        public ScaleImageAction(ImageDisplayMode displayMode) {
            super(displayMode.getLabel());
            this.displayMode = displayMode;
        }

        /* (non-Javadoc)
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            setDisplayMode(displayMode);
        }
    }

    @Override
    public void setSource(final BufferedImage img) {
        imageCache.setCurrentImage(img);
        executorService.execute(refreshImageJob);
    }

    private final class RefreshImageJob implements Runnable {

        private final Image waitImg = new ImageIcon(getClass().getResource(CLASSPATH_IMG_INPROGRESS)).getImage();
        private final Runnable preProcessRunnable = () -> {
            PreviewPane.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            icon.setImage(waitImg);
            if (isDnDEnabled() && StringUtils.isNotBlank(label.getText())) {
                label.setText(null);
            }
            label.invalidate();
            revalidate();
            repaint(250);
        };
        private final PostProcessRunnable postProcessRunnable = new PostProcessRunnable();

        private final class PostProcessRunnable implements Runnable {

            private BufferedImage bufImg;

            public void setNewImage(BufferedImage bufImg) {
                this.bufImg = bufImg;
            }

            @Override
            public void run() {
                icon.setImage(bufImg);
                label.invalidate();
                revalidate();
                repaint(250);
                PreviewPane.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        };

        @Override
        public void run() {
            preProcess();
            final BufferedImage bufImg = processImage();
            postProcess(bufImg);
            requestFocusInWindow();
        }

        private BufferedImage processImage() {
            final BufferedImage bufImg;
            if (displayMode.equals(AUTORESIZE)) {
                bufImg = imageCache.proportionalScale(imageCache.getCurrentImage(), PreviewPane.this);
            } else {
                bufImg = imageCache.proportionalScale(imageCache.getCurrentImage(), displayMode);
                if (scrollpane.isVisible()) {
                    scrollpane.requestFocusInWindow();
                }
            }
            return bufImg;
        }

        private void postProcess(final BufferedImage bufImg) {
            postProcessRunnable.setNewImage(bufImg);
            try {
                SwingUtilities.invokeAndWait(postProcessRunnable);
            } catch (InterruptedException | InvocationTargetException e) {
                // TODO
            }
        }

        private void preProcess() {
            try {
                SwingUtilities.invokeAndWait(preProcessRunnable);
            } catch (InterruptedException | InvocationTargetException e) {
                // TODO
            }
        }
    }

    protected JLabel getLabel() {
        if (label == null) {
            icon = new ImageIcon();
            label = new JLabel("Drop image", icon, JLabel.CENTER);
            label.setFont(new Font("Tahoma", Font.PLAIN, 11));
            label.addMouseListener(new PopupListener(getContextMenu()));
        }
        return label;
    }

    protected JScrollPane getScrollpane() {
        if (scrollpane == null) {
            scrollpane = new JScrollPane(getLabel());
            scrollpane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }
        return scrollpane;
    }

    @Override
    public void setBackground(Color color) {
        getScrollpane().getViewport().setBackground(color);
    }

    public void setExecutorService(ExecutorService exec) {
        this.executorService = exec;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }
}