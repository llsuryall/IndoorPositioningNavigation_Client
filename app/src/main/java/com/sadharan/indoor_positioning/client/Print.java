package com.sadharan.indoor_positioning.client;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.graphics.Color;
import android.view.GestureDetector;
import android.view.View;
import android.graphics.Matrix;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.annotation.Nullable;

import java.util.*;

class Pair implements Comparator<Pair> {
    int i, j;
    double w = Integer.MAX_VALUE;

    Pair() {
    }

    Pair(int i, int j) {
        this.i = i;
        this.j = j;
    }

    Pair(int i, int j, double w) {
        this.i = i;
        this.j = j;
        this.w = w;
    }

    double getW() {
        return this.w;
    }

    int geti() {
        return this.i;
    }

    int getj() {
        return this.j;
    }

    @Override
    public int compare(Pair n1, Pair n2) {
        if (n1.w < n2.w)
            return -1;
        else if (n1.w > n2.w)
            return 1;
        return 0;
    }
}

public class Print extends View implements View.OnTouchListener{
    private Cell[][] cells;
    private  static final int COLS=642,ROWS=258;
    private static final float WALLTHICKNESS=8;
    private float cellheight,cellwidth,hmargin,vmargin;
    private Paint wallPaint;
    private TextPaint textPaint;
    private int BackgroundColor=0XFFFDFDFD;
    private int StrokeColor=0XFFE1DEDE;
    private int pathColor=0XFF2A8DB9;
    private int roomColor=0XFFEBEBEB;
    private int shadow=0XFF7B7A72;
    private int destinationColor=0XFFF34F4F;
    private Random random;
    private ArrayList<ArrayList<Integer>> allRooms=new ArrayList<ArrayList<Integer>>();
    private ArrayList<String> roomNames=new ArrayList<String>();
    private Pair vis[][] = new Pair[ROWS][COLS];

    //    zoom variables
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetection;
    private float mScaleFactor = 1.f;
    private float pScaleFactor=1.f;
    private float mPosX,pivotPointX;
    private float mPosY,pivotPointY;
    private float mLastTouchX;
    private float mLastTouchY;
    private float startXLimit;
    private float startYLimit;
    private float canvasWidth;
    private float canvasHeight;
    private float endXLimit;
    private float endYLimit;
    private static final int INVALID_POINTER_ID = -1;
    private int mActivePointerId = INVALID_POINTER_ID;

    private Bitmap sbitmap;
//    Path variables
    private Pair source;
    private Pair destination;
    private ArrayList<Pair> path=new ArrayList<>();

//    0 for no action -1 for destination 1 for source
    private int loc=0;

    public void setLoc(int val){
        this.loc=val;
    }

    public Print(Context context,@Nullable AttributeSet attrs){
        super(context,attrs);

        wallPaint=new Paint();
        wallPaint.setColor(StrokeColor);
        wallPaint.setStrokeWidth(WALLTHICKNESS);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetection=new GestureDetector(context, new gestureDetect());
        createMaze();
        destination=new Pair(90,20);
        source=new Pair(212,350);

    }

