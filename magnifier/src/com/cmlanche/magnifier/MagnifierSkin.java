package com.cmlanche.magnifier;

/**
 * MagnifierSkin.java
 * <p>
 * Copyright (c) 2011-2015, JFXtras
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the organization nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.SnapshotResult;
import javafx.scene.control.SkinBase;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Popup;
import javafx.util.Callback;

/**
 * Skin implementation for the {@link com.cmlanche.magnifier.Magnifier} control.
 *
 * @author SaiPradeepDandem
 */
public class MagnifierSkin extends SkinBase<Magnifier> {

    private Scene scene;
    private WritableImage writeImg;
    private static final double shift = 1.0D;
    private static final double MIN_RADIUS = 50.0D;
    private static final double RADIUS_DELTA = 5.0D;
    private static final double ZOOM_DELTA = 0.3D;
    private static final double ZOOM_MAX = 10.0D;
    private static final double ZOOM_MIN = 1.0D;
    private DoubleProperty localRadius = new SimpleDoubleProperty();
    private DoubleProperty localScaleFactor = new SimpleDoubleProperty();
    private DoubleProperty prevX = new SimpleDoubleProperty();
    private DoubleProperty prevY = new SimpleDoubleProperty();

    private Callback<SnapshotResult, Void> callBack;
    private SnapshotParameters param;
    private Viewer viewer;
    private int popUpShowSpace = 8;
    private boolean isNeedUpdateSnapshot = false;

    /**
     * Instantiates the skin implementation for {@link com.cmlanche.magnifier.Magnifier}.
     *
     * @param magnifier Instance of {@link com.cmlanche.magnifier.Magnifier} control.
     */
    public MagnifierSkin(Magnifier magnifier) {
        super(magnifier);
        initialize();
    }

    private void initialize() {
        final DoubleProperty frameWidthProperty = getSkinnable().frameWidthProperty();
        final DoubleProperty scopeLineWidthProperty = getSkinnable().scopeLineWidthProperty();
        final BooleanProperty scopeLinesVisibleProperty = getSkinnable().scopeLinesVisibleProperty();

        // Adding listener to control "radiusProperty" to add the value to "localRadius".
        getSkinnable().radiusProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable arg0) {
                localRadius.set(getSkinnable().getRadius());
            }
        });
        localRadius.set(getSkinnable().getRadius());

        // Adding listener to control "scaleFactorProperty" to add the value to "localScaleFactor".
        getSkinnable().scaleFactorProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable arg0) {
                localScaleFactor.set(getSkinnable().getScaleFactor());
            }
        });
        localScaleFactor.set(getSkinnable().getScaleFactor());

        callBack = new Callback<SnapshotResult, Void>() {
            @Override
            public Void call(SnapshotResult result) {
                return null;
            }
        };
        final Scale scale = new Scale();
        scale.xProperty().bind(localScaleFactor);
        scale.yProperty().bind(localScaleFactor);
        param = new SnapshotParameters();
        param.setDepthBuffer(true);
        param.setTransform(scale);

        final StackPane mainContent = new StackPane();
        final Circle frame = new Circle();
        frame.getStyleClass().add("magnifier-frame");
        frame.setFill(Color.BLACK);
        frame.radiusProperty().bind(localRadius.add(frameWidthProperty));

        final Circle cClip = new Circle();
        cClip.radiusProperty().bind(localRadius);
        cClip.translateXProperty().bind(localRadius);
        cClip.translateYProperty().bind(localRadius);
        cClip.setFill(Color.GREEN);

        viewer = new Viewer(localRadius, localRadius);
        viewer.setClip(cClip);

        final Line vL = new Line();
        vL.setStartX(0);
        vL.setStartY(0);
        vL.setEndX(0);
        vL.getStyleClass().add("magnifier-vLine");
        vL.strokeWidthProperty().bind(scopeLineWidthProperty);
        vL.visibleProperty().bind(scopeLinesVisibleProperty);
        vL.endYProperty().bind(localRadius.multiply(2));

        final Line hL = new Line();
        hL.setStartX(0);
        hL.setStartY(0);
        hL.setEndY(0);
        hL.getStyleClass().add("magnifier-hLine");
        hL.strokeWidthProperty().bind(scopeLineWidthProperty);
        hL.visibleProperty().bind(scopeLinesVisibleProperty);
        hL.endXProperty().bind(localRadius.multiply(2));

        // Adding all parts in a container.
        mainContent.getChildren().addAll(frame, viewer, vL, hL);

        final Popup popUp = new Popup();
        popUp.getContent().add(mainContent);

        Node content = getSkinnable().getContent();
        final EventHandler<MouseEvent> innerClickEvent = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                isNeedUpdateSnapshot = true;
                takeSnap(e.getX(), e.getY());
            }
        };

        final EventHandler<MouseEvent> enteredEvent = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                popUp.show(getSkinnable(), e.getScreenX() - shift + popUpShowSpace, e.getScreenY() - shift + popUpShowSpace);
                takeSnap(e.getX(), e.getY());
            }
        };
        final EventHandler<MouseEvent> exitedEvent = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                popUp.hide();
            }
        };
        final EventHandler<MouseEvent> movedEvent = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                final double r = localRadius.get();
                final double s = localScaleFactor.get();
                if (e.getSceneX() > (scene.getWidth() - (2 * r))) {
                    popUp.setX(e.getScreenX() - (2 * r) - shift + popUpShowSpace);
                } else {
                    popUp.setX(e.getScreenX() - shift + popUpShowSpace);
                }

                if (e.getSceneY() > (scene.getHeight() - (2 * r))) {
                    popUp.setY(e.getScreenY() - (2 * r) - shift + popUpShowSpace);
                } else {
                    popUp.setY(e.getScreenY() - shift + popUpShowSpace);
                }
