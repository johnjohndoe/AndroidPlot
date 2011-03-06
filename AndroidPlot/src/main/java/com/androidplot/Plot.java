/*
Copyright 2010 Nick Fellows. All rights reserved.

Redistribution and use in source and binary forms, without modification, are
permitted provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice, this list of
      conditions and the following disclaimer.

   2. Redistributions in binary form must reproduce the above copyright notice, this list
      of conditions and the following disclaimer in the documentation and/or other materials
      provided with the distribution.

THIS SOFTWARE IS PROVIDED BY Nick Fellows ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL NICK FELLOWS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those of the
authors and should not be interpreted as representing official policies, either expressed
or implied, of Nick Fellows.
*/

package com.androidplot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.androidplot.series.Series;
import com.androidplot.exception.PlotRenderException;
import com.androidplot.ui.widget.TextOrientationType;
import com.androidplot.ui.widget.TitleWidget;
import com.androidplot.ui.layout.*;
import com.androidplot.ui.widget.Widget;
import com.androidplot.ui.widget.formatter.Formatter;
import com.androidplot.ui.widget.renderer.DataRenderer;

import java.util.*;

/**
 * Base class for all other Plot implementations..
 */
public abstract class Plot<SeriesType extends Series, FormatterType extends Formatter, RendererType extends DataRenderer> extends View {

    //private Handler handler;
    //protected LayoutType layout;
    private static final int RECT_ADJUSTMENT = 1;
    protected String title;

    private BoxModel boxModel = new BoxModel(3, 3, 3, 3, 3, 3, 3, 3);

    //private RectF margins = new RectF(3, 3, 3, 3);

    /*private float plotMarginTop = 3;
    private float plotMarginBottom = 3;
    private float plotMarginLeft = 3;
    private float plotMarginRight = 3;

    private float plotPaddingTop = 3;
    private float plotPaddingBottom = 3;
    private float plotPaddingLeft = 3;
    private float plotPaddingRight = 3;*/
    private float borderRadiusX = 15;
    private float borderRadiusY = 15;
    private boolean drawBorderEnabled = true;
    private Paint borderPaint;
    private boolean drawBackgroundEnabled = true;
    private Paint backgroundPaint;
    private LayoutManager layoutManager;
    private TitleWidget titleWidget;
    private LinkedList<RendererType> renderers;
    private LinkedHashMap<Class, SeriesAndFormatterList<SeriesType,FormatterType>> seriesRegistry;

    private final ArrayList<PlotListener> listeners;
    //private Object drawMutex;