    @Override
    protected  void onDraw(Canvas canvas){
        canvas.drawColor(BackgroundColor);
        int width=getWidth();
        canvasWidth=canvas.getWidth();
        canvasHeight=canvas.getHeight();
        int height=getHeight();
        cellwidth= (float) (width)/(COLS);
        cellheight = (float) (height)/(ROWS);
        canvas.save();
        canvas.translate(mPosX, mPosY);
        Matrix matrix = new Matrix();

        float newScaleFactor=pScaleFactor+0.09f;
        if(newScaleFactor<mScaleFactor){
            matrix.postScale(newScaleFactor, newScaleFactor,pivotPointX,pivotPointY);
            canvas.concat(matrix);

//        canvas.drawRect();
            startXLimit=getTranformedValue(0f,0f).get(0);
            startYLimit=getTranformedValue(0f,0f).get(1);

            for(int i=0;i<ROWS;i++){
                for(int j=0;j<COLS;j++){
                    if(cells[i][j].topWall){
                        canvas.drawLine(j*cellwidth ,i*cellheight,(j+1)*cellwidth,i*cellheight,wallPaint);
                    }
                    if(cells[i][j].bottomWall){
                        canvas.drawLine(j*cellwidth ,(i+1)*cellheight,(j+1)*cellwidth,(i+1)*cellheight,wallPaint);
                    }
                    if(cells[i][j].leftWall){
                        canvas.drawLine(j*cellwidth,i*cellheight,(j)*cellwidth,(i+1)*cellheight,wallPaint);
                    }
                    if(cells[i][j].rightWall){
                        canvas.drawLine((j+1)*cellwidth ,(i)*cellheight,(j+1)*cellwidth,(i+1)*cellheight,wallPaint);
                    }
                }
            }

            for(int j=0;j<allRooms.size();j++){
                Paint rectPaint=new Paint();
                rectPaint.setStyle(Paint.Style.FILL);
                rectPaint.setColor(roomColor);
                ArrayList<Integer> curRoom=allRooms.get(j);
                canvas.drawRect(curRoom.get(2)*cellwidth, curRoom.get(0)*cellheight, curRoom.get(3)*cellwidth, (curRoom.get(1)+1)*cellheight, rectPaint);

                rectPaint.setStyle(Paint.Style.STROKE);
                rectPaint.setColor(StrokeColor);
                rectPaint.setStrokeWidth(WALLTHICKNESS);
                canvas.drawRect(curRoom.get(2)*cellwidth, curRoom.get(0)*cellheight, curRoom.get(3)*cellwidth, (curRoom.get(1)+1)*cellheight, rectPaint);

                int xStart=(int)(((curRoom.get(2)+curRoom.get(3))/2)*cellwidth);
                int yStart=(int)(((curRoom.get(0)+curRoom.get(1))/2)*cellheight);
                rectPaint.setColor(Color.GRAY);
                rectPaint.setTextSize(30);
                rectPaint.setTextAlign(Paint.Align.CENTER);
                rectPaint.setStyle(Paint.Style.FILL);
                canvas.drawText(roomNames.get(j),xStart,yStart,rectPaint);
            }

//        pointing src
            wallPaint.setStrokeWidth(7);
            wallPaint.setColor(pathColor);
            canvas.drawCircle(source.j*cellwidth+cellwidth/2,source.i*cellheight+cellheight/2,10f,wallPaint);

            //        Draw source icon
            path=PrintPath(source.i,source.j,destination.i,destination.j);
            Drawable SourceIcon = getResources().getDrawable(R.drawable.ic_baseline_add_location_24,null);
            float aspectRatio = (float)SourceIcon.getIntrinsicWidth()/SourceIcon.getIntrinsicHeight();
            int desiredWidthInPx = 80;
            int derivedHeightInPx = (int)(desiredWidthInPx / aspectRatio);
            int currentXposition= (int) (source.j*cellwidth+cellwidth/2)-desiredWidthInPx/2;
            int currentYposition=(int)(source.i*cellheight+cellheight/2)-derivedHeightInPx;
            SourceIcon.setBounds(currentXposition,currentYposition,currentXposition+desiredWidthInPx,currentYposition+derivedHeightInPx);
            SourceIcon.draw(canvas);

            //        Draw destination icon
            Drawable destinationIcon = getResources().getDrawable(R.drawable.ic_baseline_add,null);
            aspectRatio = (float)destinationIcon.getIntrinsicWidth()/destinationIcon.getIntrinsicHeight();
            derivedHeightInPx = (int)(desiredWidthInPx / aspectRatio);
            currentXposition= (int) (destination.j*cellwidth+cellwidth/2)-desiredWidthInPx/2;
            currentYposition=(int)(destination.i*cellheight+cellheight/2)-derivedHeightInPx;
            destinationIcon.setBounds(currentXposition,currentYposition,currentXposition+desiredWidthInPx,currentYposition+derivedHeightInPx);
            destinationIcon.draw(canvas);

            wallPaint.setStyle(Paint.Style.FILL);
            for(int k=path.size()-1;k>0;k--){
//            ArrayList<Float> src=new ArrayList<Float>(getTranformedValue((float) path.get(k).i,(float) path.get(k).j+cellheight/2));
//            ArrayList<Float> destination=new ArrayList<>(getTranformedValue((float) path.get(k-1).i+cellwidth/2,(float) path.get(k-1).j+cellheight/2));
                int srcx=path.get(k).i;
                int srcy=path.get(k).j;
                int destx=path.get(k-1).i;
                int desty=path.get(k-1).j;

                canvas.drawLine(srcy*cellwidth+cellwidth/2,srcx*cellheight+cellheight/2,desty*cellwidth+cellwidth/2,destx*cellheight+cellheight/2,wallPaint);
            }
//        wallPaint.setColor();

            wallPaint.setStrokeWidth(7);
            wallPaint.setColor(destinationColor);
            canvas.drawCircle(destination.j*cellwidth+cellwidth/2,destination.i*cellheight+cellheight/2,10f,wallPaint);
            wallPaint.setStrokeWidth(WALLTHICKNESS);
            wallPaint.setColor(StrokeColor);
            canvas.restore();
            pScaleFactor=newScaleFactor;
            invalidate();
        }else{
        matrix.postScale(mScaleFactor, mScaleFactor,pivotPointX,pivotPointY);
        canvas.concat(matrix);

//        canvas.drawRect();
        startXLimit=getTranformedValue(0f,0f).get(0);
        startYLimit=getTranformedValue(0f,0f).get(1);

        for(int i=0;i<ROWS;i++){
            for(int j=0;j<COLS;j++){
                if(cells[i][j].topWall){
                    canvas.drawLine(j*cellwidth ,i*cellheight,(j+1)*cellwidth,i*cellheight,wallPaint);
                }
                if(cells[i][j].bottomWall){
                    canvas.drawLine(j*cellwidth ,(i+1)*cellheight,(j+1)*cellwidth,(i+1)*cellheight,wallPaint);
                }
                if(cells[i][j].leftWall){
                    canvas.drawLine(j*cellwidth,i*cellheight,(j)*cellwidth,(i+1)*cellheight,wallPaint);
                }
                if(cells[i][j].rightWall){
                    canvas.drawLine((j+1)*cellwidth ,(i)*cellheight,(j+1)*cellwidth,(i+1)*cellheight,wallPaint);
                }
            }
        }

        for(int j=0;j<allRooms.size();j++){
            Paint rectPaint=new Paint();
            rectPaint.setStyle(Paint.Style.FILL);
            rectPaint.setColor(roomColor);
            ArrayList<Integer> curRoom=allRooms.get(j);
            canvas.drawRect(curRoom.get(2)*cellwidth, curRoom.get(0)*cellheight, curRoom.get(3)*cellwidth, (curRoom.get(1)+1)*cellheight, rectPaint);

            rectPaint.setStyle(Paint.Style.STROKE);
            rectPaint.setColor(StrokeColor);
            rectPaint.setStrokeWidth(WALLTHICKNESS);
            canvas.drawRect(curRoom.get(2)*cellwidth, curRoom.get(0)*cellheight, curRoom.get(3)*cellwidth, (curRoom.get(1)+1)*cellheight, rectPaint);

            int xStart=(int)(((curRoom.get(2)+curRoom.get(3))/2)*cellwidth);
            int yStart=(int)(((curRoom.get(0)+curRoom.get(1))/2)*cellheight);
            rectPaint.setColor(Color.GRAY);
            rectPaint.setTextSize(30);
            rectPaint.setTextAlign(Paint.Align.CENTER);
            rectPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(roomNames.get(j),xStart,yStart,rectPaint);
        }

//        pointing src
        wallPaint.setStrokeWidth(7);
        wallPaint.setColor(pathColor);
        canvas.drawCircle(source.j*cellwidth+cellwidth/2,source.i*cellheight+cellheight/2,10f,wallPaint);

//        Draw source icon
            path=PrintPath(source.i,source.j,destination.i,destination.j);
            Drawable icon = getResources().getDrawable(R.drawable.ic_baseline_add_location_24,null);
            float aspectRatio = (float)icon.getIntrinsicWidth()/icon.getIntrinsicHeight();
            int desiredWidthInPx = 80;
            int derivedHeightInPx = (int)(desiredWidthInPx / aspectRatio);
            int currentXposition= (int) (source.j*cellwidth+cellwidth/2)-desiredWidthInPx/2;
            int currentYposition=(int)(source.i*cellheight+cellheight/2)-derivedHeightInPx;
            icon.setBounds(currentXposition,currentYposition,currentXposition+desiredWidthInPx,currentYposition+derivedHeightInPx);
            icon.draw(canvas);

            //        Draw destination icon
            Drawable destinationIcon = getResources().getDrawable(R.drawable.ic_baseline_add,null);
            aspectRatio = (float)destinationIcon.getIntrinsicWidth()/destinationIcon.getIntrinsicHeight();
            derivedHeightInPx = (int)(desiredWidthInPx / aspectRatio);
            currentXposition= (int) (destination.j*cellwidth+cellwidth/2)-desiredWidthInPx/2;
            currentYposition=(int)(destination.i*cellheight+cellheight/2)-derivedHeightInPx;
            destinationIcon.setBounds(currentXposition,currentYposition,currentXposition+desiredWidthInPx,currentYposition+derivedHeightInPx);
            destinationIcon.draw(canvas);

        wallPaint.setStyle(Paint.Style.FILL);
        for(int k=path.size()-1;k>0;k--){
//            ArrayList<Float> src=new ArrayList<Float>(getTranformedValue((float) path.get(k).i,(float) path.get(k).j+cellheight/2));
//            ArrayList<Float> destination=new ArrayList<>(getTranformedValue((float) path.get(k-1).i+cellwidth/2,(float) path.get(k-1).j+cellheight/2));
            int srcx=path.get(k).i;
            int srcy=path.get(k).j;
            int destx=path.get(k-1).i;
            int desty=path.get(k-1).j;

            canvas.drawLine(srcy*cellwidth+cellwidth/2,srcx*cellheight+cellheight/2,desty*cellwidth+cellwidth/2,destx*cellheight+cellheight/2,wallPaint);
        }
//        wallPaint.setColor();

        wallPaint.setStrokeWidth(7);
        wallPaint.setColor(destinationColor);
        canvas.drawCircle(destination.j*cellwidth+cellwidth/2,destination.i*cellheight+cellheight/2,10f,wallPaint);
        wallPaint.setStrokeWidth(WALLTHICKNESS);
        wallPaint.setColor(StrokeColor);
        canvas.restore();
            pScaleFactor=newScaleFactor;
        }
    }