//                System.out.println("x: " + popUp.getX() + "  y: " + popUp.getY());
                prevX.set(e.getX());
                prevY.set(e.getY());
                shiftViewerContent(prevX.get(), prevY.get(), r, s);
            }
        };

        // Adding mask implementation. The below mask is responsible to not access the contents when magnifier is shown.
        final StackPane mask = new StackPane();

        // Adding listener to activateProperty.
        getSkinnable().activeProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable arg0) {
                final Magnifier skinnable = getSkinnable();
                if (clickEvent != null) {
                    skinnable.addEventFilter(MouseEvent.MOUSE_PRESSED, clickEvent);
                }
                skinnable.addEventFilter(MouseEvent.MOUSE_PRESSED, innerClickEvent);
                if (skinnable.isActive()) {
                    skinnable.addEventFilter(MouseEvent.MOUSE_ENTERED, enteredEvent);
                    skinnable.addEventFilter(MouseEvent.MOUSE_EXITED, exitedEvent);
                    skinnable.addEventFilter(MouseEvent.MOUSE_MOVED, movedEvent);
                    if (!getChildren().contains(mask)) {
                        getChildren().add(mask);
                    }
                } else {
                    skinnable.removeEventFilter(MouseEvent.MOUSE_ENTERED, enteredEvent);
                    skinnable.removeEventFilter(MouseEvent.MOUSE_EXITED, exitedEvent);
                    skinnable.removeEventFilter(MouseEvent.MOUSE_MOVED, movedEvent);
                    if (getChildren().contains(mask)) {
                        getChildren().remove(mask);
                    }
                }
            }
        });
        final Magnifier skinnable = getSkinnable();
        if (getSkinnable().isActive()) {
            skinnable.addEventFilter(MouseEvent.MOUSE_ENTERED, enteredEvent);
            skinnable.addEventFilter(MouseEvent.MOUSE_EXITED, exitedEvent);
            skinnable.addEventFilter(MouseEvent.MOUSE_MOVED, movedEvent);
        }
        if (clickEvent != null) {
            skinnable.addEventFilter(MouseEvent.MOUSE_PRESSED, clickEvent);
        }
        skinnable.addEventFilter(MouseEvent.MOUSE_PRESSED, innerClickEvent);

        // Adding listener to contentProperty.
        getSkinnable().contentProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable arg0) {
                if (getSkinnable().getContent() != null) {
                    getChildren().clear();
                    getChildren().addAll(getSkinnable().getContent(), mask);
                }
            }
        });
        if (getSkinnable().getContent() != null) {
            getChildren().addAll(getSkinnable().getContent(), mask);
        }

        // Handling scroll behavior.
        mainContent.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent e) {
                // If scrolled with CTRL key press
                if (e.isControlDown() && getSkinnable().isScalableOnScroll()) {
                    double delta = e.getDeltaY();
                    double newValue = localScaleFactor.get();
                    if (delta > 0) { // Increasing the zoom.
                        newValue = newValue + ZOOM_DELTA;
                        if (newValue > ZOOM_MAX) {
                            newValue = ZOOM_MAX;
                        }
                    } else if (delta < 0) { // Decreasing the zoom.
                        newValue = newValue - ZOOM_DELTA;
                        if (newValue < ZOOM_MIN) {
                            newValue = ZOOM_MIN;
                        }
                    }
                    localScaleFactor.set(newValue);
                    takeSnap(prevX.get(), prevY.get());
                }
                // If scrolled with ALT key press
                else if (e.isAltDown() && getSkinnable().isResizableOnScroll()) {
                    final double delta = e.getDeltaY();
                    if (delta > 0) { // Increasing the size.
                        localRadius.set(localRadius.get() + RADIUS_DELTA);
                        shiftViewerContent(prevX.get(), prevY.get(), localRadius.get(), localScaleFactor.get());
                    } else if (delta < 0) { // Decreasing the size.
                        if (localRadius.get() > MIN_RADIUS) {
                            localRadius.set(localRadius.get() - RADIUS_DELTA);
                            shiftViewerContent(prevX.get(), prevY.get(), localRadius.get(), localScaleFactor.get());
                        }
                    }
                }
            }
        });

    }

    private EventHandler<MouseEvent> clickEvent;

    private void takeSnap(double x, double y) {
        if (writeImg == null || isNeedUpdateSnapshot) {
            isNeedUpdateSnapshot = false;
            int w = (int) (getSkinnable().getWidth() * localScaleFactor.get());
            int h = (int) (getSkinnable().getHeight() * localScaleFactor.get());
            writeImg = new WritableImage(w, h);

            // Get snapshot image
            getSkinnable().snapshot(callBack, param, writeImg);

            viewer.setContent(writeImg);
        }
        shiftViewerContent(x, y, localRadius.get(), localScaleFactor.get());
    }

    private void shiftViewerContent(double x, double y, double r, double s) {
        viewer.transXProperty().set((x * s) - r);
        viewer.transYProperty().set((y * s) - r);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void layoutChildren(double arg0, double arg1, double arg2, double arg3) {
        super.layoutChildren(arg0, arg1, arg2, arg3);
        if (this.scene == null) {
            this.scene = getNode().getScene();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double computeMinWidth(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4, double paramDouble5) {
        return getSkinnable().getMinWidth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double computeMinHeight(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4,
                                      double paramDouble5) {
        return getSkinnable().getMinHeight();
    }

    /**
     * Region(Viewer) that holds the clipped area of the scaled image.
     */
    private static class Viewer extends Region {
        private ImageView content;
        private final DoubleProperty width = new SimpleDoubleProperty();
        private final DoubleProperty height = new SimpleDoubleProperty();
        private final Rectangle clip;
        private final SimpleDoubleProperty transX = new SimpleDoubleProperty();
        private final SimpleDoubleProperty transY = new SimpleDoubleProperty();

        public Viewer(DoubleProperty w, DoubleProperty h) {
            this.width.bind(w.multiply(2));
            this.height.bind(h.multiply(2));
            this.clip = new Rectangle();
            this.clip.widthProperty().bind(this.width);
            this.clip.heightProperty().bind(this.height);
            this.clip.translateXProperty().bind(transX);
            this.clip.translateYProperty().bind(transY);
            content = new ImageView();
            getChildren().setAll(content);
        }

        public void setContent(WritableImage writableImage) {
            if (writableImage == null) {
                return;
            }
            this.content.translateXProperty().unbind();
            this.content.translateYProperty().unbind();
            this.content.setImage(writableImage);
            this.content.setClip(this.clip);
            this.content.translateXProperty().bind(transX.multiply(-1));
            this.content.translateYProperty().bind(transY.multiply(-1));
        }

        @Override
        protected double computeMinWidth(double d) {
            return width.get();
        }

        @Override
        protected double computeMinHeight(double d) {
            return height.get();
        }

        @Override
        protected double computePrefWidth(double d) {
            return width.get();
        }

        @Override
        protected double computePrefHeight(double d) {
            return height.get();
        }

        @Override
        protected double computeMaxWidth(double d) {
            return width.get();
        }

        @Override
        protected double computeMaxHeight(double d) {
            return height.get();
        }

        public SimpleDoubleProperty transXProperty() {
            return transX;
        }

        public SimpleDoubleProperty transYProperty() {
            return transY;
        }
    }
}