    {
        listeners = new ArrayList<PlotListener>();
        //drawMutex = new Object(); // TODO: lock on use
        seriesRegistry = new LinkedHashMap<Class, SeriesAndFormatterList<SeriesType,FormatterType>>();
        renderers = new LinkedList<RendererType>();
        //handler = new PlotUpdateHandler(this);
        borderPaint = new Paint();
        borderPaint.setColor(Color.rgb(150, 150, 150));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3.3f);
        borderPaint.setAntiAlias(true);
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.DKGRAY);
        backgroundPaint.setStyle(Paint.Style.FILL);

    }



    /**
     * Required by super-class. See android.view.View.
     * @param context Android Context used to doBeforeDraw this Plot.
     */
    public Plot(Context context, String title) {
        super(context);       
        this.title = title;
        postInit();
    }

    /**
     * Required by super-class. See android.view.View.
     * @param context
     * @param attrs
     */
    public Plot(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadAttrs(context, attrs);
        postInit();
        //context.o
    }

    /**
     * Required by super-class. See android.view.View.
     * @param context
     * @param attrs
     * @param defStyle
     */
    public Plot(Context context, AttributeSet attrs, int
            defStyle) {
        super(context, attrs, defStyle);
        loadAttrs(context, attrs);
        postInit();
    }

    private void postInit() {
        titleWidget = new TitleWidget(this, new SizeMetrics(25, SizeLayoutType.ABSOLUTE, 100, SizeLayoutType.ABSOLUTE), TextOrientationType.HORIZONTAL);
        layoutManager = new LayoutManager(this);
        layoutManager.position(titleWidget, 0, XLayoutStyle.RELATIVE_TO_CENTER, 0, YLayoutStyle.ABSOLUTE_FROM_TOP, AnchorPosition.TOP_MIDDLE);
    }

    private void loadAttrs(Context context, AttributeSet attrs) {

        this.title = attrs.getAttributeValue(null, "title");

        /* May be needed in the future if we ever switch to a resource-aware packaging scheme, such as .apk.
        TypedArray a = context.obtainStyledAttributes(attrs,
                com.androidplot.R.styleable.AndroidPlot);
        CharSequence s = a.getString(com.androidplot.R.styleable.AndroidPlot_title);
        if (s != null) {
            title = s.toString();
        }
        a.recycle();
        */
    }

    public boolean addListener(PlotListener listener) {
        synchronized (listeners) {
            if (listeners.contains(listener)) {
                return false;
            }
            return listeners.add(listener);
        }
    }

    public boolean removeListener(PlotListener listener) {
        synchronized(listeners) {
            return listeners.remove(listener);
        }
    }

    protected void notifyListeners(PlotEvent event) {
        synchronized (listeners) {
            for (PlotListener listener : listeners) {
                listener.onPlotUpdate(event);
            }
        }
    }

    /**
     * Convenience method - wraps LayoutManager.position().
     * @param element
     * @param x
     * @param xLayoutStyle
     * @param y
     * @param yLayoutStyle
     */
    public void position(Widget element, float x, XLayoutStyle xLayoutStyle, float y, YLayoutStyle yLayoutStyle) {
        layoutManager.position(element, x, xLayoutStyle, y, yLayoutStyle, AnchorPosition.LEFT_TOP);
    }

    /**
     * Convenience method - wraps LayoutManager.positon().
     * @param element
     * @param x
     * @param xLayoutStyle
     * @param y
     * @param yLayoutStyle
     * @param anchor
     */
    public void position(Widget element, float x, XLayoutStyle xLayoutStyle, float y, YLayoutStyle yLayoutStyle, AnchorPosition anchor) {
        layoutManager.position(element, x, xLayoutStyle, y, yLayoutStyle, anchor);
    }

    /**
     * Throw an instance of IllegalArgumentException if
     * cl is not a recognized RendererType.
     * @param cl
     * @return
     */
    protected RendererType getRendererInstance(Class cl) {
        RendererType rendererInstance = doGetRendererInstance(cl);
        if(rendererInstance == null) {
            throw new IllegalArgumentException("Unrecognized Renderer: " + cl.getCanonicalName());
        } else {
            return rendererInstance;
        }
    }

    /**
     * Return null or throw instance of IllegalArgumentException if clazz is not recognized
     * as a RendererType.
     * @param clazz
     * @return
     */
    protected abstract RendererType doGetRendererInstance(Class clazz);


    /**
     * Will *immediately* throw ClassCastException if rendererClass is not an instance of RendererType.
     * @param series
     * @param rendererClass
     */
    public boolean addSeries(SeriesType series, Class rendererClass, FormatterType formatter) {
        RendererType rt = null;
        rendererClass.cast(rt);
        SeriesAndFormatterList<SeriesType, FormatterType> sfList = seriesRegistry.get(rendererClass);
        
        // if there is no list for this renderer type, we need to getInstance one:
        if(sfList == null) {
            // if rendererClass is an invalid type, getInstance will throw an IllegalArgumentException:
            renderers.add(getRendererInstance(rendererClass));
            sfList = new SeriesAndFormatterList<SeriesType,FormatterType>();
            seriesRegistry.put(rendererClass, sfList);
        }

        // do nothing if this series already associated with the renderer:
        if(sfList.contains(series)) {
            return false;
        } else {
            sfList.add(series, formatter);
            return true;
        }
    }

    public boolean removeSeries(SeriesType series, Class rendererClass) {
        boolean result = seriesRegistry.get(rendererClass).remove(series);
        if(seriesRegistry.get(rendererClass).size() <= 0) {
            seriesRegistry.remove(rendererClass);
        }
        return result;
    }

    public void removeSeries(SeriesType series) {
        for(Class rendererClass : seriesRegistry.keySet()) {
            seriesRegistry.get(rendererClass).remove(series);
        }
        for(Iterator<SeriesAndFormatterList<SeriesType,FormatterType>> it = seriesRegistry.values().iterator(); it.hasNext();) {
            if(it.next().size() <= 0) {
                it.remove();
            }
        }
    }

    public boolean isEmpty() {
        return seriesRegistry.isEmpty();
    }




    public FormatterType getFormatter(SeriesType series, Class rendererClass) {
        return seriesRegistry.get(rendererClass).getFormatter(series);
        //throw new UnsupportedOperationException();
    }

    public boolean setFormatter(SeriesType series, Class rendererClass, FormatterType formatter) {
        throw new UnsupportedOperationException();
    }

  

    public SeriesAndFormatterList<SeriesType,FormatterType> getSeriesAndFormatterListForRenderer(Class rendererClass) {
        return seriesRegistry.get(rendererClass);
    }

    /**
     * Returns a list of all series assigned to the various renderers within the Plot.
     * The returned List will contain no duplicates.
     * @return
     */
    public Set<SeriesType> getSeriesSet() {
        Set<SeriesType> seriesSet = new LinkedHashSet<SeriesType>();
        for (DataRenderer renderer : getRendererList()) {
            for (SeriesType series : getSeriesListForRenderer(renderer.getClass())) {
                seriesSet.add(series);
            }
        }
        return seriesSet;
    }

    public List<SeriesType> getSeriesListForRenderer(Class rendererClass) {
        SeriesAndFormatterList<SeriesType,FormatterType> lst = seriesRegistry.get(rendererClass);
        if(lst == null) {
            return null;
        } else {
            return lst.getSeriesList();
        }
        //throw new UnsupportedOperationException();
    }

    public RendererType getRenderer(Class rendererClass) {
        for(RendererType renderer : renderers) {
            if(renderer.getClass() == rendererClass) {
                return renderer;
            }
        }
        return null;
    }

    public List<RendererType> getRendererList() {
        return renderers;
        //throw new UnsupportedOperationException();
    }

    public void disableAllMarkup() {
        this.layoutManager.disableAllMarkup();
    }

    /**
     * Causes the plot to be redrawn.  Should only be called by the main (UI) thead.
     * Non-UI threads should call postRedraw().
     * @throws InterruptedException
     */
    public void redraw() {
        //redraw(true);
        invalidate();
    }

    /**
     * Invalidates the Plot's underlying View and blocks until it is redrawn.
     * For a non-blocking version of this method use postRedraw(false).
     * This method should only be called from threads that are not the primary (UI) thread.
     * Use redraw() instead for calls made from the primary thread.
     * @throws InterruptedException
     */
    public void postRedraw() throws InterruptedException {
        postRedraw(true);
    }

    public void postRedraw(boolean isBlocking) throws InterruptedException {
        synchronized (this) {
            //handler.sendMessage(handler.obtainMessage());
            postInvalidate();
            if (isBlocking) {
                wait();
            }
        }
    }



    
    
    protected abstract void doBeforeDraw();
    protected abstract void doAfterDraw();

    /**
     * Called whenever the plot needs to be drawn via the Handler, which invokes invalidate().
     * Should never be called directly.
     * @param canvas
     */
    protected void onDraw(Canvas canvas) {

        doBeforeDraw();
        try {


            //lockDatasources();
            //doBeforeDraw();
            //layout.redraw();
            //RectF paddingRect = new RectF(plotPaddingLeft, plotPaddingTop, getWidth() - (plotPaddingRight + 1), getHeight() - (plotPaddingLeft + 1));

            RectF canvasRect = new RectF(0, 0, getWidth(), getHeight());
            RectF marginRect = boxModel.getMarginRect(canvasRect);
            RectF paddingRect = boxModel.getPaddingRect(canvasRect);
            if (drawBackgroundEnabled) {
                drawBackground(canvas, marginRect);
            }

            /*RectF marginRect = new RectF(paddingRect.left + margins.left
                    , paddingRect.top + margins.top, paddingRect.right - margins.right, paddingRect.bottom - margins.bottom);*/
            synchronized(this) {
                layoutManager.draw(canvas, canvasRect, marginRect, paddingRect);
            }
            if (drawBorderEnabled) {
                drawBorder(canvas, marginRect);
            }


        } catch (PlotRenderException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            doAfterDraw();
            notifyListeners(new PlotEvent(this, PlotEvent.Type.PLOT_REDRAWN));
            synchronized(this) {
                notify();
            }
        }
    }

    /**
     * Draws the plot's outer border.
     * @param canvas
     * @throws PlotRenderException
     */
    protected void drawBorder(Canvas canvas, RectF dims) throws PlotRenderException {

        canvas.drawRoundRect(dims, borderRadiusX, borderRadiusY, borderPaint);
    }

    protected void drawBackground(Canvas canvas, RectF dims) throws PlotRenderException {
        canvas.drawRoundRect(dims, borderRadiusX, borderRadiusY, backgroundPaint);
    }

    /**
     * Get the Handler used to manage display updates.  This method should not typically
     * be called by user code.
     * @return
     */
    //public Handler getHandler() {
    //    return handler;
    //}

    /**
     * Set the Handler used to manage display updates.  While this method should not typically
     * be called by user code, it can be used in advanced applications where overridden
     * display functionality is necessary.
     * @param handler
     */
    //public void setHandler(Handler handler) {
    //    this.handler = handler;
    //}

    /**
     *
     * @return The displayed title of this Plot.
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @param title  The title to display on this Plot.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    public LayoutManager getLayoutManager() {
        return layoutManager;
    }

    public void setLayoutManager(LayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    public boolean isDrawBorderEnabled() {
        return drawBorderEnabled;
    }

    public void setDrawBorderEnabled(boolean drawBorderEnabled) {
        this.drawBorderEnabled = drawBorderEnabled;
    }

    public TitleWidget getTitleWidget() {
        return titleWidget;
    }

    public void setTitleWidget(TitleWidget titleWidget) {
        this.titleWidget = titleWidget;
    }

    public Paint getBackgroundPaint() {
        return backgroundPaint;
    }

    public void setBackgroundPaint(Paint backgroundPaint) {
        this.backgroundPaint = backgroundPaint;
    }

    /**
     * Convenience method - wraps the individual setMarginXXX methods into a single method.
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void setPlotMargins(float left, float top, float right, float bottom) {
        setPlotMarginLeft(left);
        setPlotMarginTop(top);
        setPlotMarginRight(right);
        setPlotMarginBottom(bottom);
    }

    /**
     * Convenience method - wraps the individual setPaddingXXX methods into a single method.
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void setPlotPadding(float left, float top, float right, float bottom) {
        setPlotPaddingLeft(left);
        setPlotPaddingTop(top);
        setPlotPaddingRight(right);
        setPlotPaddingBottom(bottom);
    }

    public float getPlotMarginTop() {
        return boxModel.getMarginTop();
    }

    public void setPlotMarginTop(float plotMarginTop) {
        boxModel.setMarginTop(plotMarginTop);
    }

    public float getPlotMarginBottom() {
        return boxModel.getMarginBottom();
    }

    public void setPlotMarginBottom(float plotMarginBottom) {
        boxModel.setMarginBottom(plotMarginBottom);
    }

    public float getPlotMarginLeft() {
        return boxModel.getMarginLeft();
    }

    public void setPlotMarginLeft(float plotMarginLeft) {
        boxModel.setMarginLeft(plotMarginLeft);
    }

    public float getPlotMarginRight() {
        return boxModel.getMarginRight();
    }

    public void setPlotMarginRight(float plotMarginRight) {
        boxModel.setMarginRight(plotMarginRight);
    }

    /*public RectF getPadingRect(RectF marginRect) {
        //RectF marginRect = getMarginRect();
        return new RectF(marginRect.left + getPlotPaddingLeft(), marginRect.top+getPlotPaddingTop(), marginRect.right - getPlotPaddingRight(), marginRect.bottom - getPlotPaddingBottom());
    }*/

   /* public RectF getMarginRect(RectF canvasRect) {
        //return margins;
        //RectF paddedDims = getPadingRect();
        //return new RectF( canvasRect.left + getPlotMarginLeft() + RECT_ADJUSTMENT, canvasRect.top + getPlotMarginTop(), canvasRect.right - (getPlotMarginRight() + RECT_ADJUSTMENT), canvasRect.bottom - (getPlotMarginBottom() + RECT_ADJUSTMENT));
        return
    }*/

    public float getPlotPaddingTop() {
        return boxModel.getPaddingTop();
    }

    public void setPlotPaddingTop(float plotPaddingTop) {
        boxModel.setPaddingTop(plotPaddingTop);
    }

    public float getPlotPaddingBottom() {
        return boxModel.getPaddingBottom();
    }

    public void setPlotPaddingBottom(float plotPaddingBottom) {
        boxModel.setPaddingBottom(plotPaddingBottom);
    }

    public float getPlotPaddingLeft() {
        return boxModel.getPaddingLeft();
    }

    public void setPlotPaddingLeft(float plotPaddingLeft) {
        boxModel.setPaddingLeft(plotPaddingLeft);
    }

    public float getPlotPaddingRight() {
        return boxModel.getPaddingRight();
    }

    public void setPlotPaddingRight(float plotPaddingRight) {
        boxModel.setPaddingRight(plotPaddingRight);
    }
}