    private ArrayList<Float> getTranformedValue(float x,float y){

//        After scale
        float newX=mScaleFactor*pivotPointX;
        float newY=mScaleFactor*pivotPointY;

//        After translate
        newX=newX-pivotPointX-x*mScaleFactor;
        newY=newY-pivotPointY-y*mScaleFactor;
        ArrayList<Float> arr=new ArrayList<>();
        arr.add(newX);
        arr.add(newY);
        return arr;
    }

    private ArrayList<Float> getInverseTransformedvalue(Float x,Float y){
        float newX=mScaleFactor*pivotPointX-pivotPointX-x;
        float newY=mScaleFactor*pivotPointY-pivotPointY-y;

        newX=newX/mScaleFactor;
        newY=newY/mScaleFactor;

        ArrayList<Float> arr=new ArrayList<>();
        arr.add(newX);
        arr.add(newY);
        return arr;
    }

    //    Scale class for zoom in and out purpose

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            // Don't let the object get too small or too large.
            pivotPointX = detector.getFocusX();
            pivotPointY = detector.getFocusY();
            pScaleFactor=mScaleFactor;
            mScaleFactor = Math.max(1f, Math.min(mScaleFactor, 5.0f));
            invalidate();
            return true;
        }
    }

    private class gestureDetect extends GestureDetector.SimpleOnGestureListener{
        public boolean onDoubleTap(MotionEvent motionEvent) {
                pScaleFactor=mScaleFactor;
                mScaleFactor=Math.min(mScaleFactor+0.4f,5.0f);
                pivotPointY=motionEvent.getY();
                pivotPointX=motionEvent.getX();
                invalidate();
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if(loc==-1){
                float newX=startXLimit-mPosX+e.getX();
                float newY=startYLimit-mPosY+e.getY();
                newX=newX/mScaleFactor;
                newY=newY/mScaleFactor;
                int xcell=(int)(newX/cellwidth);
                int ycell=(int)(newY/cellheight);
                destination=new Pair(ycell,xcell);
                setLoc(0);
                invalidate();
            }else if(loc==1){
                float newX=startXLimit-mPosX+e.getX();
                float newY=startYLimit-mPosY+e.getY();
                newX=newX/mScaleFactor;
                newY=newY/mScaleFactor;
                int xcell=(int)(newX/cellwidth);
                int ycell=(int)(newY/cellheight);
                source=new Pair(ycell,xcell);
                setLoc(0);
                invalidate();
            }
            return super.onSingleTapConfirmed(e);
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);
        mGestureDetection.onTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = ev.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                // Only move if the ScaleGestureDetector isn't processing a gesture.
                if (!mScaleDetector.isInProgress()) {
                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;

                    mPosX += dx;
                    mPosY += dy;
                    startXLimit=getTranformedValue(0f,0f).get(0);
                    startYLimit=getTranformedValue(0f,0f).get(1);
                    endXLimit=getTranformedValue(canvasWidth,canvasHeight).get(0);
                    endYLimit=getTranformedValue(canvasWidth,canvasHeight).get(1);
                    mPosX=Math.min(startXLimit,Math.max(mPosX,endXLimit+canvasWidth));
                    mPosY=Math.min(startYLimit,Math.max(mPosY,endYLimit+canvasHeight));
                    invalidate();
                }

                mLastTouchX = x;
                mLastTouchY = y;

                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }

        return true;
    }

