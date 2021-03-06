package com.tommybrettschneider.imageviewer.ui.thumbnail;

import com.tommybrettschneider.imageviewer.base.Directory;
import com.tommybrettschneider.imageviewer.base.Walkable;
import com.tommybrettschneider.imageviewer.base.ZipFile;
import com.tommybrettschneider.imageviewer.sort.SortMenu;
import com.tommybrettschneider.imageviewer.ui.preview.PreviewPane;
import com.tommybrettschneider.imageviewer.util.UIUtils;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.apache.commons.io.FileUtils;

public class ScrollableThumbnailPane extends JScrollPane {

    private static final Logger LOGGER = Logger.getLogger(ScrollableThumbnailPane.class.getName());
    private static final Point UPPERLEFTCORNER = new Point(0, 0);

    public static final int THUMB_MARGIN = 10;

    private ThumbnailPanel thumbnailPanel;
    private PreviewPane previewpane;
    private ExecutorService executorService;
    private Walkable walkable;
    private int thumbSize = 135;
    private final PropertyChangeSupport pcs;
    private final MouseAdapter mouseListener;

    
    /**
     * Constructor.
     */
    public ScrollableThumbnailPane() {
        super();
        getViewport().setBackground(Color.WHITE);
        getViewport().setMinimumSize(new Dimension(getThumbSize() + THUMB_MARGIN + THUMB_MARGIN, getThumbSize() + THUMB_MARGIN + THUMB_MARGIN));
        this.mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                getPanel().requestFocusInWindow();                
                final Thumbnail thumbnail = (Thumbnail)evt.getSource();                
                if (UIUtils.isDoubleClick(evt)) {
                    if (thumbnail instanceof FileThumbnail) {
                        File f = ((FileThumbnail) thumbnail).getSource();
                        Walkable walkable = Walkable.getInstance(f);
                        setSource(walkable);
                    }
                } else if (SwingUtilities.isRightMouseButton(evt)) {
                    JPopupMenu popup = new JPopupMenu();                   
                    JMenu nativeCommands = new JMenu("Native Cmd"); // Desktop integration...
                    if (thumbnail instanceof FileThumbnail && Desktop.isDesktopSupported()) {
                        final File selectedFile = ((FileThumbnail) thumbnail).getSource();
                        if (selectedFile.isFile()) {
                            JMenuItem nativeCmd = new JMenuItem("Open...");
                            nativeCmd.addActionListener((ActionEvent arg0) -> {
                                if (Desktop.isDesktopSupported()) {
                                    try {
                                        Desktop.getDesktop().open(selectedFile);
                                    } catch (IOException ex) {
                                        LOGGER.warning(ex.getMessage());
                                    }
                                }
                            });
                            nativeCommands.add(nativeCmd);                            
                            nativeCmd = new JMenuItem("Edit...");
                            nativeCmd.addActionListener((ActionEvent arg0) -> {
                                if (Desktop.isDesktopSupported()) {
                                    try {
                                        Desktop.getDesktop().edit(selectedFile);
                                    } catch (IOException ex) {
                                        LOGGER.warning(ex.getMessage());
                                    }
                                }
                            });
                            nativeCommands.add(nativeCmd);
                        }
                        popup.add(nativeCommands);
                        popup.addSeparator();
                    }                   
                    JMenuItem mi = new JMenuItem("Delete");
                    mi.addActionListener((ActionEvent arg0) -> {
                        final File selectedFile = ((FileThumbnail) thumbnail).getSource();
                        FileUtils.deleteQuietly(selectedFile);
                        getPanel().remove(thumbnail);
                        revalidate();
                    });
                    popup.add(mi);
                    mi = new JMenuItem("Refresh...");
                    mi.addActionListener((ActionEvent arg0) -> {
                        refresh();
                    });
                    popup.add(mi);
                    popup.show(thumbnail, evt.getX(), evt.getY());
                }        
                try {                   
                    if (UIUtils.isDoubleClick(evt)) {
                        // Bild öffnen über OS
                        if (Desktop.isDesktopSupported()) {                           
                            Desktop.getDesktop().open(((FileThumbnail)thumbnail).getSource());
                        }                        
                        final JFrame frame = new JFrame();
                        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
                        frame.setUndecorated(true);
                        frame.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
                        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                        gd.setFullScreenWindow(frame);                        
                        final Container parent = previewpane.getParent();                       
                        frame.getContentPane().setBackground(Color.BLACK);
                        frame.getContentPane().add(previewpane);
                        frame.setVisible(true);
                        previewpane.setFocusable(true);
                        previewpane.requestFocusInWindow();
                        previewpane.addKeyListener(new KeyAdapter() {
                            @Override
                            public void keyPressed(KeyEvent evt) {
                                if (UIUtils.isEscapePressed(evt)) {
                                    frame.setVisible(false);
                                    previewpane.removeKeyListener(this);
                                    parent.add(previewpane);
                                    frame.dispose();
                                }
                            }
                        });
                    }                  
                    if (thumbnail instanceof FileThumbnail) {
                        previewpane.setSource(((FileThumbnail) thumbnail).getSource());
                    } else if (thumbnail instanceof ZipEntryThumbnail) {
                        previewpane.setSource(((ZipEntryThumbnail) thumbnail).getInputStream());
                    }
                } catch (Exception ex) {
                    LOGGER.warning(ex.getMessage());
                }
            }
        };
        setBorder(null);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setRequestFocusEnabled(true);
        setAutoscrolls(true);
        thumbnailPanel = new ThumbnailPanel(this);
        ThumbnailSelection selection = new ThumbnailSelection(thumbnailPanel);
        getViewport().add(selection);
        thumbnailPanel.setLayout(new FlowLayout(FlowLayout.LEFT, THUMB_MARGIN, THUMB_MARGIN));
        selection.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                try {
                    FileThumbnail at = (FileThumbnail) evt.getSource();
                    setSource(Walkable.getInstance(at.getSource()));
                } catch (Exception ex) {
                    LOGGER.warning(ex.getMessage());                  
                }
                if (ScrollableThumbnailPane.this.isVisible()) {
                    ScrollableThumbnailPane.this.requestFocusInWindow();
                }
            }

            @Override
            public void mousePressed(MouseEvent evt) {
                if (SwingUtilities.isRightMouseButton(evt)) {
                    final JPopupMenu sortPopupMenu = new JPopupMenu();
                    final SortMenu sort = new SortMenu(ScrollableThumbnailPane.this);
                    sort.setSortableContainer(thumbnailPanel);
                    sortPopupMenu.add(sort);
                    sortPopupMenu.show(thumbnailPanel, evt.getX(), evt.getY());
                }
            }                      
        });
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                if ((getThumbSize() + THUMB_MARGIN + THUMB_MARGIN + getVerticalScrollBar().getWidth()) > getWidth()) {
                    setThumbSize(getWidth() - THUMB_MARGIN - THUMB_MARGIN - getVerticalScrollBar().getWidth());
                }
                thumbnailPanel.invalidate();
                thumbnailPanel.validate();
                thumbnailPanel.repaint();
            }
        });
        pcs = new PropertyChangeSupport(this);
    }

    
    public final ExecutorService getExecutorService() {
        return this.executorService;
    }

    public final void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
    
    public final JPanel getPanel() {
        return this.thumbnailPanel;
    }

    private void refresh() {
        setSource(getSource());
    }

    public final Walkable getSource() {
        return this.walkable;
    }

    public final void setSource(Walkable walkable) {
        if (walkable!=null) {
            this.walkable = walkable;
            getViewport().setViewPosition(UPPERLEFTCORNER);
            getPanel().removeAll();
            getPanel().revalidate();
            getPanel().repaint();
            
            if (walkable instanceof ZipFile) {
                walkable.getChildren().forEach(zipEntry -> {
                    Thumbnail t = new ZipEntryThumbnail((ZipEntry)zipEntry, executorService, (java.util.zip.ZipFile)walkable.getSource());
                    t.addMouseListener(mouseListener);                 
                    getPanel().add(t);
                });
            } else if (walkable instanceof Directory) {
                walkable.getChildren().forEach(file -> {
                    Thumbnail t = new FileThumbnail((File)file, executorService);
                    t.addMouseListener(mouseListener);
                    getPanel().add(t);
                });
            }
        }
    }

    public final void setPreviewpane(PreviewPane previewpane) {
        this.previewpane = previewpane;
    }

    public final int getThumbSize() {
        return this.thumbSize;
    }

    public final void setThumbSize(int thumbSize) {
        this.thumbSize = thumbSize;
        Thumbnail.getDimension().setSize(thumbSize, thumbSize);
        refresh();
    }
}