//
    public void createBoundry(int i,int j){
        if(j==0){
            cells[i][j].leftWall=true;
        }
        if(j==COLS-1){
            cells[i][j].rightWall=true;
        }
//  Top boundary
        if(i==0){
            cells[i][j].topWall=true;
        }
        if(i==ROWS-1){
            cells[i][j].bottomWall=true;
        }
    }

    //    0 - no door
//    1 - top-left
//    2 - top-right
//    3 - bottom-left
//    4 - bottom-right
//    5 - left-top
//    6 - left-bottom
//    7 - right-top
//    8- right-bottom
    public void createRooms(int rowStart,int rowEnd,int colStart,int colEnd,int door_present){

        for(int i=rowStart;i<=rowEnd;i++){
            for(int j=colStart;j<=colEnd;j++){

                if(j==colStart){
                    cells[i][j].leftWall=true;
                    if(j-1>=0){
                        cells[i][j-1].rightWall=true;
                    }
                }
                if(j==colEnd){
                    cells[i][j].rightWall=true;
                    if(j+1<COLS){
                        cells[i][j+1].leftWall=true;
                    }
                }
//  Top boundary
                if(i==rowStart){
                    cells[i][j].topWall=true;
                    if(i-1>=0){
                        cells[i-1][j].bottomWall=true;
                    }
                }
                if(i==rowEnd){
                    cells[i][j].bottomWall=true;
                    if(i+1<ROWS){
                        cells[i+1][j].topWall=true;
                    }
                }
            }
        }
    }

    public void drawAllRooms(){
        Rooms(0,83,152,419,7,"Apple training center");
        Rooms(0,83,0,151,1,"R&D embedding");
        Rooms(98,139,0,151,1,"CRD 303");
        Rooms(168,257,0,151,1,"Intel COE");
        Rooms(140,167,0,151,1,"");
        Rooms(112,167,228,419,1,"");
        Rooms(0,83,496,641,1,"R&D medical");
        Rooms(112,195,496,641,1,"Schneider COE");
        Rooms(213,257,228,419,1,"");
        Rooms(195,257,496,641,1,"National MEMS");
    }

    public  void Rooms(int rowStart,int rowEnd,int colStart,int colEnd,int door_present,String text){
        createRooms(rowStart,rowEnd,colStart,colEnd,door_present);
        ArrayList<Integer> tempRoom=new ArrayList<Integer>();
        tempRoom.add(rowStart);
        tempRoom.add(rowEnd);
        tempRoom.add(colStart);
        tempRoom.add(colEnd);
        allRooms.add(tempRoom);
        roomNames.add(text);
    }

    private void createMaze(){
        cells=new Cell[ROWS][COLS];

        for(int i=0;i<ROWS;i++){
            for(int j=0;j<COLS;j++){
                cells[i][j]=new Cell(i,j);
                createBoundry(i,j);
            }
        }
        drawAllRooms();
    }

    private class Cell{
        boolean topWall=false,bottomWall=false,leftWall=false,rightWall=false,visited=false;
        int col,row, val = 1;
        public Cell(int col,int row){
            this.col=col;
            this.row=row;
        }
    }

//    Path related functions
    private ArrayList<Pair> PrintPath(int srci, int srcj, int x, int y) {
        ArrayList<Pair> ans = new ArrayList<Pair>();
        ShortestPath(srci,srcj,x,y);
        if (vis[x][y].i == -2 && vis[x][y].j == -2) {
            return ans;
        }
        else {
            System.out.println(srci+" "+srcj+" "+x+" ");
            System.out.println(y);
            while (vis[x][y].i != -1 && vis[x][y].j != -1) {
                ans.add(new Pair(x, y));
                x=vis[x][y].i;
                y=vis[x][y].j;
            }
            ans.add(new Pair(x, y));
        }
        return ans;
    }

    private void ShortestPath(int srci, int srcj, int e1, int e2) {

        for (int i = 0; i < vis.length; i++) {
            for (int j = 0; j < vis[0].length; j++)
                vis[i][j] = new Pair(-2, -2, Integer.MAX_VALUE);
        }

        PriorityQueue<Pair> q = new PriorityQueue<Pair>(2000,new Pair());
        q.add(new Pair(srci, srcj, 0));// 1st Element

        vis[srci][srcj] = new Pair(-1, -1, 0);

        int dx[] = { 0, 0, -1, 1, -1, 1, -1, 1 };
        int dy[] = { -1, 1, 0, 0, -1, -1, 1, 1 };

        while (!q.isEmpty()) {

            Pair node = q.poll(); // remove the top most element
            int ii = node.geti();
            int jj = node.getj();
            if (ii == e1 && jj == e2) {
                break;
            }

            // Diagnoal LeftUp
            if (ii + dx[4] >= 0 && jj + dy[4] >= 0 && ii + dx[4] < ROWS && jj + dy[4] < COLS
                    && !cells[ii + dx[4]][jj + dy[4]].bottomWall && !cells[ii + dx[4]][jj + dy[4]].rightWall
                    && !cells[ii][jj].topWall && !cells[ii][jj].leftWall) {

                Pair x = vis[ii + dx[4]][jj + dy[4]];// {-1,-1}

                if (node.getW() + 1.41 < x.getW()) {
                    x.w = node.w + 1.41;
                    q.add(new Pair(ii + dx[4], jj + dy[4], x.w));
                    vis[ii + dx[4]][jj + dy[4]] = new Pair(ii, jj, x.w);// {-0,0}
                }

            }
            // Diagnoal Leftdown
            if (ii + dx[5] >= 0 && jj + dy[5] >= 0 && ii + dx[5] < ROWS && jj + dy[5] < COLS
                    && !cells[ii + dx[5]][jj + dy[5]].topWall && !cells[ii + dx[5]][jj + dy[5]].rightWall
                    && !cells[ii][jj].bottomWall && !cells[ii][jj].leftWall) {
                Pair x = vis[ii + dx[5]][jj + dy[5]];

                if (node.w + 1.41 < x.getW()) {
                    x.w = node.w + 1.41;
                    q.add(new Pair(ii + dx[5], jj + dy[5], x.w));
                    vis[ii + dx[5]][jj + dy[5]] = new Pair(ii, jj, x.w);
                }
            }
            // Diagnoal rightUp

            if (ii + dx[6] >= 0 && jj + dy[6] >= 0 && ii + dx[6] < ROWS && jj + dy[6] < COLS
                    && !cells[ii + dx[6]][jj + dy[6]].bottomWall && !cells[ii + dx[6]][jj + dy[6]].leftWall
                    && !cells[ii][jj].topWall && !cells[ii][jj].rightWall) {
                Pair x = vis[ii + dx[6]][jj + dy[6]];

                if (node.w + 1.41 < x.getW()) {
                    x.w = node.w + 1.41;
                    q.add(new Pair(ii + dx[6], jj + dy[6], x.w));
                    vis[ii + dx[6]][jj + dy[6]] = new Pair(ii, jj, x.w);
                }
            }
            // Diagnoal down

            if (ii + dx[7] >= 0 && jj + dy[7] >= 0 && ii + dx[7] < ROWS && jj + dy[7] < COLS
                    && !cells[ii + dx[7]][jj + dy[7]].topWall && !cells[ii + dx[7]][jj + dy[7]].leftWall
                    && !cells[ii][jj].bottomWall && !cells[ii][jj].rightWall) {
                Pair x = vis[ii + dx[7]][jj + dy[7]];
                // if (x.i == -2 && x.j == -2) {
                if (node.w + 1.41 < x.getW()) {
                    x.w = node.w + 1.41;
                    q.add(new Pair(ii + dx[7], jj + dy[7], x.w));
                    vis[ii + dx[7]][jj + dy[7]] = new Pair(ii, jj, x.w);
                }

            }
            // Up

            if (ii + dx[2] >= 0 && jj + dy[2] >= 0 && ii + dx[2] < ROWS && jj + dy[2] < COLS
                    && !cells[ii + dx[2]][jj + dy[2]].bottomWall && !cells[ii][jj].topWall) {
                Pair x = vis[ii + dx[2]][jj + dy[2]];
                // if (x.i == -2 && x.j == -2) {

                if (node.w + 1 < x.getW()) {
                    x.w = node.w + 1;
                    q.add(new Pair(ii + dx[2], jj + dy[2], x.w));
                    vis[ii + dx[2]][jj + dy[2]] = new Pair(ii, jj, x.w);
                }
            }
            // Down

            if (ii + dx[3] >= 0 && jj + dy[3] >= 0 && ii + dx[3] < ROWS && jj + dy[3] < COLS
                    && !cells[ii + dx[3]][jj + dy[3]].topWall && !cells[ii][jj].bottomWall) {
                Pair x = vis[ii + dx[3]][jj + dy[3]];
                // if (x.i == -2 && x.j == -2) {
                if (node.w + 1.41 < x.getW()) {
                    x.w = node.w + 1;
                    q.add(new Pair(ii + dx[3], jj + dy[3], x.w));
                    vis[ii + dx[3]][jj + dy[3]] = new Pair(ii, jj, x.w);
                }

            }
            // Left

            if (ii + dx[0] >= 0 && jj + dy[0] >= 0 && ii + dx[0] < ROWS && jj + dy[0] < COLS
                    && !cells[ii + dx[0]][jj + dy[0]].rightWall && !cells[ii][jj].leftWall) {
                Pair x = vis[ii + dx[0]][jj + dy[0]];
                // if (x.i == -2 && x.j == -2) {
                if (node.w + 1.41 < x.getW()) {
                    x.w = node.w + 1;
                    q.add(new Pair(ii + dx[0], jj + dy[0], x.w));
                    vis[ii + dx[0]][jj + dy[0]] = new Pair(ii, jj, x.w);
                }
            }
            // Rigth

            if (ii + dx[1] >= 0 && jj + dy[1] >= 0 && ii + dx[1] < ROWS && jj + dy[1] < COLS
                    && !cells[ii + dx[1]][jj + dy[1]].leftWall && !cells[ii][jj].rightWall) {
                Pair x = vis[ii + dx[1]][jj + dy[1]];
                // if (x.i == -2 && x.j == -2) {
                if (node.w + 1.41 < x.getW()) {
                    x.w = node.w + 1;
                    q.add(new Pair(ii + dx[1], jj + dy[1], x.w));
                    vis[ii + dx[1]][jj + dy[1]] = new Pair(ii, jj, x.w);
                }
            }

        }
    }
}